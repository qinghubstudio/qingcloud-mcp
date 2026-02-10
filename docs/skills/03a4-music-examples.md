# 音乐生成示例（Suno）

## 示例 1：生成音乐

### 场景

使用 AI 生成一首音乐作品

### 对话示例

```
你：帮我生成一首轻松的咖啡馆背景音乐

AI：好的，我来为你生成。
    [调用 suno_generate 工具]

    🎵 音乐生成任务已创建
    任务 ID：task_abc123
    预计等待：30-60秒

    正在生成中...

    ✅ 生成完成！

    🎼 作品信息：
    标题：Cozy Coffee Moments
    时长：2:45
    风格：Lofi, Chill

    🔗 试听链接：https://...
```

### 工具调用

```json
{
  "tool": "suno_generate",
  "arguments": {
    "prompt": "轻松的咖啡馆背景音乐",
    "tags": "lofi, chill, acoustic",
    "make_instrumental": false
  }
}
```

### 返回结果

```json
{
  "code": 0,
  "success": true,
  "data": {
    "task_id": "task_abc123",
    "status": "submitted",
    "credits_left": 45
  }
}
```

---

## 示例 2：查询生成进度

### 场景

检查音乐生成任务状态

### 对话示例

```
你：查询任务 task_abc123 的状态

AI：正在查询...
    [调用 suno_get_music 工具]

    📊 任务状态：生成中
    进度：60%
    预计剩余：20秒
```

### 工具调用

```json
{
  "tool": "suno_get_music",
  "arguments": {
    "ids": ["task_abc123"]
  }
}
```

---

## 示例 3：查询配额

### 场景

检查剩余的生成次数

### 对话示例

```
你：查看我的 Suno 配额

AI：正在查询账户信息...
    [调用 suno_get_quota 工具]

    💎 配额信息
    剩余积分：45
    可生成：约15首歌曲
    下次重置：2026-02-01
```

### 工具调用

```json
{
  "tool": "suno_get_quota",
  "arguments": {}
}
```

---

## 示例 4：自定义歌词生成

### 场景

生成带自定义歌词的歌曲

### 对话示例

```
你：生成一首歌，歌词是：
    "Morning coffee, gentle breeze
     Finding peace in simple things..."

AI：收到你的歌词，正在创作...
    [调用 suno_custom_generate 工具]

    🎵 已提交自定义创作任务
    任务 ID：task_def456
    预计等待：60-90秒
```

### 工具调用

```json
{
  "tool": "suno_custom_generate",
  "arguments": {
    "prompt": "轻柔的民谣风格",
    "lyrics": "Morning coffee, gentle breeze\nFinding peace in simple things...",
    "tags": "folk, acoustic, soft",
    "title": "Simple Things"
  }
}
```

---

## 完整工作流示例

### 场景：创作完整音乐作品

```
你：帮我创作一首用于视频背景的音乐

AI 智能执行：

    1️⃣ 了解需求
    AI：请问视频主题是什么？情绪倾向？

    你：旅行 vlog，轻快活泼

    2️⃣ 检查配额
    AI：✓ 剩余配额充足(45积分)

    3️⃣ 生成音乐
    AI：✓ 提交生成任务 task_xyz

    4️⃣ 等待完成
    AI：生成中... 50%... 80%... 100%

    5️⃣ 交付结果
    AI：✅ 创作完成！

        🎵 Travel Vibes
        时长：2:30
        风格：Upbeat, Pop, Acoustic

        🔗 下载链接：https://...
        💾 已保存到本地：travel_bgm.mp3
```

---

## 实用技巧

### 优化提示词

```
❌ 不好的提示：
"生成音乐"

✅ 更好的提示：
"生成一首2分钟的轻快流行音乐，
适合旅行vlog，包含吉他和钢琴，
情绪积极向上"
```

### 批量生成

```
你：生成3首不同风格的背景音乐

AI 自动编排：
✓ 检查配额(需要9积分，当前45)
✓ 生成任务1: Lofi
✓ 生成任务2: Jazz
✓ 生成任务3: Acoustic
✓ 等待所有任务完成
✓ 返回3个音频文件
```

### 配额管理

```
AI 智能提醒：
- 配额不足时主动告知
- 建议最优的生成策略
- 避免浪费积分的失败尝试
```

---

## 注意事项

> 🎵 **音乐版权**：Suno 生成的音乐遵循其服务条款

> ⏱️ **生成时间**：通常 30-90 秒，高峰期可能更长

> 💎 **配额消耗**：每首歌约消耗 3 积分

---

[返回基础示例目录](./03a-basic-examples.md)
