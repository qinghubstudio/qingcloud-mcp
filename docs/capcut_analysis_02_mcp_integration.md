# CapCut/VectCut 项目架构分析 - Part 2: MCP 集成方式

## MCP 协议集成

两个项目都实现了 MCP (Model Context Protocol) 服务器，允许 AI Agent 通过标准协议调用视频编辑功能。

## MCP 工具定义

### 工具列表结构

```python
TOOLS = [
    {
        "name": "create_draft",
        "description": "创建新的CapCut草稿",
        "inputSchema": {
            "type": "object",
            "properties": {
                "width": {"type": "integer", "default": 1080},
                "height": {"type": "integer", "default": 1920}
            }
        }
    },
    {
        "name": "add_video",
        "description": "添加视频到草稿，支持转场、蒙版、背景模糊等效果",
        "inputSchema": {
            "type": "object",
            "properties": {
                "video_url": {"type": "string", "description": "视频URL"},
                "draft_id": {"type": "string"},
                "start": {"type": "number", "default": 0},
                "end": {"type": "number"},
                "speed": {"type": "number", "default": 1.0},
                "transition": {"type": "string"},
                "mask_type": {"type": "string"},
                # ... 更多参数
            },
            "required": ["video_url"]
        }
    },
    # ... 其他工具
]
```

### 完整工具集

VectCutAPI 提供的 MCP 工具：

1. **create_draft** - 创建草稿
2. **add_video** - 添加视频
3. **add_audio** - 添加音频
4. **add_image** - 添加图片
5. **add_text** - 添加文本
6. **add_subtitle** - 添加字幕（SRT）
7. **add_effect** - 添加特效
8. **add_sticker** - 添加贴纸
9. **add_video_keyframe** - 添加关键帧
10. **get_video_duration** - 获取视频时长
11. **save_draft** - 保存草稿

## MCP 服务器实现

### STDIO 传输模式

```python
# mcp_server.py
def handle_request(request_data: str) -> Optional[str]:
    """处理JSON-RPC请求"""
    request = json.loads(request_data.strip())

    if request.get("method") == "initialize":
        return json.dumps({
            "jsonrpc": "2.0",
            "id": request.get("id"),
            "result": {
                "protocolVersion": "2024-11-05",
                "capabilities": {
                    "tools": {"listChanged": False}
                },
                "serverInfo": {
                    "name": "capcut-api",
                    "version": "1.12.3"
                }
            }
        })

    elif request.get("method") == "tools/list":
        return json.dumps({
            "jsonrpc": "2.0",
            "id": request.get("id"),
            "result": {"tools": TOOLS}
        })

    elif request.get("method") == "tools/call":
        tool_name = request["params"]["name"]
        arguments = request["params"].get("arguments", {})
        result = execute_tool(tool_name, arguments)

        return json.dumps({
            "jsonrpc": "2.0",
            "id": request.get("id"),
            "result": {
                "content": [{
                    "type": "text",
                    "text": json.dumps(result, ensure_ascii=False)
                }]
            }
        })

def main():
    """主循环"""
    while True:
        line = sys.stdin.readline()
        if not line:
            break

        response = handle_request(line)
        if response:
            print(response)
            sys.stdout.flush()
```

### 工具执行函数

```python
def execute_tool(tool_name: str, arguments: Dict[str, Any]) -> Dict[str, Any]:
    """执行具体的工具"""
    try:
        if tool_name == "create_draft":
            draft_id, script = get_or_create_draft(
                width=arguments.get("width", 1080),
                height=arguments.get("height", 1920)
            )
            return {
                "success": True,
                "result": {
                    "draft_id": str(draft_id),
                    "draft_url": f"https://example.com/draft/{draft_id}"
                }
            }

        elif tool_name == "add_video":
            result = add_video_track(**arguments)
            return {"success": True, "result": result}

        # ... 其他工具

    except Exception as e:
        return {"success": False, "error": str(e)}
```

## HTTP API 服务（Flask）

除了 MCP 协议，项目还提供 REST API：

```python
# capcut_server.py
from flask import Flask, request, jsonify

app = Flask(__name__)

@app.route('/add_video', methods=['POST'])
def add_video():
    data = request.get_json()

    try:
        draft_result = add_video_track(
            draft_folder=data.get('draft_folder'),
            video_url=data['video_url'],
            start=data.get('start', 0),
            end=data.get('end'),
            # ... 更多参数
        )

        return jsonify({
            "success": True,
            "output": draft_result
        })

    except Exception as e:
        return jsonify({
            "success": False,
            "error": str(e)
        })

@app.route('/save_draft', methods=['POST'])
def save_draft():
    data = request.get_json()
    draft_result = save_draft_impl(
        data['draft_id'],
        data.get('draft_folder')
    )
    return jsonify({
        "success": True,
        "output": draft_result
    })
```

## 标准输出捕获

为防止调试信息干扰 JSON 响应：

```python
import contextlib
import io

@contextlib.contextmanager
def capture_stdout():
    """捕获标准输出"""
    old_stdout = sys.stdout
    sys.stdout = io.StringIO()
    try:
        yield sys.stdout
    finally:
        sys.stdout = old_stdout

# 使用
with capture_stdout() as captured:
    result = add_video_track(...)
```

## 关键设计要点

### 1. 双协议支持

- **MCP (STDIO)**：用于 AI Agent 集成
- **HTTP REST**：用于传统 Web 应用

### 2. 统一的业务逻辑

两种协议共享相同的业务逻辑函数：

```
MCP Server  ──┐
              ├──> add_video_track()
HTTP API   ──┘
```

### 3. 错误处理

统一的错误响应格式：

```python
{
    "success": False,
    "error": "错误描述",
    "traceback": "详细堆栈（可选）"
}
```

### 4. 参数验证

在工具层进行参数验证：

```python
if not video_url:
    return {
        "success": False,
        "error": "Required parameter 'video_url' is missing"
    }
```

## 下一步

继续阅读：

- Part 3: 核心功能实现细节
- Part 4: Java 移植方案
