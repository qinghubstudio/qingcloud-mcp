#!/bin/bash

BASE_URL="http://localhost:8082/mcp"

echo "=== Testing getUserProfile Tool ==="
echo ""

# Initialize session
echo "Initializing session..."
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

if ! command -v jq &> /dev/null; then
    echo "x jq command not found. This script requires jq."
    exit 1
fi

# 1. Get Feeds
echo "Calling getFeeds to find a user..."
FEEDS_BODY='{"jsonrpc":"2.0","method":"tools/call","params":{"name":"getFeeds","arguments":{}},"id":2}'

FEEDS_RES=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: $SESSION_ID" \
  -d "$FEEDS_BODY" \
  --max-time 120)

FEEDS_DATA=$(echo "$FEEDS_RES" | grep "^data: " | sed 's/^data: //')
if [ -z "$FEEDS_DATA" ]; then FEEDS_DATA="$FEEDS_RES"; fi

# Extract user info using jq
RAW_CONTENT=$(echo "$FEEDS_DATA" | jq -r '.result.content[0].text')

USER_ID=""
XSEC_TOKEN=""

# Try simple regex first on raw content to mimic PS script behavior or use jq if structural
# The PS script used regex on the JSON text to avoid duplicate key issues in ConvertFrom-Json
# We can use grep/perl for regex extraction from the raw content string
if [ -n "$RAW_CONTENT" ] && [ "$RAW_CONTENT" != "null" ]; then
    USER_ID=$(echo "$RAW_CONTENT" | grep -o '"userId"\s*:\s*"[^"]*"' | head -1 | sed 's/"userId"\s*:\s*"//' | sed 's/"//')
    XSEC_TOKEN=$(echo "$RAW_CONTENT" | grep -o '"xsecToken"\s*:\s*"[^"]*"' | head -1 | sed 's/"xsecToken"\s*:\s*"//' | sed 's/"//')
fi

echo "Found User ID: $USER_ID"
echo "Found User XsecToken: $XSEC_TOKEN"

if [ -z "$XSEC_TOKEN" ]; then
    echo "No xsecToken found for user, attempting search for more results..."
    SEARCH_BODY='{"jsonrpc":"2.0","method":"tools/call","params":{"name":"searchNotes","arguments":{"keyword":"travel"}},"id":3}'
    
    SEARCH_RES=$(curl -s -X POST "$BASE_URL" \
      -H "Content-Type: application/json" \
      -H "Accept: application/json, text/event-stream" \
      -H "Mcp-Session-Id: $SESSION_ID" \
      -d "$SEARCH_BODY" \
      --max-time 120)
      
    SEARCH_DATA=$(echo "$SEARCH_RES" | grep "^data: " | sed 's/^data: //')
    if [ -z "$SEARCH_DATA" ]; then SEARCH_DATA="$SEARCH_RES"; fi
    
    SEARCH_RAW=$(echo "$SEARCH_DATA" | jq -r '.result.content[0].text')
    
    if [ -n "$SEARCH_RAW" ] && [ "$SEARCH_RAW" != "null" ]; then
        USER_ID=$(echo "$SEARCH_RAW" | grep -o '"userId"\s*:\s*"[^"]*"' | head -1 | sed 's/"userId"\s*:\s*"//' | sed 's/"//')
        XSEC_TOKEN=$(echo "$SEARCH_RAW" | grep -o '"xsecToken"\s*:\s*"[^"]*"' | head -1 | sed 's/"xsecToken"\s*:\s*"//' | sed 's/"//')
    fi
    
    echo "From search - User ID: $USER_ID, XsecToken: $XSEC_TOKEN"
fi

if [ -n "$USER_ID" ] && [ -n "$XSEC_TOKEN" ]; then
    # 2. Get User Profile
    echo "Calling getUserProfile..."
    
    PROFILE_BODY=$(jq -n \
                  --arg userId "$USER_ID" \
                  --arg xsecToken "$XSEC_TOKEN" \
                  '{jsonrpc: "2.0", method: "tools/call", params: {name: "getUserProfile", arguments: {userId: $userId, xsecToken: $xsecToken}}, id: 4}')
    
    PROFILE_RES=$(curl -s -X POST "$BASE_URL" \
      -H "Content-Type: application/json" \
      -H "Accept: application/json, text/event-stream" \
      -H "Mcp-Session-Id: $SESSION_ID" \
      -d "$PROFILE_BODY" \
      --max-time 120)
      
    echo "Profile Response:"
    echo "$PROFILE_RES"
else
    echo "Could not find valid user ID and token to test profile."
fi

echo "=== Test Complete ==="
