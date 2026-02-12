#!/bin/bash

BASE_URL="http://localhost:8082/mcp"

echo "=== Testing Fresh Login (No Cookies) ==="
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

# Call login tool
echo "2. Calling login tool (should return QR code URL)..."
echo "NOTE: In headless mode, you'll receive a QR code URL to scan"
echo ""

LOGIN_BODY='{"jsonrpc":"2.0","method":"tools/call","params":{"name":"login","arguments":{}},"id":2}'

# Set a longer timeout for login (180 seconds to allow time for scanning)
LOGIN_RES=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: $SESSION_ID" \
  -d "$LOGIN_BODY" \
  --max-time 180)

echo "Login response received:"
echo "$LOGIN_RES"
echo ""

# Extract JSON from SSE if needed
JSON_DATA=$(echo "$LOGIN_RES" | grep "^data: " | sed 's/^data: //')
if [ -z "$JSON_DATA" ]; then JSON_DATA="$LOGIN_RES"; fi

# Save response
echo "$JSON_DATA" > fresh_login_result.txt
echo "Response saved to fresh_login_result.txt"
echo ""

# Try to parse and extract QR code URL
if command -v jq &> /dev/null; then
    QR_URL=$(echo "$JSON_DATA" | jq -r '.result.content[0].text | fromjson | .qrcodeUrl // empty')
else
    # Fallback extraction logic using grep/sed
    # Find JSON content in text field
    QR_URL=$(echo "$JSON_DATA" | grep -o '"qrcodeUrl":"[^"]*"' | sed 's/"qrcodeUrl":"//' | sed 's/"//')
fi

if [ -n "$QR_URL" ]; then
    echo "QR Code URL extracted:"
    echo "$QR_URL"
    echo ""
    echo "Please open this URL in your browser or scan with your phone to login."
else
    echo "Could not extract QR code URL from response (or already logged in)"
fi
echo ""

# Check if cookies were saved
echo "3. Checking if cookies.json was created..."
if [ -f "cookies.json" ]; then
    echo "SUCCESS: cookies.json file created"
    if command -v jq &> /dev/null; then
        COOKIE_COUNT=$(jq '. | length' cookies.json)
        echo "Cookie count: $COOKIE_COUNT"
    fi
else
    echo "INFO: cookies.json not found (expected if login timed out or not completed)"
fi
echo ""

echo "=== Test Complete ==="
