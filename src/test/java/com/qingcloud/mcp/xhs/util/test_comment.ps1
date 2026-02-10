$baseUrl = "http://localhost:8080/mcp"

Write-Output "=== Testing postComment Tool ==="
Write-Output ""
Write-Output "NOTE: Comment functionality requires proper account permissions"
Write-Output ""

# Initialize session
Write-Output "1. Initializing MCP session..."
$initBody = '{"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0"}},"id":1}'
$initRes = Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream" } -ContentType "application/json" -Body $initBody -SessionVariable ws
$sessionId = $initRes.Headers["Mcp-Session-Id"]
Write-Output "Session ID: $sessionId"
Write-Output ""

# First get a feed to obtain noteId and xsecToken
Write-Output "2. Getting feeds to obtain noteId and xsecToken..."
$feedsBody = '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"getFeeds","arguments":{}},"id":2}'
try {
    $feedsRes = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $feedsBody -WebSession $ws -TimeoutSec 60).Content
    
    # Extract first note's info
    $jsonStart = $feedsRes.IndexOf('{"code"')
    if ($jsonStart -ge 0) {
        $jsonEnd = $feedsRes.LastIndexOf('}"}]')
        if ($jsonEnd -gt $jsonStart) {
            $jsonStr = $feedsRes.Substring($jsonStart, $jsonEnd - $jsonStart + 4)
            $data = $jsonStr | ConvertFrom-Json
            
            if ($data.success -and $data.data.items.Count -gt 0) {
                $firstNote = $data.data.items[0]
                $noteId = $firstNote.id
                $xsecToken = $firstNote.xsecToken
                $noteTitle = $firstNote.noteCard.displayTitle
                
                Write-Output "Found note: $noteTitle"
                Write-Output "Note ID: $noteId"
                Write-Output ""
                
                # Call postComment
                Write-Output "3. Attempting to post comment..."
                Write-Output "WARNING: This may fail if account doesn't have comment permissions"
                Write-Output ""
                
                # Build comment request body
                $commentBody = @"
{"jsonrpc":"2.0","method":"tools/call","params":{"name":"postComment","arguments":{"noteId":"$noteId","xsecToken":"$xsecToken","content":"测试评论 - 自动化测试"}},"id":3}
"@
                
                try {
                    $commentRes = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $commentBody -WebSession $ws -TimeoutSec 60).Content
                    
                    Write-Output "Comment response received:"
                    Write-Output $commentRes
                    $commentRes | Out-File -FilePath "test_comment_result.txt" -Encoding utf8
                    Write-Output ""
                    Write-Output "Result saved to test_comment_result.txt"
                }
                catch {
                    Write-Output "Comment failed (expected): $_"
                    Write-Output "Note: Comment functionality requires proper account permissions"
                }
            }
            else {
                Write-Output "No feeds found to test comment"
            }
        }
    }
}
catch {
    Write-Output "FAILED to get feeds: $_"
}
Write-Output ""

Write-Output "=== Test Complete ==="
