# 测试脚本说明

## 登录测试脚本

### 1. test_login_qrcode.ps1 (推荐用于正式环境)

- **用途**：完整的二维码扫码登录流程
- **方法**：使用 `login` 工具
- **特点**：
  - 打开浏览器显示二维码
  - 用户扫码登录
  - 自动保存 cookies
  - 适合首次登录或 cookie 过期时使用

### 2. test_login_with_cookies.ps1 (用于开发/测试)

- **用途**：快速设置 cookies 进行测试
- **方法**：使用 `setCookies` 工具
- **特点**：
  - 从 cookies.json 读取 cookies
  - 快速设置登录状态
  - 适合开发调试时快速测试
  - 需要先运行 test_login_qrcode.ps1 获取 cookies

### 3. test_login_simple.ps1 (已废弃)

- 原有的简单登录测试
- 建议使用上述两个脚本替代

## 发布测试脚本

### test_direct_publish.ps1

- 直接测试发布功能（假设已登录）
- 用于快速测试发布流程

### test_complete_publish.ps1

- 完整的登录+发布测试
- 包含登录状态检查

## 其他测试脚本

### test_list_tools.ps1

- 列出所有可用的 MCP 工具
- 验证工具注册情况

## 使用建议

**首次使用或 cookie 过期**：

```powershell
.\test_login_qrcode.ps1
```

**开发调试时快速登录**：

```powershell
.\test_login_with_cookies.ps1
```

**测试发布功能**：

```powershell
# 确保已登录后
.\test_direct_publish.ps1
```

$p = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue; if ($p) { $id = $p.OwningProcess; Stop-Process -Id $id -Force; Write-Host "已停止端口 8080 上的进程 (PID: $id)" } else { Write-Host "端口 8080 上没有运行中的进程" }
