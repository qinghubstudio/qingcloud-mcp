# MCP Server 实现指南

## 概述

本文档以 **qingcloud-mcp** 项目为例，说明如何实现一个 MCP Server。

## 项目结构参考

```
qingcloud-mcp/
├─ src/main/java/com/qingcloud/mcp/
│  ├─ server/
│  │  ├─ MCPServer.java          # 主服务器
│  │  ├─ MCPHttpController.java  # HTTP 端点
│  │  └─ ToolRegistry.java       # 工具注册
│  ├─ xhs/tools/                  # 小红书工具
│  │  ├─ SearchToolFactory.java
│  │  ├─ PublishToolFactory.java
│  │  └─ ...
│  └─ suno/tools/                 # 音乐生成工具
│     └─ SunoToolFactory.java
└─ pom.xml
```

---

## 核心组件

### 1. MCP Server 主类

```java
@Component
public class MCPServer {

    private final ToolRegistry toolRegistry;

    public MCPServer(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * 处理 tools/list 请求
     */
    public ListToolsResult listTools() {
        List<Tool> tools = toolRegistry.getAllTools();
        return ListToolsResult.builder()
            .tools(tools)
            .build();
    }

    /**
     * 处理 tools/call 请求
     */
    public CallToolResult callTool(String name, Map<String, Object> arguments) {
        ToolHandler handler = toolRegistry.getHandler(name);

        if (handler == null) {
            throw new ToolNotFoundException(name);
        }

        return handler.execute(arguments);
    }
}
```

### 2. HTTP Controller

```java
@RestController
@RequestMapping("/mcp")
public class MCPHttpController {

    private final MCPServer mcpServer;

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleRequest(
        @RequestBody Map<String, Object> request
    ) {
        String method = (String) request.get("method");
        Object params = request.get("params");
        Object id = request.get("id");

        try {
            Object result = switch (method) {
                case "tools/list" -> mcpServer.listTools();
                case "tools/call" -> {
                    Map<String, Object> p = (Map<String, Object>) params;
                    yield mcpServer.callTool(
                        (String) p.get("name"),
                        (Map<String, Object>) p.get("arguments")
                    );
                }
                default -> throw new UnsupportedOperationException(method);
            };

            return ResponseEntity.ok(Map.of(
                "jsonrpc", "2.0",
                "result", result,
                "id", id
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "jsonrpc", "2.0",
                "error", Map.of(
                    "code", -32603,
                    "message", e.getMessage()
                ),
                "id", id
            ));
        }
    }
}
```

### 3. Tool 工厂

```java
public class SearchToolFactory {

    public static Tool createTool() {
        // 定义工具元数据
        return Tool.builder()
            .name("searchNotes")
            .description("搜索小红书笔记")
            .inputSchema(JsonSchema.builder()
                .type("object")
                .properties(Map.of(
                    "keyword", Map.of("type", "string"),
                    "page", Map.of("type", "integer", "default", 1)
                ))
                .required(List.of("keyword"))
                .build())
            .build();
    }

    public static ToolHandler createHandler(SearchAction searchAction) {
        return (arguments) -> {
            String keyword = (String) arguments.get("keyword");
            Integer page = (Integer) arguments.getOrDefault("page", 1);

            // 执行搜索
            List<Map<String, Object>> results = searchAction.search(keyword, page);

            // 返回结果
            return CallToolResult.builder()
                .content(List.of(TextContent.of(
                    toJson(results)
                )))
                .build();
        };
    }
}
```

---

## 最小实现示例

### Python 版本

```python
from fastapi import FastAPI
from pydantic import BaseModel
from typing import Any, Dict, List

app = FastAPI()

# 工具注册表
TOOLS = {}

def register_tool(name, description, schema, handler):
    """注册工具"""
    TOOLS[name] = {
        "name": name,
        "description": description,
        "inputSchema": schema,
        "handler": handler
    }

# 注册示例工具
def hello_handler(arguments):
    name = arguments.get("name", "World")
    return {"message": f"Hello, {name}!"}

register_tool(
    name="hello",
    description="Say hello",
    schema={
        "type": "object",
        "properties": {
            "name": {"type": "string"}
        }
    },
    handler=hello_handler
)

class MCPRequest(BaseModel):
    jsonrpc: str = "2.0"
    method: str
    params: Dict[str, Any] = {}
    id: Any = None

@app.post("/mcp")
async def handle_mcp(request: MCPRequest):
    if request.method == "tools/list":
        tools = [{
            "name": t["name"],
            "description": t["description"],
            "inputSchema": t["inputSchema"]
        } for t in TOOLS.values()]

        return {
            "jsonrpc": "2.0",
            "result": {"tools": tools},
            "id": request.id
        }

    elif request.method == "tools/call":
        name = request.params.get("name")
        arguments = request.params.get("arguments", {})

        if name not in TOOLS:
            return {
                "jsonrpc": "2.0",
                "error": {"code": -32601, "message": "Tool not found"},
                "id": request.id
            }

        try:
            result = TOOLS[name]["handler"](arguments)
            return {
                "jsonrpc": "2.0",
                "result": {"content": [{"type": "text", "text": str(result)}]},
                "id": request.id
            }
        except Exception as e:
            return {
                "jsonrpc": "2.0",
                "error": {"code": -32603, "message": str(e)},
                "id": request.id
            }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8080)
```

---

## 参考 qingcloud-mcp 实现

### 查看完整代码

```bash
# 查看工具实现
cat src/main/java/com/qingcloud/mcp/xhs/tools/SearchToolFactory.java

# 查看 MCP Server
cat src/main/java/com/qingcloud/mcp/server/MCPServer.java

# 查看 HTTP Controller
cat src/main/java/com/qingcloud/mcp/server/MCPHttpController.java
```

### 运行项目

```bash
# 构建
mvn clean package -DskipTests

# 运行
java -jar target/qingcloud-mcp-0.1.0-SNAPSHOT.jar

# 测试
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/list",
    "id": 1
  }'
```

---

## 关键要点

- ✅ 实现 `tools/list` 和 `tools/call` 方法
- ✅ 使用 JSON-RPC 2.0 格式
- ✅ 提供清晰的工具描述和 Schema
- ✅ 处理异常并返回标准错误
- ✅ 支持 HTTP/SSE 或 STDIO 传输

---

[返回主目录](./04-skills-integration-guide.md) | [下一篇：MCP 集成 →](./04h-mcp-integration.md)
