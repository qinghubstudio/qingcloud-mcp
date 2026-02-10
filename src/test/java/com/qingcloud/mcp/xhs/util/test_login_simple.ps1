$baseUrl = "http://localhost:8080/mcp"

echo "=== Testing Login Functionality ==="
echo ""

# Initialize session
echo "1. Initializing MCP session..."
$initBody = '{"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0"}},"id":1}'
$initRes = Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream" } -ContentType "application/json" -Body $initBody -SessionVariable ws
$sessionId = $initRes.Headers["Mcp-Session-Id"]
echo "Session ID: $sessionId"
echo ""

# Call login tool
echo "2. Calling login tool..."
echo "NOTE: Browser window will open. Please scan the QR code to login."
echo ""

$loginBody = '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"login","arguments":{}},"id":2}'
$loginRes = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $loginBody -WebSession $ws -TimeoutSec 180).Content
$loginRes | Out-File -FilePath "login_result.txt" -Encoding utf8
echo "Login result saved to login_result.txt"
echo ""

# Check cookies
echo "3. Checking cookies.json..."
if (Test-Path "cookies.json") {
    echo "cookies.json exists"
    $cookies = Get-Content "cookies.json" | ConvertFrom-Json
    echo "Cookie count: $($cookies.Count)"
}
else {
    echo "cookies.json does not exist"
}
echo ""

# Check login status
echo "4. Checking login status..."
$checkBody = '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"checkLoginStatus","arguments":{}},"id":3}'
$checkRes = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $checkBody -WebSession $ws -TimeoutSec 60).Content
echo $checkRes
echo ""

echo "=== Test Complete ==="
