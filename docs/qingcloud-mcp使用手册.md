# 小红书 MCP 客户端使用指南

本指南提供了通过 MCP 客户端（如 Claude Desktop）调用小红书工具的完整示例。

## 1. 配置说明

### Claude Desktop 配置

编辑 `%APPDATA%\Claude\claude_desktop_config.json`:

**方式一: HTTP 流模式 (推荐)**

```json
{
  "mcpServers": {
    "xhs-mcp": {
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

**方式二: Stdio 模式**

```json
{
  "mcpServers": {
    "xhs-mcp": {
      "command": "java",
      "args": [
        "-jar",
        "C:/workspace/jinghui/backend/common/qingcloud-mcp/target/qingcloud-mcp-0.1.0-SNAPSHOT.jar"
      ]
    }
  }
}
```

**注意**:

- HTTP 流模式需要先启动服务: `mvn spring-boot:run` 或 `java -jar target/qingcloud-mcp-0.1.0-SNAPSHOT.jar`
- Stdio 模式会自动启动服务,但每次调用都会重新启动

重启 Claude Desktop 后，即可在对话中使用小红书工具。

---

## 2. 工具调用示例

### 1️⃣ 设置 Cookie (首次使用必须)

从浏览器复制 Cookie 后调用 `setCookies`。

**参数:**

- `cookieString` (必需): Cookie 字符串,格式为 `name1=value1; name2=value2; ...`

```json
{
  "name": "setCookies",
  "arguments": {
    "cookieString": "a1=18f234abc...; webId=xyz123...; web_session=abc456..."
  }
}
```

### 2️⃣ 登录 (Login)

获取二维码并等待用户扫码登录。无需参数。

```json
{
  "name": "login",
  "arguments": {}
}
```

**返回示例:**

- `status`: `already_logged_in` | `success` | `timeout` | `error`
- `qrcodeUrl`: 二维码图片 URL (如果需要扫码)
- `message`: 状态消息

### 3️⃣ 检查登录状态

检查当前是否已登录小红书。无需参数。

```json
{
  "name": "checkLoginStatus",
  "arguments": {}
}
```

### 4️⃣ 搜索笔记 (Search Notes)

搜索指定关键词的笔记。

**参数:**

- `keyword` (必需): 搜索关键词
- `page` (可选): 页码,默认 1
- `page_size` (可选): 每页数量,默认 20

```json
{
  "name": "searchNotes",
  "arguments": {
    "keyword": "咖啡推荐",
    "page": 1,
    "page_size": 10
  }
}
```

### 5️⃣ 获取首页推荐 (Get Feeds)

获取小红书首页的推荐内容列表。无需参数。

```json
{
  "name": "getFeeds",
  "arguments": {}
}
```

### 6️⃣ 获取笔记详情 (Get Post Detail)

获取特定笔记的详细信息。

**参数:**

- `noteId` (必需): 笔记 ID
- `xsecToken` (必需): 访问令牌,从 Feed 列表或搜索结果中获取

```json
{
  "name": "getPostDetail",
  "arguments": {
    "noteId": "64abc123def456",
    "xsecToken": "ABWL-..."
  }
}
```

### 7️⃣ 获取用户资料 (Get User Profile)

获取指定用户的公开资料。

**参数:**

- `userId` (必需): 用户 ID
- `xsecToken` (必需): 访问令牌,从 Feed 列表中获取

```json
{
  "name": "getUserProfile",
  "arguments": {
    "userId": "5a123456789...",
    "xsecToken": "ABWL-..."
  }
}
```

### 8️⃣ 发表评论 (Post Comment)

在指定笔记下发表评论。

**参数:**

- `noteId` (必需): 笔记 ID
- `xsecToken` (必需): 访问令牌,从 Feed 列表中获取
- `content` (必需): 评论内容

```json
{
  "name": "postComment",
  "arguments": {
    "noteId": "64abc123def456",
    "xsecToken": "ABWL-...",
    "content": "不错的分享,很实用!"
  }
}
```

### 9️⃣ 发布图文笔记 (Publish Content)

发布包含图片的笔记。

**参数:**

- `title` (必需): 标题,最多 20 个中文字符或 40 个英文单词
- `content` (必需): 正文内容,不包含#标签
- `images` (必需): 图片路径列表,至少 1 张。支持:
  - HTTP/HTTPS URL (自动下载)
  - 本地绝对路径 (推荐)
- `tags` (可选): 话题标签列表

```json
{
  "name": "publish_content",
  "arguments": {
    "title": "周末探店｜超棒的咖啡馆",
    "content": "今天发现了一家宝藏店铺，环境很好，咖啡也很好喝。",
    "images": ["C:\\Users\\Photos\\cafe1.jpg", "https://example.com/cafe2.jpg"],
    "tags": ["探店", "咖啡", "周末去哪儿"]
  }
}
```

### 🔟 发布视频笔记 (Publish Video)

发布视频笔记。

**参数:**

- `title` (必需): 标题,最多 20 个中文字符或 40 个英文单词
- `content` (必需): 正文内容,不包含#标签
- `video` (必需): 本地视频文件绝对路径
- `tags` (可选): 话题标签列表

```json
{
  "name": "publish_with_video",
  "arguments": {
    "title": "Vlog｜我的晨间日常",
    "content": "记录一下美好的早晨。",
    "video": "C:\\Users\\Videos\\morning_vlog.mp4",
    "tags": ["Vlog", "日常", "生活记录"]
  }
}
```

---

## 3. 获取浏览器 Cookie 方法

1. 打开 [小红书网页版](https://www.xiaohongshu.com)
2. 登录账号
3. 按 `F12` 打开开发者工具
4. 切换到 **Application** → **Cookies** → `www.xiaohongshu.com`
5. 复制所有 Cookie 值,格式化为: `key1=value1; key2=value2; ...`

**关键 Cookie:**

- `a1`: 设备标识
- `webId`: Web ID
- `web_session`: 会话标识

---

## 4. PowerShell 测试脚本

以下是使用 PowerShell 测试 MCP 工具的示例:

### 测试搜索功能

```powershell
$body = @{
    method = "tools/call"
    params = @{
        name = "searchNotes"
        arguments = @{
            keyword = "咖啡"
            page = 1
            page_size = 5
        }
    }
} | ConvertTo-Json -Depth 10

