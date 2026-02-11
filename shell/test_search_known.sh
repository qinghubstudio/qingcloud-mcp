#!/bin/bash

# Configuration
BASE_URL="http://localhost:8089/mcp"
HEADERS_FILE="search_headers_known.txt"
RESPONSE_FILE="search_response_known.json"
KEYWORD="重庆XSC试卷"

echo -e "\033[36m=== Testing Search (Known Keyword) ===\033[0m"

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

curl -s -D "$HEADERS_FILE" -X POST "$BASE_URL" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json, text/event-stream" \
    -d "$INIT_BODY" > "$RESPONSE_FILE"

SESSION_ID=$(grep -i "Mcp-Session-Id" "$HEADERS_FILE" | awk '{print $2}' | tr -d '\r')

if [ -z "$SESSION_ID" ]; then
    echo -e "\033[31m✗ Session initialization failed\033[0m"
    exit 1
fi
echo -e "\033[32m✓ Session initialized: $SESSION_ID\033[0m"

# 2. Check Login Status (Expect True)
echo -e "\033[33m2. Verifying login status...\033[0m"
CHECK_BODY='{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
        "name": "checkLoginStatus",
        "arguments": {}
    },
    "id": 2
}'

curl -s -X POST "$BASE_URL" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json, text/event-stream" \
    -H "Mcp-Session-Id: $SESSION_ID" \
    -d "$CHECK_BODY" > status_known.json

if grep -q "true" status_known.json; then
    echo -e "\033[32m✓ User is logged in\033[0m"
else
    echo -e "\033[31m✗ User is NOT logged in. Scan might be required.\033[0m"
    # Continue anyway
fi

# 3. Search
echo -e "\033[33m3. Searching for '$KEYWORD'...\033[0m"
SEARCH_BODY='{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
        "name": "searchNotes",
        "arguments": {
            "keyword": "'"$KEYWORD"'"
        }
    },
    "id": 3
}'

curl -s -X POST "$BASE_URL" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json, text/event-stream" \
    -H "Mcp-Session-Id: $SESSION_ID" \
    -d "$SEARCH_BODY" > search_result_known.json

echo -e "\033[32m✓ Search request completed\033[0m"
echo -e "\033[36mResult snippet:\033[0m"
head -n 20 search_result_known.json
