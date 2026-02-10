# 项目集成 MCP Server

## 概述

本文档说明如何在你的项目中调用 MCP Server（如 qingcloud-mcp）。

## 集成方式

### 方式一：HTTP 客户端直接调用

最简单的方式，直接通过 HTTP 调用：

```python
import requests

def call_mcp_tool(server_url, tool_name, arguments):
    """调用 MCP 工具"""

    response = requests.post(
        server_url,
        json={
            "jsonrpc": "2.0",
            "method": "tools/call",
            "params": {
                "name": tool_name,
                "arguments": arguments
            },
            "id": 1
        }
    )

    result = response.json()

    if "error" in result:
        raise Exception(result["error"]["message"])

    return result["result"]

# 使用
result = call_mcp_tool(
    "http://localhost:8080/mcp",
    "searchNotes",
    {"keyword": "咖啡", "page": 1}
)

print(result)
```

### 方式二：封装客户端类

```python
class MCPClient:
    """MCP 客户端封装"""

    def __init__(self, server_url):
        self.server_url = server_url
        self.request_id = 0

    def list_tools(self):
        """列出所有工具"""
        response = self._request("tools/list", {})
        return response.get("tools", [])

    def call_tool(self, name, arguments=None):
        """调用工具"""
        response = self._request("tools/call", {
            "name": name,
            "arguments": arguments or {}
        })
        return response

    def _request(self, method, params):
        """发送请求"""
        self.request_id += 1

        response = requests.post(
            self.server_url,
            json={
                "jsonrpc": "2.0",
                "method": method,
                "params": params,
                "id": self.request_id
            },
            timeout=30
        )

        result = response.json()

        if "error" in result:
            raise MCPError(result["error"])

        return result["result"]

# 使用
client = MCPClient("http://localhost:8080/mcp")

# 列出工具
tools = client.list_tools()
for tool in tools:
    print(f"- {tool['name']}: {tool['description']}")

# 调用工具
result = client.call_tool("searchNotes", {
    "keyword": "咖啡"
})
```

---

## Java 集成

```java
package com.example.client;

import org.springframework.web.client.RestTemplate;
import java.util.Map;

public class MCPClient {

    private final String serverUrl;
    private final RestTemplate restTemplate;
    private int requestId = 0;

    public MCPClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.restTemplate = new RestTemplate();
    }

    public Map<String, Object> callTool(String name, Map<String, Object> arguments) {
        Map<String, Object> request = Map.of(
            "jsonrpc", "2.0",
            "method", "tools/call",
            "params", Map.of(
                "name", name,
                "arguments", arguments
            ),
            "id", ++requestId
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
            serverUrl,
            request,
            Map.class
        );

        if (response.containsKey("error")) {
            throw new RuntimeException(
                response.get("error").toString()
            );
        }

        return (Map<String, Object>) response.get("result");
    }

    public static void main(String[] args) {
        MCPClient client = new MCPClient("http://localhost:8080/mcp");

        Map<String, Object> result = client.callTool(
            "searchNotes",
            Map.of("keyword", "咖啡")
        );

        System.out.println(result);
    }
}
```

---

## 与 AI 模型结合

### 使用 OpenAI

```python
from openai import OpenAI
import json

client_openai = OpenAI()
client_mcp = MCPClient("http://localhost:8080/mcp")

# 获取 MCP 工具列表
mcp_tools = client_mcp.list_tools()

# 转换为 OpenAI 格式
openai_tools = [{
    "type": "function",
    "function": {
        "name": tool["name"],
        "description": tool["description"],
        "parameters": tool["inputSchema"]
    }
} for tool in mcp_tools]

# 调用 OpenAI
response = client_openai.chat.completions.create(
    model="gpt-4",
    messages=[{
        "role": "user",
        "content": "搜索小红书上关于咖啡的笔记"
    }],
    tools=openai_tools
)

# 处理工具调用
if response.choices[0].message.tool_calls:
    for tool_call in response.choices[0].message.tool_calls:
        function_name = tool_call.function.name
        arguments = json.loads(tool_call.function.arguments)

        # 调用 MCP 工具
        result = client_mcp.call_tool(function_name, arguments)
        print(f"MCP Result: {result}")
```

