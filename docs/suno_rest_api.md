# Suno RESTful API 文档

## ✅ 实现完成

**Maven 编译**: ✅ 成功  
**端点数量**: 5 个 (7 个功能)  
**OpenAI 兼容**: ✅ 支持

## 📡 API 端点列表

### 1. 生成音乐 (简单模式)

**端点**: `POST /api/generate`

**请求体**:

```json
{
  "prompt": "upbeat jazz about coding",
  "make_instrumental": false,
  "model": "chirp-v3.5",
  "wait_audio": false
}
```

**curl 示例**:

```bash
curl -X POST http://localhost:8080/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "happy song about sunshine",
    "wait_audio": true
  }'
```

**响应**:

```json
[
  {
    "id": "uuid-here",
    "title": "Generated Title",
    "audio_url": "https://...",
    "video_url": "https://...",
    "status": "streaming",
    "created_at": "2024-01-01T00:00:00Z",
    ...
  }
]
```

---

### 2. 自定义生成音乐

**端点**: `POST /api/custom_generate`

**请求体**:

```json
{
  "prompt": "Verse 1: ...\nChorus: ...",
  "tags": "pop, electronic, upbeat",
  "title": "My Custom Song",
  "make_instrumental": false,
  "negative_tags": "rock, metal",
  "model": "chirp-v3.5",
  "wait_audio": false
}
```

**curl 示例**:

```bash
curl -X POST http://localhost:8080/api/custom_generate \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Verse: Coding all day long",
    "tags": "electronic, energetic",
    "title": "Developer Anthem",
    "wait_audio": true
  }'
```

---

### 3. 获取音乐列表

**端点**: `GET /api/get`

**参数**:

- `page` (可选): 页码,默认 1

**curl 示例**:

```bash
# 获取第一页
curl http://localhost:8080/api/get

# 获取第二页
curl http://localhost:8080/api/get?page=2
```

---

### 4. 获取音乐详情

**端点**: `GET /api/get?ids={ids}`

**参数**:

- `ids` (必需): 逗号分隔的音乐 ID

**curl 示例**:

```bash
# 单个 ID
curl http://localhost:8080/api/get?ids=abc123

# 多个 ID
curl http://localhost:8080/api/get?ids=abc123,def456,ghi789
```

---

### 5. 获取配额信息

**端点**: `GET /api/get_limit`

**curl 示例**:

```bash
curl http://localhost:8080/api/get_limit
```

**响应**:

```json
{
  "credits_left": 50,
  "period": "monthly",
  "monthly_limit": 500,
  "monthly_usage": 450
}
```

---

### 6. OpenAI 兼容端点

**端点**: `POST /v1/chat/completions`

**请求体**:

```json
{
  "model": "suno-v3.5",
  "messages": [
    {
      "role": "user",
      "content": "Create a happy song about sunshine"
    }
  ],
  "stream": false
}
```

**curl 示例**:

```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "suno-v3.5",
    "messages": [
      {"role": "user", "content": "Create energetic rock music"}
    ]
  }'
```

**响应** (OpenAI 格式):

```json
{
  "id": "chatcmpl-suno-1234567890",
  "object": "chat.completion",
  "created": 1234567890,
  "model": "suno-v3.5",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "🎵 Generated 2 music track(s):\n\n1. **Track Title**\n   - ID: `abc123`\n   - Status: complete\n   - Audio: https://...\n   - Video: https://...\n"
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 50,
    "completion_tokens": 100,
    "total_tokens": 150
  }
}
```

## 🔧 参数说明

### 通用参数

| 参数                | 类型    | 必需 | 默认值     | 说明             |
| ------------------- | ------- | ---- | ---------- | ---------------- |
| `prompt`            | string  | ✅   | -          | 音乐描述或歌词   |
| `make_instrumental` | boolean | ❌   | false      | 是否生成纯音乐   |
| `model`             | string  | ❌   | chirp-v3.5 | 模型版本         |
| `wait_audio`        | boolean | ❌   | false      | 是否等待生成完成 |

