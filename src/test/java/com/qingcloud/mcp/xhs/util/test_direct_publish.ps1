$baseUrl = "http://localhost:8080/mcp"

Write-Output "=== Direct Publish Test (assumes already logged in) ==="
Write-Output ""

# Initialize session
Write-Output "1. Initializing MCP session..."
$initBody = '{"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0"}},"id":1}'
$initRes = Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream" } -ContentType "application/json" -Body $initBody -SessionVariable ws
$sessionId = $initRes.Headers["Mcp-Session-Id"]
Write-Output "Session ID: $sessionId"
Write-Output ""

# Publish content directly
Write-Output "2. Publishing content..."
$imagePath = "C:\workspace\jinghui\backend\common\qingcloud-mcp\src\test\java\com\qingcloud\mcp\xhs\util\girls.png"

# Check if image exists
if (-not (Test-Path $imagePath)) {
    Write-Output "ERROR: Test image not found at: $imagePath"
    Write-Output ""
    Write-Output "=== Test Aborted ==="
    exit 1
}

Write-Output "Image path: $imagePath"
Write-Output "Title: Test Publish"
Write-Output "Content: one girl."
Write-Output ""

# Escape backslashes for JSON
$imagePathJson = $imagePath -replace '\\', '\\'

# Build publish request - FIXED: removed double comma
$publishBody = @"
{"jsonrpc":"2.0","method":"tools/call","params":{"name":"publish_content","arguments":{"title":"Test Publish","content":"one girl.","images":["$imagePathJson"],"tags":["test"]}},"id":2}
"@

Write-Output "Sending publish request..."
Write-Output "NOTE: This may take 1-2 minutes (browser automation)..."
Write-Output ""

try {
    $publishRes = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $publishBody -WebSession $ws -TimeoutSec 300).Content
    
    Write-Output "Publish response:"
    Write-Output $publishRes
    $publishRes | Out-File -FilePath "test_direct_publish_result.txt" -Encoding utf8
    Write-Output ""
    Write-Output "Result saved to test_direct_publish_result.txt"
    Write-Output ""
    
    # Check if successful
    if ($publishRes.Contains('"success":true')) {
        Write-Output "========================================="
        Write-Output "SUCCESS: Content published successfully!"
        Write-Output "========================================="
    }
    elseif ($publishRes.Contains('"code":0')) {
        Write-Output "========================================="
        Write-Output "SUCCESS: Content published successfully!"
        Write-Output "========================================="
    }
    else {
        Write-Output "Response received - check result file for details"
    }
}
catch {
    Write-Output "Publish FAILED: $_"
    Write-Output ""
    Write-Output "FAILED: Publish test FAILED"
}
Write-Output ""

Write-Output "=== Test Complete ==="
