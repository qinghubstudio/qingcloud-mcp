$baseUrl = "http://localhost:8080/mcp"

Write-Output "=== Testing publish_content Tool ==="
Write-Output ""
Write-Output "NOTE: This test requires login first"
Write-Output ""

# Initialize session
Write-Output "1. Initializing MCP session..."
$initBody = '{"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0"}},"id":1}'
$initRes = Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream" } -ContentType "application/json" -Body $initBody -SessionVariable ws
$sessionId = $initRes.Headers["Mcp-Session-Id"]
Write-Output "Session ID: $sessionId"
Write-Output ""

# Check login status first
Write-Output "2. Checking login status..."
$checkBody = '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"checkLoginStatus","arguments":{}},"id":2}'
try {
    $checkRes = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $checkBody -WebSession $ws -TimeoutSec 30).Content
    Write-Output $checkRes
    Write-Output ""
    
    if ($checkRes -notmatch '"isLoggedIn"\s*:\s*true') {
        Write-Output "ERROR: Not logged in. Please run test_login_simple.ps1 first"
        Write-Output ""
        Write-Output "=== Test Aborted ==="
        exit 1
    }
}
catch {
    Write-Output "FAILED to check login status: $_"
    Write-Output ""
    Write-Output "=== Test Aborted ==="
    exit 1
}

# Test with local image (you need to provide a valid image path)
Write-Output "3. Testing publish_content with local image..."
Write-Output "NOTE: Please update the image path in the script to a valid local image"
Write-Output ""

# Example with a placeholder image path - UPDATE THIS
$imagePath = "C:\Users\Public\Pictures\Sample Pictures\test.jpg"

# Check if image exists
if (-not (Test-Path $imagePath)) {
    Write-Output "WARNING: Test image not found at: $imagePath"
    Write-Output "Please create a test image or update the path in the script"
    Write-Output ""
    Write-Output "Skipping publish test..."
    Write-Output ""
    Write-Output "=== Test Complete (Skipped) ==="
    exit 0
}

# Build publish request
$publishBody = @"
{"jsonrpc":"2.0","method":"tools/call","params":{"name":"publish_content","arguments":{"title":"自动化测试发布","content":"这是一条自动化测试内容，请忽略。","images":["$($imagePath -replace '\\', '\\')"],"tags":["测试","自动化"]}},,"id":3}
"@

Write-Output "Publishing content..."
Write-Output "Title: 自动化测试发布"
Write-Output "Image: $imagePath"
Write-Output ""

try {
    $publishRes = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $publishBody -WebSession $ws -TimeoutSec 300).Content
    
    Write-Output "Publish response received:"
    Write-Output $publishRes
    $publishRes | Out-File -FilePath "test_publish_result.txt" -Encoding utf8
    Write-Output ""
    Write-Output "Result saved to test_publish_result.txt"
    
    # Check if successful
    if ($publishRes -match '"success"\s*:\s*true') {
        Write-Output ""
        Write-Output "✓ Publish test PASSED"
    }
    else {
        Write-Output ""
        Write-Output "✗ Publish test FAILED - check result file for details"
    }
}
catch {
    Write-Output "Publish FAILED: $_"
    Write-Output ""
    Write-Output "✗ Publish test FAILED"
}
Write-Output ""

Write-Output "=== Test Complete ==="
