$baseUrl = "http://localhost:8080/mcp"

Write-Output "=== Testing Headless Mode Login ==="
Write-Output ""

# Test 1: Cookie-based auto-login
Write-Output "Test 1: Verifying cookie-based auto-login..."
$initBody = '{"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0"}},"id":1}'
$initRes = Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream" } -ContentType "application/json" -Body $initBody -SessionVariable ws
$sessionId = $initRes.Headers["Mcp-Session-Id"]
Write-Output "Session ID: $sessionId"
Write-Output ""

# Test getFeeds (requires login)
Write-Output "Test 2: Calling getFeeds with existing cookies..."
$feedsBody = '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"getFeeds","arguments":{}},"id":2}'
try {
    $feedsRes = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $feedsBody -WebSession $ws -TimeoutSec 60).Content
    Write-Output "SUCCESS: getFeeds returned data"
    $feedsRes | Out-File -FilePath "headless_feeds_test.txt" -Encoding utf8
    Write-Output "Result saved to headless_feeds_test.txt"
}
catch {
    Write-Output "FAILED: $_"
}
Write-Output ""

# Test 3: Check login tool response format
Write-Output "Test 3: Testing login tool response format..."
$loginBody = '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"login","arguments":{}},"id":3}'
try {
    $loginRes = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $loginBody -WebSession $ws -TimeoutSec 10).Content
    Write-Output "Login response received:"
    Write-Output $loginRes
    $loginRes | Out-File -FilePath "headless_login_response.txt" -Encoding utf8
    Write-Output ""
    Write-Output "Response saved to headless_login_response.txt"
}
catch {
    Write-Output "Login call failed (expected if already logged in): $_"
}
Write-Output ""

Write-Output "=== Test Complete ==="
