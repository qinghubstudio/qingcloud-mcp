# LLM-MCP 完整集成

## 架构设计

```
用户请求 → 本地 LLM (Ollama) → 工具调用决策
                ↓
         MCP Client → MCP Server (qingcloud-mcp)
                ↓
           执行工具 → 返回结果
                ↓
         本地 LLM → 生成最终回答
```

---

## 完整实现（Python）

### 核心类

```python
# local_skills_agent.py

import ollama
import requests
from typing import List, Dict, Any

class MCPClient:
    """MCP 客户端"""

    def __init__(self, server_url: str):
        self.server_url = server_url

    def list_tools(self) -> List[Dict]:
        response = requests.post(
            self.server_url,
            json={
                "jsonrpc": "2.0",
                "method": "tools/list",
                "id": 1
            }
        )
        return response.json()["result"]["tools"]

    def call_tool(self, name: str, arguments: Dict) -> Any:
        response = requests.post(
            self.server_url,
            json={
                "jsonrpc": "2.0",
                "method": "tools/call",
                "params": {
                    "name": name,
                    "arguments": arguments
                },
                "id": 2
            }
        )
        result = response.json()
        if "error" in result:
            raise Exception(result["error"]["message"])
        return result["result"]


class LocalSkillsAgent:
    """本地 Skills Agent"""

    def __init__(
        self,
        mcp_server_url: str = "http://localhost:8080/mcp",
        llm_model: str = "qwen2.5:7b"
    ):
        self.mcp_client = MCPClient(mcp_server_url)
        self.llm_model = llm_model
        self.tools = self._load_tools()
        self.messages = []

    def _load_tools(self) -> List[Dict]:
        """加载并转换 MCP 工具"""
        mcp_tools = self.mcp_client.list_tools()

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

    def chat(self, user_message: str) -> str:
        """发送消息并获取回答"""

        # 添加用户消息
        self.messages.append({
            "role": "user",
            "content": user_message
        })

        # 调用 LLM
        response = ollama.chat(
            model=self.llm_model,
            messages=self.messages,
            tools=self.tools
        )

        assistant_msg = response["message"]
        self.messages.append(assistant_msg)

        # 处理工具调用
        if assistant_msg.get("tool_calls"):
            return self._handle_tool_calls(assistant_msg["tool_calls"])

        return assistant_msg["content"]

    def _handle_tool_calls(self, tool_calls: List[Dict]) -> str:
        """处理工具调用"""

        for tool_call in tool_calls:
            func_name = tool_call["function"]["name"]
            func_args = tool_call["function"]["arguments"]

            print(f"🔧 调用工具: {func_name}")
            print(f"📝 参数: {func_args}")

            # 调用 MCP 工具
            result = self.mcp_client.call_tool(func_name, func_args)

            # 添加工具结果到消息历史
            self.messages.append({
                "role": "tool",
                "content": str(result)
            })

        # 让 LLM 根据工具结果生成最终回答
        final_response = ollama.chat(
            model=self.llm_model,
            messages=self.messages
        )

        return final_response["message"]["content"]

    def reset(self):
        """重置对话"""
        self.messages = []


# 使用示例
if __name__ == "__main__":
    agent = LocalSkillsAgent()

    # 单轮对话
    answer = agent.chat("搜索小红书上关于咖啡的笔记")
    print(f"🤖 {answer}")

    # 多轮对话
    agent.reset()
    print(agent.chat("搜索咖啡笔记"))
    print(agent.chat("看第一条详情"))
```

---

## Java 实现

