# 本地部署与优化

## 部署架构

### 单机部署

```
┌─────────────────────────────────┐
│         服务器                    │
│                                 │
│  ┌──────────┐  ┌─────────────┐ │
│  │  Ollama  │  │ MCP Server  │ │
│  │  :11434  │  │   :8080     │ │
│  └──────────┘  └─────────────┘ │
│       ↑              ↑          │
│       └──────┬───────┘          │
│           ┌──┴───┐              │
│           │ Agent│              │
│           │ :8000│              │
│           └──────┘              │
└─────────────────────────────────┘
```

### 分布式部署

```
┌──────────┐     ┌──────────┐     ┌──────────┐
│  Ollama  │     │   MCP    │     │  Agent   │
│  Server  │◄────┤  Server  │◄────┤  API     │
│ (GPU机器)│     │ (轻量级)  │     │ (应用层)  │
└──────────┘     └──────────┘     └──────────┘
```

---

## 性能优化

### Ollama 优化

```bash
# 1. 设置并发数
export OLLAMA_NUM_PARALLEL=4

# 2. 设置上下文长度
export OLLAMA_CTX_SIZE=4096

# 3. GPU 内存限制
export OLLAMA_GPU_MEMORY=16384  # MB

# 4. 启用 Flash Attention
export OLLAMA_FLASH_ATTENTION=1
```

### MCP Server 优化

```yaml
# application.yml
server:
  tomcat:
    threads:
      max: 200
      min-spare: 10
    max-connections: 10000

spring:
  task:
    execution:
      pool:
        core-size: 10
        max-size: 20
```

---

## 缓存策略

### 响应缓存

```python
from functools import lru_cache
import hashlib

class CachedAgent:

    def __init__(self, agent):
        self.agent = agent
        self.cache = {}

    def chat(self, message: str) -> str:
        # 生成缓存键
        cache_key = hashlib.md5(message.encode()).hexdigest()

        # 检查缓存
        if cache_key in self.cache:
            print("✅ 使用缓存")
            return self.cache[cache_key]

        # 调用原始方法
        result = self.agent.chat(message)

        # 保存缓存
        self.cache[cache_key] = result

        return result
```

### 工具列表缓存

```python
@lru_cache(maxsize=1)
def get_tools():
    """缓存工具列表（1小时有效）"""
    return mcp_client.list_tools()
```

---

## 监控

### Prometheus 指标

```python
from prometheus_client import Counter, Histogram, start_http_server

# 定义指标
chat_requests = Counter('chat_requests_total', 'Total chat requests')
chat_duration = Histogram('chat_duration_seconds', 'Chat duration')
tool_calls = Counter('tool_calls_total', 'Total tool calls', ['tool_name'])

class MonitoredAgent:

    def chat(self, message):
        chat_requests.inc()

        with chat_duration.time():
            result = self.agent.chat(message)

        return result

# 启动指标服务器
start_http_server(9090)
```

### 健康检查

```python
@app.get("/health")
async def health_check():
    """健康检查"""

    checks = {
        "ollama": check_ollama(),
        "mcp": check_mcp(),
        "agent": "ok"
    }

    all_ok = all(v == "ok" for v in checks.values())

    return {
        "status": "healthy" if all_ok else "unhealthy",
        "checks": checks
    }

def check_ollama():
    try:
        ollama.list()
        return "ok"
    except:
        return "error"

def check_mcp():
    try:
        mcp_client.list_tools()
        return "ok"
    except:
        return "error"
```

---

## 最佳实践

### 1. 资源管理

```python
# 限制并发请求
from asyncio import Semaphore

semaphore = Semaphore(10)  # 最多10个并发

async def rate_limited_chat(message):
    async with semaphore:
        return agent.chat(message)
```

### 2. 错误重试

```python
from tenacity import retry, stop_after_attempt, wait_exponential

@retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(min=1, max=10)
)
def resilient_chat(message):
    return agent.chat(message)
```

### 3. 超时控制

```python
import asyncio

async def chat_with_timeout(message, timeout=30):
    try:
        return await asyncio.wait_for(
            agent.chat(message),
            timeout=timeout
        )
    except asyncio.TimeoutError:
        return "请求超时，请稍后重试"
```

---

## 成本分析

### 硬件成本

| 配置                | 成本    | 性能 |
| ------------------- | ------- | ---- |
| RTX 4090 (24GB)     | ¥12,000 | 最佳 |
| RTX 3090 (24GB)     | ¥8,000  | 优秀 |
| RTX 4070 Ti (12GB)  | ¥5,000  | 良好 |
| CPU only (32GB RAM) | ¥2,000  | 可用 |

### 运营成本对比

**Claude API:**

- 每百万 token: $3-$15
- 月均成本: $100-$1000 (根据使用量)

**本地部署:**

- 电费: $50/月 (GPU 满载)
- 一次性硬件: $1000-$10000
- **6-12 个月回本**

---

## 扩展建议

### 模型微调

```bash
# 使用自己的数据微调模型
# 提升特定领域的能力

# Ollama 支持导入 GGUF 格式模型
ollama create my-custom-model -f Modelfile
```

### 多模型支持

```python
class MultiModelAgent:
    """支持多个模型"""

    def __init__(self):
        self.models = {
            "fast": "qwen2.5:7b",     # 快速响应
            "accurate": "qwen2.5:14b",  # 高准确度
            "english": "llama3.1:8b"   # 英文优先
        }

    def chat(self, message, model_type="fast"):
        model = self.models.get(model_type, "fast")
        # ...
```

---

## 检查清单

部署前确认：

- [ ] Ollama 已安装并运行
- [ ] 模型已下载（qwen2.5:7b）
- [ ] MCP Server 已启动
- [ ] GPU 驱动已安装（如有 GPU）
- [ ] 防火墙规则已配置
- [ ] 健康检查通过
- [ ] 监控已启用

---

## 故障排查

### Ollama 无法启动

```bash
# 检查端口
lsof -i :11434

# 查看日志
journalctl -u ollama -f

# 重启服务
systemctl restart ollama
```

### GPU 未被使用

```bash
# 检查 CUDA
nvidia-smi

# 检查 Ollama GPU
ollama ps
```

### 性能慢

```bash
# 检查资源
htop
nvidia-smi

# 减小模型或上下文
ollama run qwen2.5:7b --ctx-size 2048
```

---

[返回本地方案目录](./05-local-skills-solution.md)
