$baseUrl = "http://localhost:8080/mcp"

echo "=== Testing Cookie Persistence ==="
echo ""

# Initialize session
echo "1. Initializing MCP session..."
$initBody = '{"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0"}},"id":1}'
$initRes = Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream" } -ContentType "application/json" -Body $initBody -SessionVariable ws
$sessionId = $initRes.Headers["Mcp-Session-Id"]
echo "Session ID: $sessionId"
echo ""

# Check login status (should be logged in using saved cookies)
echo "2. Checking login status (should use saved cookies)..."
$checkBody = '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"checkLoginStatus","arguments":{}},"id":2}'
$checkRes = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $checkBody -WebSession $ws -TimeoutSec 60).Content
echo $checkRes
$checkRes | Out-File -FilePath "cookie_persistence_test.txt" -Encoding utf8
echo ""

# Try to get feeds (requires login)
echo "3. Testing getFeeds (requires login)..."
$feedsBody = '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"getFeeds","arguments":{}},"id":3}'
try {
    $feedsRes = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $feedsBody -WebSession $ws -TimeoutSec 60).Content
    echo "SUCCESS: getFeeds returned data"
    $feedsRes | Out-File -FilePath "feeds_with_cookies.txt" -Encoding utf8
}
catch {
    echo "FAILED: $_"
}
echo ""

echo "=== Test Complete ==="
