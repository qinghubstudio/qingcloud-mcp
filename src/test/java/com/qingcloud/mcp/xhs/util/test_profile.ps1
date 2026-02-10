
$baseUrl = "http://localhost:8080/mcp"
Write-Output "Initializing session..."
$session = Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream" } -ContentType "application/json" -Body '{"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0"}},"id":1}' -SessionVariable ws
$sessionId = $session.Headers["Mcp-Session-Id"]
Write-Output "Session ID: $sessionId"

# 1. Get Feeds to find a user
Write-Output "Calling getFeeds..."
$feedsBody = '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"getFeeds","arguments":{}},"id":3}'
$feedsResponseText = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $feedsBody -WebSession $ws -TimeoutSec 120).Content

# Parse out note content - avoid ConvertFrom-Json due to duplicate keys issue
$feedsResponse = $feedsResponseText -split "`n" | Where-Object { $_ -like 'data: *' } | Select-Object -First 1
$feedsJsonText = $feedsResponse.Substring(6)

# Extract userId and xsecToken using regex to avoid JSON parsing issues
$userId = $null
$userXsecToken = $null
if ($feedsJsonText -match '"userId"\s*:\s*"([^"]+)"') {
    $userId = $Matches[1]
}
if ($feedsJsonText -match '"xsecToken"\s*:\s*"([^"]+)"') {
    $userXsecToken = $Matches[1]
}

Write-Output "Found User ID: $userId"
Write-Output "Found User XsecToken: $userXsecToken"

if (!$userXsecToken) {
    Write-Output "No xsecToken found for user, attempting search for more results..."
    $searchBody = '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"searchNotes","arguments":{"keyword":"travel"}},"id":4}'
    $searchResponseText = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $searchBody -WebSession $ws -TimeoutSec 120).Content
    $searchResponse = $searchResponseText -split "`n" | Where-Object { $_ -like 'data: *' } | Select-Object -First 1
    $searchJsonText = $searchResponse.Substring(6)
    
    # Extract userId and xsecToken using regex
    if ($searchJsonText -match '"userId"\s*:\s*"([^"]+)"') {
        $userId = $Matches[1]
    }
    if ($searchJsonText -match '"xsecToken"\s*:\s*"([^"]+)"') {
        $userXsecToken = $Matches[1]
    }
    Write-Output "From search - User ID: $userId, XsecToken: $userXsecToken"
}

# 2. Get User Profile
Write-Output "Calling getUserProfile..."
$profileBody = @{
    jsonrpc = "2.0"
    method  = "tools/call"
    params  = @{
        name      = "getUserProfile"
        arguments = @{
            userId    = $userId
            xsecToken = $userXsecToken
        }
    }
    id      = 5
} | ConvertTo-Json

$profileResponseText = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $profileBody -WebSession $ws -TimeoutSec 120).Content
Write-Output "Profile Response:"
$profileResponseText
