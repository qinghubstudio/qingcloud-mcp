# Qingcloud MCP Service

基于 Model Context Protocol (MCP) 的小红书 API 工具服务，为 AI Agents 提供小红书核心操作能力。

## 快速开始

### 1. 构建项目

```bash
cd qingcloud-mcp
mvn clean package -DskipTests
```

### 2. 运行模式

服务支持两种传输模式：**STDIO** 和 **HTTP**。

---

#### 模式一：STDIO 模式（默认）

适用于 Claude Desktop 等本地 MCP 客户端。

**启动方式**: 通过 MCP 客户端配置启动

**claude_desktop_config.json:**

```json
{
  "mcpServers": {
    "xhs-mcp": {
      "command": "java",
      "args": [
        "-Dmcp.transport.mode=stdio",
        "-jar",
        "C:/workspace/jinghui/backend/common/qingcloud-mcp/target/qingcloud-mcp-0.1.0-SNAPSHOT.jar"
      ]
    }
  }
}
```

---

#### 模式二：HTTP 流式传输模式

适用于远程 MCP 客户端或 Web 应用集成。

**启动方式**:

```bash
java -jar target/qingcloud-mcp-0.1.0-SNAPSHOT.jar
```

服务器默认在 `http://localhost:8080/mcp` 启动。

**MCP 客户端配置:**

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

**测试连接:**

```bash
# 初始化会话
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d '{"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"curl-test","version":"1.0"}},"id":1}'
```

---

### 3. 导入 Cookie

从浏览器导入小红书 Cookie 以启用 API 访问：

```
工具: setCookies
参数: {
  "cookieString": "a1=xxx; webId=yyy; web_session=zzz; ..."
}
```

## 可用工具

### 会话管理

| 工具名             | 描述         | 参数                 |
| ------------------ | ------------ | -------------------- |
| `login`            | 登录         | username, password   |
| `checkLoginStatus` | 检查登录状态 | 无                   |
| `setCookies`       | 导入 Cookie  | cookies/cookieString |

### 内容操作

| 工具名           | 描述     | 必填参数           | 可选参数                 |
| ---------------- | -------- | ------------------ | ------------------------ |
| `searchNotes`    | 搜索笔记 | keyword            | page, pageSize, sortType |
| `getFeed`        | 首页推荐 | -                  | cursor, num, category    |
| `getNoteDetail`  | 笔记详情 | noteId             | xsecSource, xsecToken    |
| `publishPost`    | 发布笔记 | title, description | imageUrls                |
| `postComment`    | 发评论   | noteId, content    | atUsers                  |
| `getUserProfile` | 用户主页 | userId             | cursor, num              |

## 使用示例

### 搜索笔记

```json
{
  "tool": "searchNotes",
  "arguments": {
    "keyword": "美食推荐",
    "page": 1,
    "pageSize": 20,
    "sortType": "hot"
  }
}
```

### 获取笔记详情

```json
{
  "tool": "getNoteDetail",
  "arguments": {
    "noteId": "64abc123def456"
  }
}
```

### 发布评论

```json
{
  "tool": "postComment",
  "arguments": {
    "noteId": "64abc123def456",
    "content": "写得真好！"
  }
}
```

## 注意事项

> ⚠️ **签名说明**: 当前使用本地 MD5 签名，实际小红书 API 需要动态签名，可能需要配合浏览器扩展或 Playwright 获取有效签名。

> 💡 **Cookie 获取**: 建议从浏览器 DevTools → Application → Cookies 复制完整 Cookie 字符串。

## 技术架构

```
MCP Client (Claude/AI Agent/Web App)
        ↓ STDIO / HTTP+SSE
    MCP Server
        ↓
    Tool Router
        ↓
    RedClient (HTTP + Cookie + Signature)
        ↓
    Xiaohongshu API
```

## 配置项

| 配置项               | 默认值 | 说明                  |
| -------------------- | ------ | --------------------- |
| `server.port`        | 8080   | HTTP 服务端口         |
| `mcp.transport.mode` | http   | 传输模式 (stdio/http) |
| `mcp.http.endpoint`  | /mcp   | HTTP 端点路径         |

## 版本

- 当前版本: **0.4.0**
- Java: 17
- Spring Boot: 3.5.7
- MCP SDK: 0.17.0
