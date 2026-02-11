$baseUrl = "http://localhost:8080/mcp"

Write-Output "=== Testing getFeeds Tool ==="
Write-Output ""

# Initialize session
Write-Output "1. Initializing MCP session..."
$initBody = '{"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0"}},"id":1}'
$initRes = Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream" } -ContentType "application/json" -Body $initBody -SessionVariable ws
$sessionId = $initRes.Headers["Mcp-Session-Id"]
Write-Output "Session ID: $sessionId"
Write-Output ""

# Call getFeeds
Write-Output "2. Calling getFeeds tool..."
$feedsBody = '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"getFeeds","arguments":{}},"id":2}'
try {
    $feedsRes = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $feedsBody -WebSession $ws -TimeoutSec 60).Content
    
    Write-Output "SUCCESS: getFeeds returned data"
    $feedsRes | Out-File -FilePath "test_getfeeds_result.txt" -Encoding utf8
    
    # Parse and display summary
    try {
        $jsonStart = $feedsRes.IndexOf('{"code"')
        if ($jsonStart -ge 0) {
            $jsonEnd = $feedsRes.LastIndexOf('}"}]')
            if ($jsonEnd -gt $jsonStart) {
                $jsonStr = $feedsRes.Substring($jsonStart, $jsonEnd - $jsonStart + 4)
                $data = $jsonStr | ConvertFrom-Json
                
                if ($data.success) {
                    Write-Output "Feed count: $($data.data.total)"
                    Write-Output "First 3 feeds:"
                    for ($i = 0; $i -lt [Math]::Min(3, $data.data.items.Count); $i++) {
                        $item = $data.data.items[$i]
                        Write-Output "  - $($item.noteCard.displayTitle) (by $($item.noteCard.user.nickName))"
                    }
                }
            }
        }
    }
    catch {
        Write-Output "Could not parse feed data"
    }
    
    Write-Output ""
    Write-Output "Full result saved to test_getfeeds_result.txt"
}
catch {
    Write-Output "FAILED: $_"
}
Write-Output ""

Write-Output "=== Test Complete ==="
