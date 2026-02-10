# Function Calling 实现

## 概述

本地大模型需要实现 Function Calling 能力，才能调用 MCP 工具。

## 工具格式标准

### OpenAI 格式（推荐）

```json
{
  "type": "function",
  "function": {
    "name": "searchNotes",
    "description": "搜索小红书笔记",
    "parameters": {
      "type": "object",
      "properties": {
        "keyword": {
          "type": "string",
          "description": "搜索关键词"
        }
      },
      "required": ["keyword"]
    }
  }
}
```

---

## 从 MCP 转换工具格式

```python
def convert_mcp_to_openai_tools(mcp_tools):
    """将 MCP 工具转换为 OpenAI 格式"""

    openai_tools = []

    for tool in mcp_tools:
        openai_tools.append({
            "type": "function",
            "function": {
                "name": tool["name"],
                "description": tool["description"],
                "parameters": tool["inputSchema"]
            }
        })

    return openai_tools

# 使用
from mcp_client import MCPClient

mcp = MCPClient("http://localhost:8080/mcp")
mcp_tools = mcp.list_tools()
tools = convert_mcp_to_openai_tools(mcp_tools)
```

---

## Ollama Function Calling

### 基础调用

```python
import ollama

response = ollama.chat(
    model='qwen2.5:7b',
    messages=[{
        'role': 'user',
        'content': '搜索关于咖啡的笔记'
    }],
    tools=[{
        'type': 'function',
        'function': {
            'name': 'searchNotes',
            'description': '搜索小红书笔记',
            'parameters': {
                'type': 'object',
                'properties': {
                    'keyword': {'type': 'string'}
                },
                'required': ['keyword']
            }
        }
    }]
)

# 检查是否调用工具
if response.get('message', {}).get('tool_calls'):
    tool_call = response['message']['tool_calls'][0]
    print(f"调用工具: {tool_call['function']['name']}")
    print(f"参数: {tool_call['function']['arguments']}")
```

---

## 完整工作流

```python
def chat_with_tools(user_message, mcp_client, model='qwen2.5:7b'):
    """带工具调用的完整对话"""

    # 1. 获取 MCP 工具
    mcp_tools = mcp_client.list_tools()
    tools = convert_mcp_to_openai_tools(mcp_tools)

    # 2. 调用 LLM
    response = ollama.chat(
        model=model,
        messages=[{
            'role': 'user',
            'content': user_message
        }],
        tools=tools
    )

    message = response['message']

    # 3. 处理工具调用
    if message.get('tool_calls'):
        results = []

        for tool_call in message['tool_calls']:
            func_name = tool_call['function']['name']
            func_args = tool_call['function']['arguments']

            # 调用 MCP 工具
            result = mcp_client.call_tool(func_name, func_args)
            results.append(result)

        # 4. 将结果返回给 LLM
        messages = [
            {'role': 'user', 'content': user_message},
            message,
            {'role': 'tool', 'content': str(results)}
        ]

        final_response = ollama.chat(
            model=model,
            messages=messages
        )

        return final_response['message']['content']

    else:
        # 直接回答，无需工具
        return message['content']

# 使用
from mcp_client import MCPClient

mcp = MCPClient("http://localhost:8080/mcp")
answer = chat_with_tools("搜索咖啡相关笔记", mcp)
print(answer)
```

---

## Prompt 工程优化

### 系统提示词

```python
SYSTEM_PROMPT = """
你是一个智能助手，可以使用以下工具：

{tools_description}

当用户请求时，分析需求并调用合适的工具。
工具调用格式必须严格遵循 JSON Schema。
"""

def build_system_prompt(tools):
    """构建系统提示词"""

    tools_desc = []
    for tool in tools:
        desc = f"- {tool['name']}: {tool['description']}"
        tools_desc.append(desc)

    return SYSTEM_PROMPT.format(
        tools_description='\n'.join(tools_desc)
    )
```

### 带系统提示的调用

```python
response = ollama.chat(
    model='qwen2.5:7b',
    messages=[
        {
            'role': 'system',
            'content': build_system_prompt(tools)
        },
        {
            'role': 'user',
            'content': user_message
        }
    ],
    tools=tools
)
```

---

## 多轮对话

```python
class ChatSession:
    """支持工具的对话会话"""

    def __init__(self, mcp_client, model='qwen2.5:7b'):
        self.mcp_client = mcp_client
        self.model = model
        self.messages = []
        self.tools = convert_mcp_to_openai_tools(
            mcp_client.list_tools()
        )

    def chat(self, user_message):
        """发送消息"""

        # 添加用户消息
        self.messages.append({
            'role': 'user',
            'content': user_message
        })

        # 调用 LLM
        response = ollama.chat(
            model=self.model,
            messages=self.messages,
            tools=self.tools
        )

        assistant_message = response['message']
        self.messages.append(assistant_message)

        # 处理工具调用
        if assistant_message.get('tool_calls'):
            for tool_call in assistant_message['tool_calls']:
                result = self.mcp_client.call_tool(
                    tool_call['function']['name'],
                    tool_call['function']['arguments']
                )

                # 添加工具结果
                self.messages.append({
                    'role': 'tool',
                    'content': str(result)
                })

            # 再次调用 LLM 生成最终回答
            final_response = ollama.chat(
                model=self.model,
                messages=self.messages
            )

            return final_response['message']['content']

        return assistant_message['content']

# 使用
session = ChatSession(mcp_client)
print(session.chat("搜索咖啡笔记"))
print(session.chat("看第一条的详情"))  # 上下文记忆
```

---

## 下一步

→ [LLM-MCP 完整集成](./05c-llm-mcp-integration.md)

---

[返回本地方案目录](./05-local-skills-solution.md)
