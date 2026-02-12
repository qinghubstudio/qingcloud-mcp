#!/bin/bash

BASE_URL="http://localhost:8082/mcp"

echo "=== Listing Available MCP Tools ==="
echo ""

# Initialize session
echo "1. Initializing MCP session..."
INIT_BODY='{"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0"}},"id":1}'

# Use a temporary file to store headers
HEADERS_FILE=$(mktemp)
INIT_RESPONSE=$(curl -s -D "$HEADERS_FILE" -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d "$INIT_BODY")

# Extract Session ID
SESSION_ID=$(grep -i "Mcp-Session-Id" "$HEADERS_FILE" | awk -F': ' '{print $2}' | tr -d '\r')

rm "$HEADERS_FILE"

echo "Session ID: $SESSION_ID"
echo ""

if [ -z "$SESSION_ID" ]; then
  echo "Error: Failed to get Session ID"
  echo "Response: $INIT_RESPONSE"
  exit 1
fi

# List tools
echo "2. Listing all available tools..."
LIST_BODY='{"jsonrpc":"2.0","method":"tools/list","params":{},"id":2}'

LIST_RESPONSE=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: $SESSION_ID" \
  -d "$LIST_BODY")

echo "Tools list response:"
echo "$LIST_RESPONSE"
echo "$LIST_RESPONSE" > tools_list.txt
echo ""
echo "Result saved to tools_list.txt"
echo ""

# Try to extract and display tool names
echo "Registered tools:"

# Extract JSON from SSE response (lines starting with 'data: ')
JSON_DATA=$(echo "$LIST_RESPONSE" | grep "^data: " | sed 's/^data: //')

if [ -z "$JSON_DATA" ]; then
    # Fallback if no data line found (maybe just JSON?)
    JSON_DATA="$LIST_RESPONSE"
fi

if command -v jq &> /dev/null; then
    # Parse the extracted JSON
    echo "$JSON_DATA" | jq -r '.result.tools[].name' | sed 's/^/  - /'
else
    # Fallback to grep/sed if jq is not installed
    echo "$JSON_DATA" | grep -o '"name"\s*:\s*"[^"]*"' | sed 's/"name"[[:space:]]*:[[:space:]]*"//g' | sed 's/"//g' | while read -r tool; do
      echo "  - $tool"
    done
fi

echo ""
echo "=== Test Complete ==="
