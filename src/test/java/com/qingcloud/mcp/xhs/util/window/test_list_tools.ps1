$baseUrl = "http://localhost:8080/mcp"

Write-Output "=== Listing Available MCP Tools ==="
Write-Output ""

# Initialize session
Write-Output "1. Initializing MCP session..."
$initBody = '{"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0"}},"id":1}'
$initRes = Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream" } -ContentType "application/json" -Body $initBody -SessionVariable ws
$sessionId = $initRes.Headers["Mcp-Session-Id"]
Write-Output "Session ID: $sessionId"
Write-Output ""

# List tools
Write-Output "2. Listing all available tools..."
$listBody = '{"jsonrpc":"2.0","method":"tools/list","params":{},"id":2}'
try {
    $listRes = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $listBody -WebSession $ws -TimeoutSec 30).Content
    
    Write-Output "Tools list response:"
    Write-Output $listRes
    $listRes | Out-File -FilePath "tools_list.txt" -Encoding utf8
    Write-Output ""
    Write-Output "Result saved to tools_list.txt"
    Write-Output ""
    
    # Try to extract and display tool names
    if ($listRes -match '"tools"\s*:\s*\[') {
        Write-Output "Registered tools:"
        $toolMatches = [regex]::Matches($listRes, '"name"\s*:\s*"([^"]+)"')
        foreach ($match in $toolMatches) {
            Write-Output "  - $($match.Groups[1].Value)"
        }
    }
}
catch {
    Write-Output "FAILED to list tools: $_"
}
Write-Output ""

Write-Output "=== Test Complete ==="
