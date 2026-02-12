#!/bin/bash

BASE_URL="http://localhost:8082/mcp"

echo "=== Testing Login Functionality ==="
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

# 2. Call login tool
echo "2. Calling login tool..."
echo "  Hint: A browser window will open, please scan the QR code to login"
echo ""

LOGIN_BODY='{"jsonrpc":"2.0","method":"tools/call","params":{"name":"login","arguments":{}},"id":2}'

echo "  Waiting for login completion (max 120s)..."
LOGIN_RES=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: $SESSION_ID" \
  -d "$LOGIN_BODY" \
  --max-time 180)

echo "✓ Login tool call completed"
echo ""
echo "Login result:"
echo "$LOGIN_RES"
echo ""

# Save result
echo "$LOGIN_RES" > login_test_result.txt
echo "  Result saved to: login_test_result.txt"
echo ""

# 3. Check cookies file
echo "3. Checking cookies file..."
if [ -f "cookies.json" ]; then
    echo "✓ cookies.json file exists"
    # Count cookies if jq is available
    if command -v jq &> /dev/null; then
        COOKIE_COUNT=$(jq '. | length' cookies.json)
        echo "  Cookie count: $COOKIE_COUNT"
    fi
    echo ""
else
    echo "x cookies.json file does not exist"
    echo ""
fi

# 4. Check login status
echo "4. Testing login status check..."
CHECK_BODY='{"jsonrpc":"2.0","method":"tools/call","params":{"name":"checkLoginStatus","arguments":{}},"id":3}'

CHECK_RES=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: $SESSION_ID" \
  -d "$CHECK_BODY" \
  --max-time 60)

echo "✓ Login status check completed"
echo ""
echo "Login status:"
echo "$CHECK_RES"
echo ""

echo "=== Test Complete ==="
