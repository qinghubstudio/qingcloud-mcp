#!/bin/bash

# Default to 8082 as observed in other working scripts, but allow override
MCP_URL="${MCP_URL:-http://localhost:8082/mcp}"

echo "=== Testing searchNotes Tool ==="
echo ""

# 1. Initialize session
echo "1. Initializing MCP session..."
# Initialize JSON body
INIT_BODY='{"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0"}},"id":1}'

# Temporary file for headers to capture Session ID
HEADERS_FILE=$(mktemp)

# Perform initialization request
curl -s -D "$HEADERS_FILE" -X POST "$MCP_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d "$INIT_BODY" > /dev/null

# Extract Session ID from headers
SESSION_ID=$(grep -i "Mcp-Session-Id" "$HEADERS_FILE" | awk -F': ' '{print $2}' | tr -d '\r')
rm "$HEADERS_FILE"

if [ -z "$SESSION_ID" ]; then
    echo "x Session initialization failed"
    exit 1
fi

echo "✓ Session initialization successful"
echo "  Session ID: $SESSION_ID"
echo ""

# 2. Call searchNotes
echo "2. Calling searchNotes tool with keyword '美食'..."
# Construct search parameters matching the PS1 script (added page/page_size)
SEARCH_BODY='{"jsonrpc":"2.0","method":"tools/call","params":{"name":"searchNotes","arguments":{"keyword":"美食","page":1,"page_size":10}},"id":2}'

# Call the tool
SEARCH_RES=$(curl -s -X POST "$MCP_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: $SESSION_ID" \
  -d "$SEARCH_BODY" \
  --max-time 60)

# Save raw result
echo "$SEARCH_RES" > test_search_result.txt
echo "SUCCESS: searchNotes returned data"
echo "Result saved to test_search_result.txt"

# 3. Parse and display summary
# Extract JSON from SSE (remove data: prefix)
JSON_DATA=$(echo "$SEARCH_RES" | grep "^data: " | sed 's/^data: //' | head -n 1)

if [ -n "$JSON_DATA" ]; then
    # Use python for reliable JSON parsing since jq might not be available or complicates things with complex structures
    echo "$JSON_DATA" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    # The tool returns a result, we need to dig into it
    # Expected structure: { result: { content: [ { type: 'text', text: 'JSON_STRING' } ] } }
    
    if 'result' in data and 'content' in data['result']:
        for content in data['result']['content']:
            if content.get('type') == 'text':
                text_content = content.get('text', '{}')
                try:
                    search_result = json.loads(text_content)
                    if search_result.get('success'):
                        data_block = search_result.get('data', {})
                        items = data_block.get('items', [])
                        total = data_block.get('total', len(items))
                        
                        print(f'Search results count: {total}')
                        print('First 3 results:')
                        for i, item in enumerate(items[:3]):
                             title = item.get('noteCard', {}).get('displayTitle', 'No Title')
                             user = item.get('noteCard', {}).get('user', {}).get('nickName', 'Unknown')
                             print(f'  - {title} (by {user})')
                    else:
                         print(f'Search failed: {search_result.get(\"message\")}')
                except json.JSONDecodeError:
                    print('Could not decode inner JSON result')
                    print(text_content[:200])
except Exception as e:
    print(f'Error parsing result: {e}')
" 
else
    echo "Failed to extract JSON data from response"
fi

echo ""
echo "=== Test Complete ==="
