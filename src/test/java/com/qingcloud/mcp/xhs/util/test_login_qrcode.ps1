$baseUrl = "http://localhost:8080/mcp"

Write-Output "=== QR Code Login Test (Production Method) ==="
Write-Output ""
Write-Output "NOTE: This script uses the login tool to display QR code for scanning."
Write-Output "This is the recommended method for production use."
Write-Output ""

# Initialize session
Write-Output "1. Initializing MCP session..."
$initBody = '{"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0"}},"id":1}'
$initRes = Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream" } -ContentType "application/json" -Body $initBody -SessionVariable ws
$sessionId = $initRes.Headers["Mcp-Session-Id"]
Write-Output "Session ID: $sessionId"
Write-Output ""

# Call login tool
Write-Output "2. Calling login tool..."
Write-Output "NOTE: Browser window will open. Please scan the QR code to login."
Write-Output "Waiting for login (max 180 seconds)..."
Write-Output ""

$loginBody = '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"login","arguments":{}},"id":2}'
try {
    $loginRes = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $loginBody -WebSession $ws -TimeoutSec 180).Content
    Write-Output "Login result:"
    Write-Output $loginRes
    $loginRes | Out-File -FilePath "login_qrcode_result.txt" -Encoding utf8
    Write-Output ""
    Write-Output "Result saved to login_qrcode_result.txt"
    Write-Output ""
}
catch {
    Write-Output "FAILED: Login timeout or error: $_"
    Write-Output ""
    Write-Output "=== Test Aborted ==="
    exit 1
}

# Check cookies
Write-Output "3. Checking cookies.json..."
if (Test-Path "..\..\..\..\..\..\..\..\cookies.json") {
    Write-Output "SUCCESS: cookies.json exists"
    $cookies = Get-Content "..\..\..\..\..\..\..\..\cookies.json" | ConvertFrom-Json
    Write-Output "Cookie count: $($cookies.Count)"
}
else {
    Write-Output "WARNING: cookies.json does not exist"
}
Write-Output ""

# Check login status
Write-Output "4. Checking login status..."
$checkBody = '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"checkLoginStatus","arguments":{}},"id":3}'
try {
    $checkRes = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $checkBody -WebSession $ws -TimeoutSec 60).Content
    Write-Output $checkRes
    Write-Output ""
    
    # Check for login status - use simple string match
    if ($checkRes -like '*"isLoggedIn":true*') {
        Write-Output "========================================="
        Write-Output "SUCCESS: QR code login completed!"
        Write-Output "========================================="
    }
    else {
        Write-Output "WARNING: Login status check failed"
    }
}
catch {
    Write-Output "FAILED to check login status: $_"
}
Write-Output ""

Write-Output "=== Test Complete ==="