---

## Spring Boot 集成

```java
// MCPConfiguration.java
@Configuration
public class MCPConfiguration {

    @Bean
    public MCPClient mcpClient(
        @Value("${mcp.server.url}") String serverUrl
    ) {
        return new MCPClient(serverUrl);
    }
}

// MCPService.java
@Service
public class MCPService {

    @Autowired
    private MCPClient mcpClient;

    public List<Note> searchNotes(String keyword) {
        Map<String, Object> result = mcpClient.callTool(
            "searchNotes",
            Map.of("keyword", keyword)
        );

        // 解析结果
        return parseNotes(result);
    }
}

// Controller.java
@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private MCPService mcpService;

    @GetMapping("/notes/search")
    public ResponseEntity<List<Note>> search(
        @RequestParam String keyword
    ) {
        List<Note> notes = mcpService.searchNotes(keyword);
        return ResponseEntity.ok(notes);
    }
}
```

---

## 异步调用

```python
import asyncio
import aiohttp

class AsyncMCPClient:
    """异步 MCP 客户端"""

    def __init__(self, server_url):
        self.server_url = server_url

    async def call_tool(self, name, arguments):
        async with aiohttp.ClientSession() as session:
            async with session.post(
                self.server_url,
                json={
                    "jsonrpc": "2.0",
                    "method": "tools/call",
                    "params": {
                        "name": name,
                        "arguments": arguments
                    },
                    "id": 1
                }
            ) as response:
                result = await response.json()
                return result["result"]

# 批量调用
async def batch_search(keywords):
    client = AsyncMCPClient("http://localhost:8080/mcp")

    tasks = [
        client.call_tool("searchNotes", {"keyword": kw})
        for kw in keywords
    ]

    results = await asyncio.gather(*tasks)
    return results

# 使用
results = asyncio.run(batch_search(["咖啡", "美食", "旅行"]))
```

---

## 错误处理

```python
class MCPError(Exception):
    """MCP 错误"""

    def __init__(self, error_info):
        self.code = error_info.get("code")
        self.message = error_info.get("message")
        super().__init__(self.message)

def safe_call_tool(client, name, arguments):
    """安全调用工具"""

    try:
        return client.call_tool(name, arguments)

    except requests.Timeout:
        print("❌ Request timeout")
        return None

    except requests.ConnectionError:
        print("❌ Connection error, is MCP server running?")
        return None

    except MCPError as e:
        print(f"❌ MCP Error [{e.code}]: {e.message}")
        return None

    except Exception as e:
        print(f"❌ Unexpected error: {e}")
        return None
```

---

## 部署建议

### Docker 部署

```yaml
# docker-compose.yml
version: "3.8"

services:
  mcp-server:
    build: ./qingcloud-mcp
    ports:
      - "8080:8080"
    environment:
      - MCP_TRANSPORT_MODE=http
      - SERVER_PORT=8080
    volumes:
      - ./logs:/var/log/mcp

  your-app:
    build: ./your-app
    depends_on:
      - mcp-server
    environment:
      - MCP_SERVER_URL=http://mcp-server:8080/mcp
```

### Kubernetes 部署

```yaml
# mcp-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mcp-server
spec:
  replicas: 3
  selector:
    matchLabels:
      app: mcp-server
  template:
    metadata:
      labels:
        app: mcp-server
    spec:
      containers:
        - name: mcp
          image: qingcloud-mcp:latest
          ports:
            - containerPort: 8080
---
apiVersion: v1
kind: Service
metadata:
  name: mcp-service
spec:
  selector:
    app: mcp-server
  ports:
    - port: 8080
      targetPort: 8080
```

---

## 最佳实践

- ✅ 使用连接池复用连接
- ✅ 设置合理的超时时间
- ✅ 实现重试机制
- ✅ 记录详细日志
- ✅ 监控 MCP Server 健康状态

---

[返回主目录](./04-skills-integration-guide.md)
