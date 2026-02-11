#!/bin/bash

# Configuration
BASE_URL="http://localhost:8089/mcp"
HEADERS_FILE="headers.txt"
RESPONSE_FILE="response.json"

echo -e "\033[36m=== Testing Login Function ===\033[0m"
echo ""

# 1. Initialize Session
echo -e "\033[33m1. Initializing MCP session...\033[0m"
INIT_BODY='{
    "jsonrpc": "2.0",
    "method": "initialize",
    "params": {
        "protocolVersion": "2024-11-05",
        "capabilities": {},
        "clientInfo": {
            "name": "test-client",
            "version": "1.0"
        }
    },
    "id": 1
}'

# Execute Initialize Request
curl -s -D "$HEADERS_FILE" -X POST "$BASE_URL" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json, text/event-stream" \
    -d "$INIT_BODY" > "$RESPONSE_FILE"

# Extract Session ID (remove carriage return)
SESSION_ID=$(grep -i "Mcp-Session-Id" "$HEADERS_FILE" | awk '{print $2}' | tr -d '\r')

if [ -z "$SESSION_ID" ]; then
    echo -e "\033[31m✗ Session initialization failed\033[0m"
    echo "Response:"
    cat "$RESPONSE_FILE"
    echo "Headers:"
    cat "$HEADERS_FILE"
    exit 1
fi

echo -e "\033[32m✓ Session initialized\033[0m"
echo -e "\033[90m  Session ID: $SESSION_ID\033[0m"
echo ""

# 2. Call Login Tool
echo -e "\033[33m2. Calling login tool...\033[0m"
echo -e "\033[36m  Note: Browser window should open (or headless). Scan QR in logs if needed.\033[0m"

LOGIN_BODY='{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
        "name": "login",
        "arguments": {}
    },
    "id": 2
}'

# Execute Login Request
curl -s -X POST "$BASE_URL" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json, text/event-stream" \
    -H "Mcp-Session-Id: $SESSION_ID" \
    -d "$LOGIN_BODY" > "login_test_result.txt"

echo -e "\033[32m✓ Login tool called\033[0m"
echo ""
echo -e "\033[36mLogin Result:\033[0m"
cat "login_test_result.txt"
echo ""
echo -e "\033[90m  Result saved to: login_test_result.txt\033[0m"
echo ""

# 3. Check cookies file
echo -e "\033[33m3. Checking cookies file...\033[0m"
if [ -f "cookies.json" ]; then
    COOKIE_COUNT=$(grep -o "{" "cookies.json" | wc -l)
    echo -e "\033[32m✓ cookies.json file exists\033[0m"
else
    echo -e "\033[31m✗ cookies.json file does not exist\033[0m"
fi
echo ""

# 4. Check Login Status
echo -e "\033[33m4. Checking login status...\033[0m"
CHECK_BODY='{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
        "name": "checkLoginStatus",
        "arguments": {}
    },
    "id": 3
}'

curl -s -X POST "$BASE_URL" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json, text/event-stream" \
    -H "Mcp-Session-Id: $SESSION_ID" \
    -d "$CHECK_BODY" > status_result.txt

echo -e "\033[32m✓ Login status check complete\033[0m"
echo ""
echo -e "\033[36mLogin Status:\033[0m"
cat status_result.txt
echo ""

echo -e "\033[36m=== Test Complete ===\033[0m"