```java
// LocalSkillsAgent.java
package com.example.local;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.*;
import java.util.*;

public class LocalSkillsAgent {

    private final String mcpServerUrl;
    private final String llmModel;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LocalSkillsAgent(
        String mcpServerUrl,
        String llmModel
    ) {
        this.mcpServerUrl = mcpServerUrl;
        this.llmModel = llmModel;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String chat(String userMessage) throws Exception {
        // 1. 获取可用工具
        List<Map<String, Object>> tools = listMCPTools();

        // 2. 调用 Ollama
        String ollamaUrl = "http://localhost:11434/api/chat";

        Map<String, Object> request = Map.of(
            "model", llmModel,
            "messages", List.of(
                Map.of("role", "user", "content", userMessage)
            ),
            "tools", tools,
            "stream", false
        );

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(ollamaUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                objectMapper.writeValueAsString(request)
            ))
            .build();

        HttpResponse<String> response = httpClient.send(
            httpRequest,
            HttpResponse.BodyHandlers.ofString()
        );

        // 3. 处理响应
        Map<String, Object> result = objectMapper.readValue(
            response.body(),
            Map.class
        );

        Map<String, Object> message =
            (Map<String, Object>) result.get("message");

        // 4. 处理工具调用
        if (message.containsKey("tool_calls")) {
            return handleToolCalls(
                (List<Map<String, Object>>) message.get("tool_calls")
            );
        }

        return (String) message.get("content");
    }

    private List<Map<String, Object>> listMCPTools() throws Exception {
        // 调用 MCP 的 tools/list
        // ... 实现省略
        return new ArrayList<>();
    }

    private String handleToolCalls(List<Map<String, Object>> toolCalls) {
        // 执行工具并返回结果
        // ... 实现省略
        return "";
    }
}
```

---

## 命令行工具

```python
#!/usr/bin/env python3
# local_skills_cli.py

from local_skills_agent import LocalSkillsAgent
import sys

def main():
    agent = LocalSkillsAgent()

    print("🚀 本地 Skills Agent 已启动")
    print("📦 LLM: qwen2.5:7b (本地)")
    print("🔧 MCP: http://localhost:8080/mcp")
    print("输入 'quit' 退出\n")

    while True:
        try:
            user_input = input("👤 你: ").strip()

            if not user_input:
                continue

            if user_input.lower() in ['quit', 'exit', 'q']:
                print("👋 再见!")
                break

            if user_input.lower() == 'reset':
                agent.reset()
                print("🔄 对话已重置")
                continue

            answer = agent.chat(user_input)
            print(f"🤖 Agent: {answer}\n")

        except KeyboardInterrupt:
            print("\n👋 再见!")
            break
        except Exception as e:
            print(f"❌ 错误: {e}\n")

if __name__ == "__main__":
    main()
```

使用：

```bash
chmod +x local_skills_cli.py
./local_skills_cli.py
```

---

## Web API 服务

```python
# app.py
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from local_skills_agent import LocalSkillsAgent

app = FastAPI(title="Local Skills API")

# 单例 Agent
agent = LocalSkillsAgent()

class ChatRequest(BaseModel):
    message: str
    reset: bool = False

class ChatResponse(BaseModel):
    reply: str

@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """聊天接口"""
    try:
        if request.reset:
            agent.reset()

        reply = agent.chat(request.message)
        return ChatResponse(reply=reply)

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/tools")
async def list_tools():
    """列出可用工具"""
    return {"tools": agent.tools}

# 运行: uvicorn app:app --reload
```

---

## Docker Compose 部署

```yaml
# docker-compose.yml
version: "3.8"

services:
  # MCP Server
  mcp-server:
    build: ./qingcloud-mcp
    ports:
      - "8080:8080"
    environment:
      - MCP_TRANSPORT_MODE=http

  # Ollama
  ollama:
    image: ollama/ollama:latest
    ports:
      - "11434:11434"
    volumes:
      - ollama-data:/root/.ollama
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]

  # Local Skills Agent API
  agent-api:
    build: ./local-skills-agent
    ports:
      - "8000:8000"
    depends_on:
      - mcp-server
      - ollama
    environment:
      - MCP_SERVER_URL=http://mcp-server:8080/mcp
      - OLLAMA_URL=http://ollama:11434

volumes:
  ollama-data:
```

---

## 下一步

→ [本地部署优化](./05d-local-deployment.md)

---

[返回本地方案目录](./05-local-skills-solution.md)
