#!/bin/bash

BASE_URL="http://localhost:8082/mcp"

echo "=== Testing postComment Tool ==="
echo ""
echo "NOTE: Comment functionality requires proper account permissions"
echo ""

# Initialize session
echo "1. Initializing MCP session..."
INIT_BODY='{"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0"}},"id":1}'

HEADERS_FILE=$(mktemp)
curl -s -D "$HEADERS_FILE" -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d "$INIT_BODY" > /dev/null

SESSION_ID=$(grep -i "Mcp-Session-Id" "$HEADERS_FILE" | awk -F': ' '{print $2}' | tr -d '\r')
rm "$HEADERS_FILE"

echo "Session ID: $SESSION_ID"
echo ""

# 2. Get Feeds
echo "2. Getting feeds to obtain noteId and xsecToken..."
FEEDS_BODY='{"jsonrpc":"2.0","method":"tools/call","params":{"name":"getFeeds","arguments":{}},"id":2}'

FEEDS_RES=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: $SESSION_ID" \
  -d "$FEEDS_BODY" \
  --max-time 60)

# Extract JSON content from response
FEEDS_DATA=$(echo "$FEEDS_RES" | grep "^data: " | sed 's/^data: //')
if [ -z "$FEEDS_DATA" ]; then FEEDS_DATA="$FEEDS_RES"; fi

# Need jq for complex parsing
if ! command -v jq &> /dev/null; then
    echo "x jq command not found. This script requires jq for parsing JSON."
    exit 1
fi

# Parse feed data
RAW_CONTENT=$(echo "$FEEDS_DATA" | jq -r '.result.content[0].text')
if [ "$RAW_CONTENT" == "null" ] || [ -z "$RAW_CONTENT" ]; then
    echo "x Failed to get feed content"
    echo "$FEEDS_RES"
    exit 1
fi

ITEM_COUNT=$(echo "$RAW_CONTENT" | jq '.data.items | length')

if [ "$ITEM_COUNT" -gt 0 ]; then
    NOTE_ID=$(echo "$RAW_CONTENT" | jq -r '.data.items[0].id')
    XSEC_TOKEN=$(echo "$RAW_CONTENT" | jq -r '.data.items[0].xsecToken')
    NOTE_TITLE=$(echo "$RAW_CONTENT" | jq -r '.data.items[0].noteCard.displayTitle')
    
    echo "Found note: $NOTE_TITLE"
    echo "Note ID: $NOTE_ID"
    echo ""
    
    # 3. Post Comment
    echo "3. Attempting to post comment..."
    echo "WARNING: This may fail if account doesn't have comment permissions"
    echo ""
    
    COMMENT_BODY=$(jq -n \
                  --arg noteId "$NOTE_ID" \
                  --arg xsecToken "$XSEC_TOKEN" \
                  --arg content "测试评论 - 自动化测试" \
                  '{jsonrpc: "2.0", method: "tools/call", params: {name: "postComment", arguments: {noteId: $noteId, xsecToken: $xsecToken, content: $content}}, id: 3}')
    
    COMMENT_RES=$(curl -s -X POST "$BASE_URL" \
      -H "Content-Type: application/json" \
      -H "Accept: application/json, text/event-stream" \
      -H "Mcp-Session-Id: $SESSION_ID" \
      -d "$COMMENT_BODY" \
      --max-time 60)
    
    echo "Comment response received:"
    echo "$COMMENT_RES"
    echo "$COMMENT_RES" > test_comment_result.txt
    echo ""
    echo "Result saved to test_comment_result.txt"
else
    echo "No feeds found to test comment"
fi

echo ""
echo "=== Test Complete ==="
