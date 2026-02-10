# LangChain DeepAgents 概述

## 什么是 DeepAgents？

DeepAgents 是 LangChain 推出的**高级 Agent 框架**，专门处理长时间、复杂的任务。

## 核心能力

### 1. 任务规划 (Planning)

- 使用 `write_todos` 工具分解任务
- 自动生成执行计划

### 2. 文件系统访问

- `ls` - 列出文件
- `read_file` - 读取文件
- `write_file` - 写入文件
- `edit_file` - 编辑文件

### 3. 子 Agent 委派

- 使用 `task` 工具
- 隔离执行上下文
- 专门化处理

### 4. 跨会话记忆

- 持久化状态
- 长期记忆管理

---

## 与本项目的关系

**相似之处：**

- ✅ 都使用工具/技能扩展能力
- ✅ 都支持复杂任务编排
- ✅ 都可以组合多个工具

**差异：**

| 特性     | DeepAgents          | qingcloud-mcp        |
| -------- | ------------------- | -------------------- |
| 框架     | LangChain/LangGraph | MCP 协议             |
| 工具     | 内置 + 自定义       | 小红书/Suno/AutoClip |
| 规划     | 自动规划工具        | LLM 智能决策         |
| 文件系统 | 内置支持            | 可扩展               |

---

## 快速示例

```python
from deepagents import create_deep_agent

agent = create_deep_agent(
    tools=[internet_search],
    system_prompt="Conduct research and write a report."
)

result = agent.invoke({
    "messages": [{
        "role": "user",
        "content": "What is LangGraph?"
    }]
})
```

---

## 借鉴价值

对于 qingcloud-mcp 项目，可以借鉴：

1. **规划工具** - 添加 `write_todos` 类似功能
2. **文件系统** - 增强文件处理能力
3. **子 Agent** - 实现 Agent 委派机制
4. **中间件架构** - 模块化工具注入

---

[详细对比 →](./06b-deepagents-comparison.md)
