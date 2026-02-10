# MCP 配置说明

## 问题描述

如果遇到错误：`Server "xhs-mcp" must have either a command (for stdio) or url (for SSE)`

说明 MCP 服务器配置缺少必要的 `command`（用于 STDIO 模式）或 `url`（用于 SSE/HTTP 模式）。

## 解决方案

### 方案一：STDIO 模式（推荐用于 Cursor）

在项目根目录创建 `.cursor/mcp.json` 文件：

```json
{
  "mcpServers": {
    "xhs-mcp": {
      "command": "java",
      "args": [
        "-Dmcp.transport.mode=stdio",
        "-jar",
        "C:/workspace/jinghui/backend/common/qingcloud-mcp/target/qingcloud-mcp-0.1.0-SNAPSHOT.jar"
      ],
      "env": {}
    }
  }
}
```

**注意**：
- 请将 JAR 文件路径替换为你的实际路径
- 路径使用正斜杠 `/` 或双反斜杠 `\\`
- 确保 JAR 文件已构建（运行 `mvn clean package -DskipTests`）

### 方案二：HTTP 模式

如果使用 HTTP 模式，需要：

1. **启动服务器**：
```bash
java -Dmcp.transport.mode=http -jar target/qingcloud-mcp-0.1.0-SNAPSHOT.jar
```

2. **配置 MCP 客户端**（`.cursor/mcp.json`）：
```json
{
  "mcpServers": {
    "xhs-mcp": {
      "url": "http://localhost:8080/mcp",
      "transport": "streamable-http"
    }
  }
}
```

## 验证配置

1. **检查 JAR 文件是否存在**：
```bash
# Windows PowerShell
Test-Path target\qingcloud-mcp-0.1.0-SNAPSHOT.jar

# 如果不存在，构建项目
mvn clean package -DskipTests
```

2. **检查 Java 版本**（需要 Java 17+）：
```bash
java -version
```

3. **测试 STDIO 模式**（手动测试）：
```bash
java -Dmcp.transport.mode=stdio -jar target/qingcloud-mcp-0.1.0-SNAPSHOT.jar
```

如果配置正确，应该能看到 MCP 服务器启动信息。

## 常见问题

### 1. JAR 文件不存在

**错误**：找不到 JAR 文件

**解决**：
```bash
mvn clean package -DskipTests
```

### 2. Java 版本不匹配

**错误**：需要 Java 17 或更高版本

**解决**：安装 Java 17+ 并确保 `java` 命令在 PATH 中

### 3. 路径问题

**错误**：路径找不到或格式错误

**解决**：
- Windows 使用正斜杠 `/` 或双反斜杠 `\\`
- 使用绝对路径
- 检查路径中的空格（可能需要引号）

### 4. 端口被占用（HTTP 模式）

**错误**：端口 8080 已被占用

**解决**：
- 修改 `application.yml` 中的 `server.port`
- 或关闭占用端口的程序

## 配置文件位置

- **项目级配置**：`<project-root>/.cursor/mcp.json`（推荐）
- **全局配置**：`~/.cursor/mcp.json`（Windows: `C:\Users\<username>\.cursor\mcp.json`）

项目级配置优先级高于全局配置。

## 重启 Cursor

配置完成后，需要重启 Cursor IDE 才能生效。


