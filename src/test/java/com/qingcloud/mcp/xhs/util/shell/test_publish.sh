#!/bin/bash

BASE_URL="http://localhost:8082/mcp"

echo "=== Testing publish_content Tool ==="
echo ""
echo "NOTE: This test requires login first"
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

# 2. Check login status
echo "2. Checking login status..."
CHECK_BODY='{"jsonrpc":"2.0","method":"tools/call","params":{"name":"checkLoginStatus","arguments":{}},"id":2}'
CHECK_RES=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: $SESSION_ID" \
  -d "$CHECK_BODY" \
  --max-time 30)

echo "$CHECK_RES"
echo ""

IS_LOGGED_IN=false
if echo "$CHECK_RES" | grep -q '"isLoggedIn":true' || echo "$CHECK_RES" | grep -q '"isLoggedIn": true'; then
    IS_LOGGED_IN=true
fi
# Try jq if installed
if command -v jq &> /dev/null; then
    CHECK_DATA=$(echo "$CHECK_RES" | grep "^data: " | sed 's/^data: //')
    IS_LOGGED_IN_JQ=$(echo "$CHECK_DATA" | jq -r '.result.content[0].text | fromjson | .isLoggedIn // false')
    if [ "$IS_LOGGED_IN_JQ" == "true" ]; then IS_LOGGED_IN=true; fi
fi

if [ "$IS_LOGGED_IN" != true ]; then
    echo "ERROR: Not logged in. Please run ./test_login.sh first"
    echo ""
    echo "=== Test Aborted ==="
    exit 1
fi

# 3. Test with local image
echo "3. Testing publish_content with local image..."
echo "NOTE: Please update the image path in the script to a valid local image"
echo ""

# Example with a placeholder image path
IMAGE_PATH="${1:-./girls.png}"

if [ ! -f "$IMAGE_PATH" ]; then
    echo "WARNING: Test image not found at: $IMAGE_PATH"
    echo "Please create a test image or update the path in the script"
    echo ""
    echo "Skipping publish test..."
    echo ""
    echo "=== Test Complete (Skipped) ==="
    exit 0
fi

echo "Publishing content..."
echo "Title: 自动化测试发布"
echo "Image: $IMAGE_PATH"
echo ""

PUBLISH_BODY=$(jq -n \
              --arg title "自动化测试发布" \
              --arg content "这是一条自动化测试内容，请忽略。" \
              --arg img "$IMAGE_PATH" \
              --arg tag "测试" \
              --arg tag2 "自动化" \
              '{jsonrpc: "2.0", method: "tools/call", params: {name: "publish_content", arguments: {title: $title, content: $content, images: [$img], tags: [$tag, $tag2]}}, id: 3}')

PUBLISH_RES=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: $SESSION_ID" \
  -d "$PUBLISH_BODY" \
  --max-time 300)

echo "Publish response received:"
echo "$PUBLISH_RES"
echo "$PUBLISH_RES" > test_publish_result.txt
echo ""
echo "Result saved to test_publish_result.txt"

if echo "$PUBLISH_RES" | grep -q '"success":true' || echo "$PUBLISH_RES" | grep -q '"success": true'; then
    echo ""
    echo "✓ Publish test PASSED"
else
    echo ""
    echo "✗ Publish test FAILED - check result file for details"
fi

echo ""
echo "=== Test Complete ==="
