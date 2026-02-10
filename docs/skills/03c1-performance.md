# 性能优化最佳实践

## 并发控制

### 问题：短时间大量请求导致限流

```
❌ 不推荐：
for note in notes:
    comment(note)  # 立即执行100次

结果：触发平台限流，账号被封
```

```
✅ 推荐：
AI 自动控制：
- 检测批量操作
- 自动添加间隔（2-5秒）
- 分批执行
- 监控限流信号

你：给100条笔记都点赞

AI：
  检测到批量操作，自动优化：
  ✓ 分10批执行
  ✓ 每批间隔5秒
  ✓ 预计总时间：8分钟
  ✓ 避免触发风控
```

### 并发配置

```yaml
# 建议配置
concurrency:
  max_parallel: 3 # 最多3个并发
  interval_ms: 2000 # 间隔2秒
  batch_size: 10 # 每批10个
  retry_on_limit: true # 限流后重试
```

---

## 缓存策略

### 用户信息缓存

```
场景：频繁查询同一用户

❌ 不推荐：
每次都调用 getUserProfile

✅ 推荐：
AI 自动缓存：
- 首次查询：调用API
- 后续查询：使用缓存
- 缓存时效：60分钟
- 手动刷新：支持

你：查看用户 @咖啡研究所 的主页
AI：✓ 获取并缓存

你：这个用户的粉丝是多少？
AI：✓ 使用缓存（无需再次请求）
```

### 搜索结果缓存

```yaml
cache_config:
  search_results:
    ttl: 300s # 5分钟
    max_size: 100 # 最多100条
  user_profiles:
    ttl: 3600s # 1小时
    max_size: 50
```

---

## 资源管理

### 浏览器资源

```
问题：长时间运行导致内存泄漏

✓ 自动管理策略：
- 每个任务使用独立页面
- 任务完成后立即关闭
- 定时清理僵尸进程
- 内存使用监控

配置：
browser:
  max_pages: 5          # 最多5个页面
  page_timeout: 30s     # 页面超时
  auto_close: true      # 自动关闭
```

### 文件资源

```
临时文件清理：
✓ 图片上传后删除本地文件
✓ 视频导出后清理缓存
✓ 定时清理超过3天的临时文件

storage:
  temp_dir: /tmp/mcp
  max_age_days: 3
  auto_cleanup: true
```

---

## API 调用优化

### 批量操作合并

```
场景：需要获取多个笔记详情

❌ 低效：
for note_id in note_ids:
    getNoteDetail(note_id)  # 100次请求

✅ 优化：
AI 智能合并：
- 检测批量需求
- 使用批量接口（如果支持）
- 或合理分批 + 缓存

实际调用：
batch_size = 20
for batch in chunks(note_ids, 20):
    getNoteDetailBatch(batch)  # 5次请求
    sleep(2)
```

### 请求去重

```
场景：重复请求同一资源

AI 自动识别：
你：查看笔记 abc123
AI：✓ 调用 API

你：再看一下刚才那个笔记
AI：✓ 使用缓存（识别到重复）
```

---

## 长任务处理

### 异步生成

```
场景：音乐生成需要 60秒

❌ 阻塞方式：
生成音乐... (等待60秒)

✅ 异步方式：
AI 智能处理：
1. 提交生成任务
2. 返回任务ID
3. 后台轮询状态
4. 完成后通知

你：生成一首咖啡店背景音乐

AI：
  ✓ 任务已提交 (task_abc)
  你可以继续做其他事情

  (30秒后)

  💡 您的音乐已生成完成！
  🔗 下载链接：...
```

### 超时控制

```yaml
timeouts:
  search: 10s # 搜索超时
  upload: 60s # 上传超时
  generate_music: 120s # 音乐生成超时
  default: 30s # 默认超时
```

---

## 性能监控

### 关键指标

```
监控维度：
📊 响应时间
- P50: <500ms
- P95: <2s
- P99: <5s

📈 成功率
- 目标：>99%
- 告警阈值：<95%

💾 资源使用
- 内存：<2GB
- CPU：<70%
- 磁盘：<80%
```

### 性能日志

```
[PERF] searchNotes completed in 423ms
[PERF] uploadImage completed in 1.2s
[WARN] getUserProfile slow: 3.5s
[ERROR] publishPost timeout after 30s
```

---

## 优化检查清单

- [ ] 批量操作已添加间隔
- [ ] 缓存策略已配置
- [ ] 浏览器资源自动释放
- [ ] 临时文件定时清理
- [ ] API 调用已合并去重
- [ ] 长任务使用异步处理
- [ ] 超时时间合理设置
- [ ] 性能监控已启用

---

[返回最佳实践目录](./03c-best-practices.md)
