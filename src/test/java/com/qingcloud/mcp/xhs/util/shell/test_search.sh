#!/bin/bash

BASE_URL="http://localhost:8082/mcp"
KEYWORD=${1:-"重庆XSC试卷"}

echo "=== Testing Search Functionality ==="
echo "Keyword: $KEYWORD"
echo ""

# 1. Initialize session
echo "1. Initializing MCP session..."
INIT_BODY='{"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0"}},"id":1}'

HEADERS_FILE=$(mktemp)
curl -s -D "$HEADERS_FILE" -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d "$INIT_BODY" > /dev/null

SESSION_ID=$(grep -i "Mcp-Session-Id" "$HEADERS_FILE" | awk -F': ' '{print $2}' | tr -d '\r')
rm "$HEADERS_FILE"

if [ -z "$SESSION_ID" ]; then
    echo "x Session initialization failed"
    exit 1
fi

echo "✓ Session initialization successful"
echo "  Session ID: $SESSION_ID"
echo ""

# 2. Call search tool
echo "2. Calling searchNotes tool..."
SEARCH_BODY=$(printf '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"searchNotes","arguments":{"keyword":"%s"}},"id":2}' "$KEYWORD")

SEARCH_RES=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: $SESSION_ID" \
  -d "$SEARCH_BODY" \
  --max-time 120)

echo "✓ Search tool call completed"
echo ""
echo "Search result (first 1000 chars):"
echo "$SEARCH_RES" | head -c 1000
echo "..."
echo ""

if echo "$SEARCH_RES" | grep -q "\"success\":true"; then
    COUNT=$(echo "$SEARCH_RES" | jq '.result.content[0].text | fromjson | .data.total' 2>/dev/null)
    echo "✓ Search successful! Found $COUNT results."
else
    echo "x Search failed or returned error."
    echo "$SEARCH_RES" | jq . 2>/dev/null || echo "$SEARCH_RES"
fi

echo ""
echo "=== Test Complete ==="
