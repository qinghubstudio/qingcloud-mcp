$baseUrl = "http://localhost:8080/mcp"

Write-Output "=== Testing searchNotes Tool ==="
Write-Output ""

# Initialize session
Write-Output "1. Initializing MCP session..."
$initBody = '{"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0"}},"id":1}'
$initRes = Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream" } -ContentType "application/json" -Body $initBody -SessionVariable ws
$sessionId = $initRes.Headers["Mcp-Session-Id"]
Write-Output "Session ID: $sessionId"
Write-Output ""

# Call searchNotes
Write-Output "2. Calling searchNotes tool with keyword '美食'..."
$searchBody = '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"searchNotes","arguments":{"keyword":"美食","page":1,"page_size":10}},"id":2}'
try {
    $searchRes = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $searchBody -WebSession $ws -TimeoutSec 60).Content
    
    Write-Output "SUCCESS: searchNotes returned data"
    $searchRes | Out-File -FilePath "test_search_result.txt" -Encoding utf8
    
    # Parse and display summary
    try {
        $jsonStart = $searchRes.IndexOf('{"code"')
        if ($jsonStart -ge 0) {
            $jsonEnd = $searchRes.LastIndexOf('}"}]')
            if ($jsonEnd -gt $jsonStart) {
                $jsonStr = $searchRes.Substring($jsonStart, $jsonEnd - $jsonStart + 4)
                $data = $jsonStr | ConvertFrom-Json
                
                if ($data.success) {
                    Write-Output "Search results count: $($data.data.total)"
                    Write-Output "First 3 results:"
                    for ($i = 0; $i -lt [Math]::Min(3, $data.data.items.Count); $i++) {
                        $item = $data.data.items[$i]
                        Write-Output "  - $($item.noteCard.displayTitle) (by $($item.noteCard.user.nickName))"
                    }
                }
            }
        }
    }
    catch {
        Write-Output "Could not parse search data"
    }
    
    Write-Output ""
    Write-Output "Full result saved to test_search_result.txt"
}
catch {
    Write-Output "FAILED: $_"
}
Write-Output ""

Write-Output "=== Test Complete ==="
