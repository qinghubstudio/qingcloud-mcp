# 本地大模型安装配置

## 选择本地大模型

### 推荐方案：Ollama

**优势：**

- ✅ 安装简单
- ✅ 自动管理模型
- ✅ 提供 API 接口
- ✅ 支持多种模型

**官网：** https://ollama.ai

---

## 安装 Ollama

### Windows

```powershell
# 下载安装包
# https://ollama.ai/download/windows

# 或使用 winget
winget install Ollama.Ollama
```

### Linux

```bash
curl -fsSL https://ollama.ai/install.sh | sh
```

### macOS

```bash
# 下载 .dmg 文件
# https://ollama.ai/download/mac

# 或使用 brew
brew install ollama
```

---

## 启动 Ollama

```bash
# 启动服务（后台运行）
ollama serve

# 默认监听 http://localhost:11434
```

---

## 下载模型

### 推荐模型

```bash
# Qwen2.5 (7B) - 推荐，中文好
ollama pull qwen2.5:7b

# Llama 3.1 (8B) - 英文好
ollama pull llama3.1:8b

# Mistral (7B) - 平衡
ollama pull mistral:7b

# 查看已安装模型
ollama list
```

### 模型对比

| 模型        | 大小  | 中文       | 英文       | Function Calling | VRAM |
| ----------- | ----- | ---------- | ---------- | ---------------- | ---- |
| qwen2.5:7b  | 4.7GB | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐   | ✅ 优秀          | 8GB  |
| llama3.1:8b | 4.9GB | ⭐⭐⭐     | ⭐⭐⭐⭐⭐ | ✅ 良好          | 8GB  |
| mistral:7b  | 4.1GB | ⭐⭐       | ⭐⭐⭐⭐⭐ | ✅ 良好          | 8GB  |

---

## 测试模型

### 命令行测试

```bash
# 交互式聊天
ollama run qwen2.5:7b

> 你好，介绍一下自己
> （模型回复）

# Ctrl+D 退出
```

### API 测试

```bash
# 测试 API
curl http://localhost:11434/api/generate -d '{
  "model": "qwen2.5:7b",
  "prompt": "你好，介绍一下自己",
  "stream": false
}'
```

### Python 测试

```python
import ollama

response = ollama.chat(
    model='qwen2.5:7b',
    messages=[{
        'role': 'user',
        'content': '你好，介绍一下自己'
    }]
)

print(response['message']['content'])
```

---

## 安装 Python SDK

```bash
pip install ollama
```

---

## 硬件要求

### 最低配置

- CPU: 4 核心
- 内存: 16GB
- 推荐: 集成显卡或 CPU 运行（很慢）

### 推荐配置

- CPU: 8 核心+
- 内存: 32GB
- GPU: NVIDIA (8GB+ VRAM)
- 存储: SSD

### 最佳配置

- GPU: NVIDIA RTX 3090/4090 (24GB VRAM)
- 内存: 64GB
- 存储: NVMe SSD

---

## GPU 加速配置

### NVIDIA GPU

```bash
# Ollama 自动检测 CUDA
# 确保安装了 NVIDIA 驱动

# 检查 GPU 使用
nvidia-smi
```

### Apple Silicon

```bash
# Ollama 自动使用 Metal
# M1/M2/M3 芯片原生支持
```

---

## 常见问题

**Q: 模型下载慢怎么办？**

```bash
# 使用镜像加速（如果有）
export OLLAMA_MODELS=/path/to/models
```

**Q: 如何删除模型？**

```bash
ollama rm qwen2.5:7b
```

**Q: 如何更新模型？**

```bash
ollama pull qwen2.5:7b
```

---

## 下一步

安装完成后，继续学习如何实现 Function Calling：

→ [Function Calling 实现](./05b-local-function-calling.md)

---

[返回本地方案目录](./05-local-skills-solution.md)
