$baseUrl = "http://localhost:8080/mcp"

Write-Output "=== Quick Login Test (Using setCookies for Development) ==="
Write-Output ""
Write-Output "NOTE: This script uses setCookies tool to quickly set cookies for testing."
Write-Output "For production, use test_login_qrcode.ps1 instead."
Write-Output ""

# Initialize session
Write-Output "1. Initializing MCP session..."
$initBody = '{"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0"}},"id":1}'
$initRes = Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream" } -ContentType "application/json" -Body $initBody -SessionVariable ws
$sessionId = $initRes.Headers["Mcp-Session-Id"]
Write-Output "Session ID: $sessionId"
Write-Output ""

# Check if cookies.json exists
if (-not (Test-Path "..\..\..\..\..\..\..\..\cookies.json")) {
    Write-Output "ERROR: cookies.json not found!"
    Write-Output "Please run test_login_qrcode.ps1 first to get cookies, or manually create cookies.json"
    Write-Output ""
    Write-Output "=== Test Aborted ==="
    exit 1
}

# Read cookies from file
Write-Output "2. Reading cookies from cookies.json..."
$cookiesContent = Get-Content "..\..\..\..\..\..\..\..\cookies.json" -Raw
$cookiesArray = $cookiesContent | ConvertFrom-Json

# Convert cookies array to cookie string
$cookieString = ($cookiesArray | ForEach-Object { "$($_.name)=$($_.value)" }) -join "; "
Write-Output "Found $($cookiesArray.Count) cookies"
Write-Output ""

# Call setCookies tool
Write-Output "3. Setting cookies using setCookies tool..."
$setCookiesBody = @"
{"jsonrpc":"2.0","method":"tools/call","params":{"name":"setCookies","arguments":{"cookieString":"$cookieString"}},"id":2}
"@

try {
    $setCookiesRes = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $setCookiesBody -WebSession $ws -TimeoutSec 30).Content
    Write-Output "setCookies response:"
    Write-Output $setCookiesRes
    Write-Output ""
}
catch {
    Write-Output "FAILED to set cookies: $_"
    Write-Output ""
    Write-Output "=== Test Aborted ==="
    exit 1
}

# Check login status
Write-Output "4. Checking login status..."
$checkBody = '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"checkLoginStatus","arguments":{}},"id":3}'
try {
    $checkRes = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $checkBody -WebSession $ws -TimeoutSec 30).Content
    Write-Output $checkRes
    Write-Output ""
    
    if ($checkRes.Contains('"isLoggedIn":true')) {
        Write-Output "SUCCESS: Logged in using cookies"
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
