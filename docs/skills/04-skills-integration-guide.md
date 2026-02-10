# 项目集成 Agent Skills：完整指南

## 概述

本系列文档详细说明如何在**项目代码中**集成 Agent Skills，通过 API 编程方式使用（而非通过 Claude Desktop 等 IDE 工具）。

## 适用场景

✅ **适合阅读本系列的情况：**

- 需要在后端服务中集成 Claude Skills
- 构建自定义 AI Agent 应用
- 批量自动化处理任务
- 企业级 AI 能力集成
- 需要完全可控的 Skills 调用

❌ **不适合的情况：**

- 仅需个人使用 Claude（建议使用 Claude Desktop）
- 简单的对话交互（直接使用 Claude.ai 网页版）

---

## 两种集成方式对比

### 方式一：Claude 托管 Skills（推荐）

**原理：** 使用 Claude API，Skills 在 Claude 的沙箱环境中执行

```mermaid
graph LR
    App[你的应用] -->|API 请求| Claude[Claude API]
    Claude -->|执行| Skills[Skills 沙箱]
    Skills -->|结果| Claude
    Claude -->|响应| App
```

**优点：**

- ✅ 无需自建基础设施
- ✅ 安全沙箱隔离
- ✅ 自动扩展
- ✅ 官方维护

**缺点：**

- ❌ 需要 Claude API 密钥
- ❌ 按使用量计费
- ❌ 依赖网络连接

---

### 方式二：自建 MCP Server（完全自主）

**原理：** 自己实现 MCP 协议服务，完全本地化

```mermaid
graph LR
    App[你的应用] -->|直接调用| MCP[自建 MCP Server]
    MCP -->|执行| Tools[本地工具]
    Tools -->|结果| MCP
    MCP -->|响应| App
```

**优点：**

- ✅ 完全控制
- ✅ 无需外部 API
- ✅ 数据不出本地
- ✅ 免费使用

**缺点：**

- ❌ 需要自己实现 AI 推理（或调用其他 LLM）
- ❌ 维护成本高
- ❌ 需要自建基础设施

---

## 文档导航

本系列文档按集成方式分为两大部分：

### 📘 方式一：Claude API 集成（推荐新手）

1. **[04a-api-quickstart.md](./04a-api-quickstart.md)** - 快速开始

   - API 密钥获取
   - 第一个 Skill 调用
   - 基础响应处理

2. **[04b-api-container.md](./04b-api-container.md)** - Container 参数详解

   - Skills 配置
   - 多 Skills 组合
   - 版本管理

3. **[04c-custom-skills.md](./04c-custom-skills.md)** - 创建自定义 Skills

   - SKILL.md 编写
   - Skills 上传
   - 版本控制

4. **[04d-file-handling.md](./04d-file-handling.md)** - 文件处理

   - 上传文件到容器
   - 下载生成的文件
   - 文件 API 使用

5. **[04e-api-examples.md](./04e-api-examples.md)** - 完整代码示例
   - Python 集成
   - Java 集成
   - Node.js 集成

---

### 📗 方式二：自建 MCP Server（高级用户）

6. **[04f-mcp-overview.md](./04f-mcp-overview.md)** - MCP 协议概述

   - 协议原理
   - 架构设计
   - 适用场景

7. **[04g-mcp-implementation.md](./04g-mcp-implementation.md)** - 实现 MCP Server

   - Java 实现示例
   - Python 实现示例
   - Node.js 实现示例

8. **[04h-mcp-integration.md](./04h-mcp-integration.md)** - 项目集成
   - 如何调用 MCP Server
   - 错误处理
   - 性能优化

---

## 推荐学习路径

### 🎯 快速上手（Day 1）

```
04a-api-quickstart.md
    ↓
运行第一个示例
    ↓
04e-api-examples.md (选择你的编程语言)
```

### 🚀 深入掌握（Week 1）

```
04b-api-container.md
    ↓
04c-custom-skills.md
    ↓
04d-file-handling.md
    ↓
实战项目练习
```

### 🏭 生产部署（Week 2-3）

```
阅读方式二文档
    ↓
评估是否需要自建
    ↓
根据需求选择方案
```

---

## 核心概念速览

### Skills 是什么？

Skills 是 Claude 的**可执行能力扩展**：

```python
# 不使用 Skills
user: "分析这份 Excel 数据"
claude: "抱歉，我无法直接处理 Excel 文件"

# 使用 Skills
user: "分析这份 Excel 数据"
claude: [调用 excel-analysis skill]
       "已完成分析，这是结果：..."
```

### 两类 Skills

1. **Anthropic 预置 Skills**

   - 由 Anthropic 官方提供
   - 例如：`pptx`, `xlsx`, `docx`, `pdf`
   - 开箱即用
   - 持续更新

2. **自定义 Skills**
   - 你自己创建上传
   - 针对特定业务逻辑
   - 完全可控
   - 可版本化管理

---

## 环境需求

### 方式一：Claude API

- Claude API Key（从 [Console](https://platform.claude.com/settings/keys) 获取）
- 编程语言：Python 3.7+ / Java 11+ / Node.js 14+
- 网络连接

### 方式二：自建 MCP

- Java 17+ / Python 3.10+ / Node.js 16+
- 可选：Docker（用于容器化部署）
- 本地或云服务器

---

## 快速决策指南

**选择方式一（Claude API）如果：**

- ✅ 你需要快速集成
- ✅ 项目可接受 API 调用成本
- ✅ 不介意数据传输到 Claude 服务器
- ✅ 需要官方维护的 Skills

**选择方式二（自建 MCP）如果：**

- ✅ 需要完全私有化部署
- ✅ 数据不能出本地
- ✅ 有技术团队维护
- ✅ 需要自定义 AI 模型

---

## 下一步

根据你的需求选择：

- **快速开始** → [API 快速入门](./04a-api-quickstart.md)
- **了解原理** → [MCP 协议概述](./04f-mcp-overview.md)
- **查看示例** → [完整代码示例](./04e-api-examples.md)

---

## 常见问题

**Q: 我能同时使用两种方式吗？**
A: 可以！例如：开发时用 Claude API，生产环境用自建 MCP。

**Q: 哪种方式更便宜？**
A: Claude API 按使用量计费。自建 MCP 需要服务器成本但调用免费。

**Q: Claude API 有免费额度吗？**
A: 参考 [Claude 定价页面](https://www.anthropic.com/pricing)

**Q: 自建 MCP 难吗？**
A: 本项目 `qingcloud-mcp` 已经是一个完整实现，可以直接参考使用。

---

[开始学习 →](./04a-api-quickstart.md)
