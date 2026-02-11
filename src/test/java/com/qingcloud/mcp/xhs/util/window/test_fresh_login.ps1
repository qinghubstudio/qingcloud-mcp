$baseUrl = "http://localhost:8080/mcp"

Write-Output "=== Testing Fresh Login (No Cookies) ==="
Write-Output ""

# Initialize session
Write-Output "1. Initializing MCP session..."
$initBody = '{"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0"}},"id":1}'
$initRes = Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream" } -ContentType "application/json" -Body $initBody -SessionVariable ws
$sessionId = $initRes.Headers["Mcp-Session-Id"]
Write-Output "Session ID: $sessionId"
Write-Output ""

# Call login tool
Write-Output "2. Calling login tool (should return QR code URL)..."
Write-Output "NOTE: In headless mode, you'll receive a QR code URL to scan"
Write-Output ""

$loginBody = '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"login","arguments":{}},"id":2}'
try {
    # Set a longer timeout for login (180 seconds to allow time for scanning)
    $loginRes = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $loginBody -WebSession $ws -TimeoutSec 180).Content
    
    Write-Output "Login response received:"
    Write-Output $loginRes
    Write-Output ""
    
    # Save response
    $loginRes | Out-File -FilePath "fresh_login_result.txt" -Encoding utf8
    Write-Output "Response saved to fresh_login_result.txt"
    Write-Output ""
    
    # Try to parse and extract QR code URL
    try {
        $jsonStart = $loginRes.IndexOf('{"status"')
        if ($jsonStart -ge 0) {
            $jsonEnd = $loginRes.IndexOf('"}]', $jsonStart) + 3
            $jsonStr = $loginRes.Substring($jsonStart, $jsonEnd - $jsonStart)
            $loginData = $jsonStr | ConvertFrom-Json
            
            if ($loginData.qrcodeUrl) {
                Write-Output "QR Code URL extracted:"
                Write-Output $loginData.qrcodeUrl
                Write-Output ""
                Write-Output "Please open this URL in your browser or scan with your phone to login."
            }
        }
    }
    catch {
        Write-Output "Could not extract QR code URL from response"
    }
    
}
catch {
    Write-Output "Login call failed: $_"
    Write-Output ""
}

# Check if cookies were saved
Write-Output "3. Checking if cookies.json was created..."
if (Test-Path "cookies.json") {
    Write-Output "SUCCESS: cookies.json file created"
    $cookies = Get-Content "cookies.json" | ConvertFrom-Json
    Write-Output "Cookie count: $($cookies.Count)"
}
else {
    Write-Output "INFO: cookies.json not found (expected if login timed out or not completed)"
}
Write-Output ""

Write-Output "=== Test Complete ==="
