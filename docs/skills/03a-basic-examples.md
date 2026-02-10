# Skills 基础示例：入门指南

## 概述

本文档介绍 qingcloud-mcp 的基础功能使用，适合初学者快速上手。每个示例都是单一技能的调用，简单易懂。

## 文档导航

为便于学习，我们将基础示例按功能模块拆分：

### 📑 内容模块

- **[搜索与浏览](./03a1-search-examples.md)**
  - 搜索笔记
  - 获取推荐内容
  - 查看笔记详情

### 👤 用户模块

- **[用户信息](./03a2-user-examples.md)**
  - 获取用户主页
  - 查看用户作品
  - 检查登录状态

### 💬 互动模块

- **[互动操作](./03a3-interaction-examples.md)**
  - 发表评论
  - 点赞操作
  - 收藏管理

### 🎵 创作模块

- **[音乐生成](./03a4-music-examples.md)**
  - 生成音乐
  - 查询任务状态
  - 获取配额信息

---

## 使用前准备

### 1. 确保服务运行

```bash
# 检查服务状态
curl http://localhost:8080/actuator/health
```

### 2. 配置 AI Agent

Claude Desktop 配置示例：

```json
{
  "mcpServers": {
    "qingcloud": {
      "url": "http://localhost:8080/mcp",
      "transport": "streamable-http"
    }
  }
}
```

### 3. 导入认证信息

```
对话示例：
你：请帮我设置小红书 Cookie
AI：好的，请提供 Cookie 字符串
你：a1=xxx; webId=yyy; web_session=zzz
AI：✓ Cookie 已设置成功
```

---

## Skills 方式的核心优势

### 🤖 智能自然语言交互

**你只需描述目标，AI 自动选择合适的工具：**

```
你：帮我搜索最近的咖啡店推荐

AI 自动理解并执行：
1. 识别关键词："咖啡店推荐"
2. 选择工具：searchNotes
3. 设置参数：keyword="咖啡店推荐", sortType="hot"
4. 执行搜索
5. 返回结果
```

### 🔄 自动异常处理

**传统方式：**

```python
result = search_notes("咖啡")
if result.error:
    if error.code == 401:
        refresh_token()
        retry_search()
    elif error.code == 429:
        wait_and_retry()
    # ... 需要手动处理各种错误
```

**Skills 方式：**

```
你：搜索咖啡相关笔记

AI：（自动处理所有异常）
✓ 检测到未登录 → 提示用户设置 Cookie
✓ 检测到限流 → 自动等待后重试
✓ 返回最终结果
```

### 📊 上下文理解

**AI 能记住对话上下文：**

```
你：搜索咖啡笔记
AI：✓ 找到 20 条结果

你：看第一条的详情
AI：（理解"第一条"指的是前面搜索结果的第一条）
    ✓ 调用 getNoteDetail 获取详情

你：给它点个赞
AI：（理解"它"指的是当前查看的笔记）
    ✓ 调用点赞功能
```

---

## 快速开始

选择一个模块开始学习：

1. **新手入门**：从 [搜索示例](./03a1-search-examples.md) 开始
2. **用户管理**：查看 [用户信息示例](./03a2-user-examples.md)
3. **社交互动**：学习 [互动示例](./03a3-interaction-examples.md)
4. **内容创作**：尝试 [音乐生成](./03a4-music-examples.md)

---

## 下一步

- 掌握基础后，查看 [高级场景](./03b-advanced-scenarios.md) 学习多技能组合
- 生产环境部署参考 [最佳实践](./03c-best-practices.md)

---

## 常见问题

**Q: 为什么我的命令没有执行？**
A: 检查服务是否运行，Cookie 是否有效

**Q: 如何查看可用的工具？**
A: 直接问 AI："有哪些可用的工具？"

**Q: 出错了怎么办？**
A: AI 会自动处理大部分错误，如果持续出错请查看服务日志
