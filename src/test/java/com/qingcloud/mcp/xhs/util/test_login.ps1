# 测试登录功能
$baseUrl = "http://localhost:8080/mcp"

Write-Host "=== 测试登录功能 ===" -ForegroundColor Cyan
Write-Host ""

# 1. 初始化会话
Write-Host "1. 初始化MCP会话..." -ForegroundColor Yellow
$initBody = @{
    jsonrpc = "2.0"
    method  = "initialize"
    params  = @{
        protocolVersion = "2024-11-05"
        capabilities    = @{}
        clientInfo      = @{
            name    = "test-client"
            version = "1.0"
        }
    }
    id      = 1
} | ConvertTo-Json -Depth 10

try {
    $initRes = Invoke-WebRequest -Uri $baseUrl -Method Post `
        -Headers @{"Accept" = "application/json, text/event-stream" } `
        -ContentType "application/json" `
        -Body $initBody `
        -SessionVariable ws `
        -TimeoutSec 30
    
    $sessionId = $initRes.Headers["Mcp-Session-Id"]
    Write-Host "✓ 会话初始化成功" -ForegroundColor Green
    Write-Host "  Session ID: $sessionId" -ForegroundColor Gray
    Write-Host ""
}
catch {
    Write-Host "✗ 会话初始化失败: $_" -ForegroundColor Red
    exit 1
}

# 2. 调用登录工具
Write-Host "2. 调用登录工具..." -ForegroundColor Yellow
Write-Host "  提示: 浏览器窗口将会打开,请在窗口中扫描二维码登录" -ForegroundColor Cyan
Write-Host ""

$loginBody = @{
    jsonrpc = "2.0"
    method  = "tools/call"
    params  = @{
        name      = "login"
        arguments = @{}
    }
    id      = 2
} | ConvertTo-Json -Depth 10

try {
    Write-Host "  等待登录完成(最多120秒)..." -ForegroundColor Gray
    $loginRes = Invoke-WebRequest -Uri $baseUrl -Method Post `
        -Headers @{
        "Accept"         = "application/json, text/event-stream"
        "Mcp-Session-Id" = $sessionId
    } `
        -ContentType "application/json" `
        -Body $loginBody `
        -WebSession $ws `
        -TimeoutSec 180
    
    $loginResult = $loginRes.Content | ConvertFrom-Json
    Write-Host "✓ 登录工具调用完成" -ForegroundColor Green
    Write-Host ""
    Write-Host "登录结果:" -ForegroundColor Cyan
    Write-Host $loginRes.Content -ForegroundColor Gray
    Write-Host ""
    
    # 保存结果到文件
    $loginRes.Content | Out-File -FilePath "login_test_result.txt" -Encoding utf8
    Write-Host "  结果已保存到: login_test_result.txt" -ForegroundColor Gray
    Write-Host ""
    
}
catch {
    Write-Host "✗ 登录工具调用失败: $_" -ForegroundColor Red
    Write-Host ""
}

# 3. 检查cookies文件
Write-Host "3. 检查cookies文件..." -ForegroundColor Yellow
if (Test-Path "cookies.json") {
    $cookieContent = Get-Content "cookies.json" -Raw
    $cookies = $cookieContent | ConvertFrom-Json
    Write-Host "✓ cookies.json 文件存在" -ForegroundColor Green
    Write-Host "  Cookie数量: $($cookies.Count)" -ForegroundColor Gray
    Write-Host ""
}
else {
    Write-Host "✗ cookies.json 文件不存在" -ForegroundColor Red
    Write-Host ""
}

# 4. 测试登录状态检查
Write-Host "4. 测试登录状态检查..." -ForegroundColor Yellow
$checkLoginBody = @{
    jsonrpc = "2.0"
    method  = "tools/call"
    params  = @{
        name      = "checkLoginStatus"
        arguments = @{}
    }
    id      = 3
} | ConvertTo-Json -Depth 10

try {
    $checkRes = Invoke-WebRequest -Uri $baseUrl -Method Post `
        -Headers @{
        "Accept"         = "application/json, text/event-stream"
        "Mcp-Session-Id" = $sessionId
    } `
        -ContentType "application/json" `
        -Body $checkLoginBody `
        -WebSession $ws `
        -TimeoutSec 60
    
    Write-Host "✓ 登录状态检查完成" -ForegroundColor Green
    Write-Host ""
    Write-Host "登录状态:" -ForegroundColor Cyan
    Write-Host $checkRes.Content -ForegroundColor Gray
    Write-Host ""
    
}
catch {
    Write-Host "✗ 登录状态检查失败: $_" -ForegroundColor Red
    Write-Host ""
}

Write-Host "=== 测试完成 ===" -ForegroundColor Cyan
