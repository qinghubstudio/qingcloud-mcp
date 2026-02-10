
$baseUrl = "http://localhost:8080/mcp"
Write-Output "=== Debug getUserProfile Tool ==="
Write-Output ""

# 1. Initialize session
Write-Output "1. Initializing session..."
$session = Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream" } -ContentType "application/json" -Body '{\"jsonrpc\":\"2.0\",\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{},\"clientInfo\":{\"name\":\"test-client\",\"version\":\"1.0\"}},\"id\":1}' -SessionVariable ws
$sessionId = $session.Headers["Mcp-Session-Id"]
Write-Output "Session ID: $sessionId"
Write-Output ""

# 2. Get fresh userId and xsecToken from getFeeds
Write-Output "2. Getting fresh userId and xsecToken from getFeeds..."
$feedsBody = '{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\",\"params\":{\"name\":\"getFeeds\",\"arguments\":{}},\"id\":2}'
$feedsResponseText = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $feedsBody -WebSession $ws -TimeoutSec 120).Content

# Extract first data line
$feedsResponse = $feedsResponseText -split "`n" | Where-Object { $_ -like 'data: *' } | Select-Object -First 1
$feedsJsonText = $feedsResponse.Substring(6)

# Extract userId and xsecToken using regex
$userId = $null
$userXsecToken = $null
if ($feedsJsonText -match '"userId"\s*:\s*"([^"]+)"') {
    $userId = $Matches[1]
}
if ($feedsJsonText -match '"xsecToken"\s*:\s*"([^"]+)"') {
    $userXsecToken = $Matches[1]
}

Write-Output "Extracted User ID: $userId"
Write-Output "Extracted XsecToken: $userXsecToken"
Write-Output ""

if (!$userId -or !$userXsecToken) {
    Write-Output "ERROR: Failed to extract userId or xsecToken from getFeeds response"
    exit 1
}

# 3. Call getUserProfile with debug enabled
Write-Output "3. Calling getUserProfile with extracted credentials..."
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
    id      = 3
} | ConvertTo-Json

$profileResponseText = (Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Accept" = "application/json, text/event-stream"; "Mcp-Session-Id" = $sessionId } -ContentType "application/json" -Body $profileBody -WebSession $ws -TimeoutSec 120).Content

Write-Output "Profile Response:"
Write-Output $profileResponseText
Write-Output ""

# Parse the response
$profileDataLine = $profileResponseText -split "`n" | Where-Object { $_ -like 'data: *' } | Select-Object -First 1
if ($profileDataLine) {
    $profileJson = $profileDataLine.Substring(6)
    Write-Output "Parsed Profile Data:"
    Write-Output $profileJson
    
    # Check if profile and notes are empty
    if ($profileJson -match '"profile"\s*:\s*\{\s*\}') {
        Write-Output ""
        Write-Output "WARNING: Profile object is empty!"
    }
    if ($profileJson -match '"notes"\s*:\s*\[\s*\]') {
        Write-Output ""
        Write-Output "WARNING: Notes array is empty!"
    }
}

Write-Output ""
Write-Output "=== Debug Complete ==="
