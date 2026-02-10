# OpenSkills 项目概述

## 核心功能

OpenSkills 是一个**通用的 Skills 加载器**，让所有 AI 编程助手（Claude Code、Cursor、Windsurf、Aider）都能使用 Claude Code 的 Skills 系统。

## 主要特性

### 1. 完全兼容 Claude Code

- ✅ 相同的 XML 格式 (`<available_skills>`)
- ✅ 相同的 SKILL.md 格式（YAML + Markdown）
- ✅ 相同的渐进式加载
- ✅ 访问 Anthropic 官方技能市场

### 2. 扩展能力

- 从任何 GitHub 仓库安装
- 从本地路径安装
- 从私有仓库安装
- 跨多个 Agent 共享 Skills

### 3. 安装简单

```bash
npm i -g openskills
```

---

## 工作原理

### Claude Code 方式

```
用户请求 → Claude 扫描 <available_skills>
         → 调用 Skill("pdf")
         → 加载 SKILL.md 详细指令
         → 执行任务
```

### OpenSkills 方式

```
用户请求 → Agent 扫描 <available_skills>
         → 执行 Bash("openskills read pdf")
         → 输出 SKILL.md 到上下文
         → 执行任务
```

**唯一区别：调用方式**

- Claude Code: `Skill("pdf")`
- OpenSkills: `openskills read pdf`

---

## 使用示例

### 1. 安装 Skills

```bash
# 从 Anthropic 官方市场安装
openskills install anthropics/skills

# 从自定义仓库安装
openskills install your-org/custom-skills

# 从本地路径安装
openskills install ./my-local-skill
```

### 2. 同步到 AGENTS.md

```bash
openskills sync
```

生成的 AGENTS.md 内容：

```xml
<skills_system priority="1">
## Available Skills

<available_skills>
  <skill>
    <name>pdf</name>
    <description>PDF 处理工具</description>
    <location>project</location>
  </skill>
  <skill>
    <name>xlsx</name>
    <description>Excel 表格处理</description>
    <location>project</location>
  </skill>
</available_skills>
</skills_system>
```

### 3. Agent 使用

当用户问："处理这个 PDF 文件"

Agent 自动：

1. 扫描 `<available_skills>`
2. 找到 `pdf` skill
3. 执行 `openskills read pdf`
4. 加载详细指令并执行

---

## 可用的官方 Skills

来自 Anthropic 的 [skills 仓库](https://github.com/anthropics/skills)：

- **xlsx** - 表格创建、编辑、公式、数据分析
- **docx** - Word 文档创建
- **pdf** - PDF 提取、合并、拆分
- **pptx** - PPT 创建和编辑
- **canvas-design** - 海报和视觉设计
- **mcp-builder** - 构建 MCP 服务器
- **skill-creator** - Skills 创作指南

---

## 应用场景

### 场景 1：多 Agent 共享 Skills

```bash
# 使用 --universal 模式
openskills install anthropics/skills --universal

# 在 .agent/skills/ 安装
# Claude Code 和其他 Agent 共享
```

### 场景 2：私有企业 Skills

```bash
# 安装私有仓库的 Skills
openskills install git@github.com:company/internal-skills.git
```

### 场景 3：本地开发

```bash
# 符号链接本地开发中的 Skill
openskills install ./my-skill --mode=project
```

---

## SKILL.md 格式

```markdown
---
name: pdf
description: PDF 处理工具
---

# PDF Skill 使用说明

当用户要求处理 PDF 时：

1. 安装依赖：`pip install pypdf2`
2. 使用 scripts/extract_text.py 提取文本
3. 对于捆绑资源，使用输出中的基础目录
4. [详细步骤...]
```

**渐进式加载：** 只有调用时才加载完整指令，保持上下文整洁。

---

## 与 qingcloud-mcp 的关系

**相似之处：**

- 都是为 Agent 提供扩展能力
- 都使用声明式的技能描述
- 都支持技能组合

**差异：**

| 维度     | OpenSkills      | qingcloud-mcp |
| -------- | --------------- | ------------- |
| 协议     | CLI + AGENTS.md | MCP 协议      |
| 安装     | npm 全局安装    | Java 应用     |
| 技能来源 | GitHub 仓库     | 内置工具      |
| 适用范围 | 所有编程 Agent  | AI 对话 Agent |
| 格式     | SKILL.md        | Tool Schema   |

---

## 总结

OpenSkills 的价值：

1. **通用性** - 适用所有 AI Agent
2. **生态** - 复用 Anthropic 技能市场
3. **简单** - npm 一键安装
4. **标准化** - 统一的 SKILL.md 格式

对 qingcloud-mcp 的启发：

- 可以考虑支持 SKILL.md 格式
- 实现技能的动态加载机制
- 建立技能共享生态