Invoke-RestMethod -Uri "http://localhost:8080/mcp" -Method Post -Body $body -ContentType "application/json"
```

### 测试发布图文

```powershell
$body = @{
    method = "tools/call"
    params = @{
        name = "publish_content"
        arguments = @{
            title = "测试笔记"
            content = "这是一篇测试笔记"
            images = @("C:\path\to\image.jpg")
            tags = @("测试")
        }
    }
} | ConvertTo-Json -Depth 10

Invoke-RestMethod -Uri "http://localhost:8080/mcp" -Method Post -Body $body -ContentType "application/json"
```

---

## 5. 常见问题

### Q: Cookie 失效怎么办?

A: 重新从浏览器获取 Cookie 并调用 `setCookies` 工具。Cookie 会自动保存到 `cookies.json` 文件。

### Q: 为什么需要 xsecToken?

A: `xsecToken` 是小红书的访问令牌,用于访问笔记详情、用户资料等接口。可以从 `getFeeds` 或 `searchNotes` 的返回结果中获取。

### Q: 图片上传支持哪些格式?

A: 支持常见图片格式 (JPG, PNG, GIF 等)。可以使用本地绝对路径或 HTTP/HTTPS URL。

### Q: 视频上传有什么限制?

A: 目前仅支持本地视频文件路径,不支持 URL。建议视频大小不超过 500MB。

---

## 6. 完整工作流示例

### 场景: 搜索并评论笔记

```javascript
// 1. 检查登录状态
checkLoginStatus();

// 2. 搜索笔记
searchNotes({
  keyword: "咖啡推荐",
  page: 1,
  page_size: 5,
});
// 从结果中获取 noteId 和 xsecToken

// 3. 查看笔记详情
getPostDetail({
  noteId: "64abc123def456",
  xsecToken: "ABWL-...",
});

// 4. 发表评论
postComment({
  noteId: "64abc123def456",
  xsecToken: "ABWL-...",
  content: "很棒的分享!",
});
```

### 场景: 发布新笔记

```javascript
// 1. 准备图片 (本地或URL)
const images = [
  "C:\\Users\\Photos\\photo1.jpg",
  "https://example.com/photo2.jpg",
];

// 2. 发布图文笔记
publish_content({
  title: "我的新发现",
  content: "今天发现了一个好地方,分享给大家!",
  images: images,
  tags: ["分享", "生活"],
});
```
