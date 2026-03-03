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

LOGIN_BODY='{"jsonrpc":"2.0","method":"tools/call","params":{"name":"login","arguments":{"forceRefresh":true}},"id":2}'



# Start background process to monitor logs for QR code
LOG_FILE="/tmp/mcp_server.log"
echo "  Monitoring $LOG_FILE for QR code..."
(
    tail -f -n 0 "$LOG_FILE" | while read -r line; do
        # Debug: print relevant lines
        if echo "$line" | grep -q "LoginAction"; then
            echo "[DEBUG] $line"
        fi
        
        if echo "$line" | grep -q "QR code image URL:"; then
            # Extract everything after the last colon and space
            URL=$(echo "$line" | sed -e 's/.*QR code image URL: //')
            echo ""
            echo "================================================================"
            echo "A QR code has been generated. Please scan it to proceed:"
            if [[ "$URL" == data:image* ]]; then
                echo "[Base64 Image Data Detected]"
                echo "${URL:0:100}..."
                # Extract base64 part regardless of exact image type
                B64_DATA=$(echo "$URL" | sed -e 's/data:image\/[^;]*;base64,//')
                echo "$B64_DATA" | base64 -d > /tmp/mcp_qrcode.png 2>/dev/null
                echo "QR Code saved to /tmp/mcp_qrcode.png"
            else
                echo "$URL"
            fi
            echo "================================================================"
            echo ""
        fi
        # Stop monitoring if login is successful or timeout
        if echo "$line" | grep -q "Login successful"; then
            echo "✓ Detected login success in logs"
            pkill -P $$ tail
            break
        fi
    done
) &
BG_PID=$!

echo "  Waiting for login completion (max 120s)..."
# Set a trap to kill background process on exit
trap "kill $BG_PID 2>/dev/null" EXIT

LOGIN_RES=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: $SESSION_ID" \
  -d "$LOGIN_BODY" \
  --max-time 180)

# Kill background monitoring process
kill $BG_PID 2>/dev/null

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
COOKIES_FILE="/u01/jenkins/workspace/qingcloud-mcp/cookies.json"
if [ -f "$COOKIES_FILE" ]; then
    echo "✓ cookies.json file exists at $COOKIES_FILE"
    # Count cookies if jq is available
    if command -v jq &> /dev/null; then
        COOKIE_COUNT=$(jq '. | length' "$COOKIES_FILE")
        echo "  Cookie count: $COOKIE_COUNT"
    fi
    echo ""
else
    echo "x cookies.json file does not exist at $COOKIES_FILE"
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
