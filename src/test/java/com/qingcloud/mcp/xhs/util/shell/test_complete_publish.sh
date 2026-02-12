#!/bin/bash

BASE_URL="http://localhost:8082/mcp"

echo "=== Complete Publish Test: Login + Publish ==="
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

# Check login status
echo "2. Checking login status..."
CHECK_BODY='{"jsonrpc":"2.0","method":"tools/call","params":{"name":"checkLoginStatus","arguments":{}},"id":2}'
CHECK_RES=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: $SESSION_ID" \
  -d "$CHECK_BODY" \
  --max-time 30)

echo "Login status response:"
echo "$CHECK_RES"
echo ""

# Extract JSON from SSE if needed
CHECK_DATA=$(echo "$CHECK_RES" | grep "^data: " | sed 's/^data: //')
if [ -z "$CHECK_DATA" ]; then CHECK_DATA="$CHECK_RES"; fi

IS_LOGGED_IN=false
if echo "$CHECK_DATA" | grep -q '"isLoggedIn":true' || echo "$CHECK_DATA" | grep -q '"isLoggedIn": true'; then
    IS_LOGGED_IN=true
fi
# Using jq for better check if available
if command -v jq &> /dev/null; then
    IS_LOGGED_IN_JQ=$(echo "$CHECK_DATA" | jq -r '.result.content[0].text | fromjson | .isLoggedIn')
    if [ "$IS_LOGGED_IN_JQ" == "true" ]; then IS_LOGGED_IN=true; fi
fi

if [ "$IS_LOGGED_IN" = true ]; then
    echo "Already logged in - proceeding to publish"
    echo ""
else
    echo "Not logged in. Please run login first."
    echo "Run: ./test_login.sh or ./test_fresh_login.sh"
    echo ""
    echo "=== Test Aborted ==="
    exit 1
fi

# Publish content
echo "3. Publishing content..."
# Default image path, can be overridden by argument or modified
IMAGE_PATH="${1:-./girls.png}"

# Check if image exists
if [ ! -f "$IMAGE_PATH" ]; then
    echo "ERROR: Test image not found at: $IMAGE_PATH"
    echo "Please provide a valid image path as argument or place 'girls.png' in current directory."
    echo ""
    echo "=== Test Aborted ==="
    exit 1
fi

echo "Image path: $IMAGE_PATH"
echo "Title: Test Publish"
echo "Content: one girl."
echo ""

# Build publish request
# Need to construct JSON array for images properly
PUBLISH_BODY=$(jq -n \
                  --arg title "Test Publish" \
                  --arg content "one girl." \
                  --arg img "$IMAGE_PATH" \
                  --arg tag "test" \
                  '{jsonrpc: "2.0", method: "tools/call", params: {name: "publish_content", arguments: {title: $title, content: $content, images: [$img], tags: [$tag]}}, id: 3}')

echo "Sending publish request..."
echo "NOTE: This may take 1-2 minutes (browser automation)..."
echo ""

PUBLISH_RES=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: $SESSION_ID" \
  -d "$PUBLISH_BODY" \
  --max-time 300)

echo "Publish response:"
echo "$PUBLISH_RES"
echo "$PUBLISH_RES" > test_complete_publish_result.txt
echo ""
echo "Result saved to test_complete_publish_result.txt"
echo ""

# Check if successful
if echo "$PUBLISH_RES" | grep -q '"success":true' || echo "$PUBLISH_RES" | grep -q '"success": true'; then
    echo "========================================="
    echo "SUCCESS: Content published successfully!"
    echo "========================================="
else
    echo "FAILED: Publish test FAILED - check result file for details"
fi
echo ""

echo "=== Test Complete ==="