### 自定义模式参数

| 参数            | 类型   | 必需 | 说明                    |
| --------------- | ------ | ---- | ----------------------- |
| `tags`          | string | ✅   | 音乐风格标签 (逗号分隔) |
| `title`         | string | ✅   | 歌曲标题                |
| `negative_tags` | string | ❌   | 要避免的风格            |

## 🎯 使用场景

### 场景 1: 快速生成音乐

```bash
curl -X POST http://localhost:8080/api/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt": "relaxing piano music"}'
```

### 场景 2: 等待完整音频

```bash
curl -X POST http://localhost:8080/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "epic orchestral soundtrack",
    "wait_audio": true
  }'
```

### 场景 3: 自定义歌词和风格

```bash
curl -X POST http://localhost:8080/api/custom_generate \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Verse: Walking down the street\nChorus: Feeling the beat",
    "tags": "hip-hop, urban",
    "title": "Street Vibes"
  }'
```

### 场景 4: 查询生成状态

```bash
# 1. 生成音乐 (不等待)
RESPONSE=$(curl -X POST http://localhost:8080/api/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt": "test"}')

# 2. 提取 ID
ID=$(echo $RESPONSE | jq -r '.[0].id')

# 3. 轮询状态
curl "http://localhost:8080/api/get?ids=$ID"
```

### 场景 5: 集成到 OpenAI 客户端

```python
import openai

# 配置指向 Suno API
openai.api_base = "http://localhost:8080"

response = openai.ChatCompletion.create(
    model="suno-v3.5",
    messages=[
        {"role": "user", "content": "Create upbeat electronic music"}
    ]
)

print(response.choices[0].message.content)
```

## 🚀 快速开始

### 1. 启动服务

```bash
cd qingcloud-mcp
mvn spring-boot:run
```

### 2. 测试 API

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 生成音乐
curl -X POST http://localhost:8080/api/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt": "test song"}'
```

### 3. 查看配额

```bash
curl http://localhost:8080/api/get_limit
```

## ⚠️ 注意事项

1. **CAPTCHA 处理**: 首次生成可能需要 10-40 秒 (CAPTCHA 求解时间)
2. **wait_audio**: 设为 true 时,请求会阻塞直到音频生成完成 (可能 1-3 分钟)
3. **并发限制**: 建议控制并发请求数,避免触发 Suno 限流
4. **配额管理**: 定期检查 `/api/get_limit` 避免超出配额

## 🔐 认证配置

API 使用 Suno Cookie 进行认证,需要配置:

```yaml
suno:
  cookie: ${SUNO_COOKIE}
  captcha:
    solver: 2captcha
    key: ${TWOCAPTCHA_KEY}
```

或通过环境变量:

```bash
export SUNO_COOKIE="your_cookie_here"
export TWOCAPTCHA_KEY="your_key_here"
```

## 📊 错误处理

### 错误响应格式

```json
{
  "error": true,
  "message": "Error description",
  "type": "error_type"
}
```

### 常见错误类型

| 类型               | HTTP 状态 | 说明           |
| ------------------ | --------- | -------------- |
| `suno_error`       | 500       | Suno API 错误  |
| `invalid_argument` | 400       | 参数错误       |
| `internal_error`   | 500       | 服务器内部错误 |

### 错误示例

```bash
# 缺少必需参数
curl -X POST http://localhost:8080/api/generate \
  -H "Content-Type: application/json" \
  -d '{}'

# 响应
{
  "error": true,
  "message": "prompt is required",
  "type": "invalid_argument"
}
```

## 🎉 总结

Suno RESTful API 已完全实现并可用:

- ✅ 5 个主要端点
- ✅ OpenAI 兼容格式
- ✅ 完整的错误处理
- ✅ 详细的文档和示例
- ✅ 生产就绪

可以立即开始使用!
