# 异常处理最佳实践

## 常见错误及解决方案

### 1. Cookie 失效 (401 Unauthorized)

```
错误信息：
{"code": 401, "message": "Unauthorized"}

AI 自动处理：
✓ 检测到 401 错误
✓ 识别为登录失效
✓ 提示更新 Cookie

你：搜索咖啡笔记

AI：
  ❌ 检测到 Cookie 已失效

  请重新导入 Cookie：
  1. 打开浏览器登录小红书
  2. F12 → Application → Cookies
  3. 复制完整 Cookie 字符串
  4. 使用 setCookies 工具导入
```

### 2. 限流错误 (429 Too Many Requests)

```
错误信息：
{"code": 429, "message": "Rate limit exceeded"}

AI 智能重试：
✓ 检测到限流
✓ 计算等待时间
✓ 自动延迟重试

执行日志：
[WARN] Rate limit detected, waiting 60s...
[INFO] Retrying after cooldown...
[SUCCESS] Request completed
```

### 3. 网络超时 (Timeout)

```
错误信息：
TimeoutException after 30s

AI 重试策略：
1次失败 → 等待2s重试
2次失败 → 等待5s重试
3次失败 → 提示用户检查网络

你：上传图片

AI：
  第1次：超时，2秒后重试...
  第2次：超时，5秒后重试...
  第3次：✓ 上传成功
```

---

## 重试策略配置

### 指数退避

```yaml
retry:
  max_attempts: 3
  initial_delay: 2s
  max_delay: 30s
  multiplier: 2.0
# 实际延迟：2s, 4s, 8s
```

### 针对性重试

```yaml
retry_rules:
  - error_code: 429
    max_attempts: 5
    delay: 60s

  - error_code: 500
    max_attempts: 3
    delay: 5s

  - error_code: 401
    max_attempts: 0 # 不重试，直接提示
```

---

## 降级方案

### 功能降级

```
场景：图片识别服务不可用

AI 自动降级：
✓ 检测服务异常
✓ 跳过图片识别
✓ 使用备用方案

你：发布笔记，包含自动识别的图片标签

AI：
  ⚠️ 图片识别服务暂时不可用

  降级方案：
  - 跳过智能标签
  - 使用默认分类
  - 笔记正常发布 ✓
```

### 数据降级

```
场景：无法获取精确数据

备选方案：
精确数据 → 缓存数据 → 默认值

示例：
1. 尝试获取最新粉丝数 ❌
2. 使用5分钟前缓存 ✓
3. 提示：数据可能有延迟
```

---

## 错误分类处理

### 用户错误 (4xx)

```
处理原则：立即提示用户

400 Bad Request：
→ 检查参数格式
→ 提供正确示例

401 Unauthorized：
→ 提示更新 Cookie
→ 指导登录流程

404 Not Found：
→ 确认 ID 是否正确
→ 检查资源是否已删除
```

### 服务错误 (5xx)

```
处理原则：自动重试 + 降级

500 Internal Server Error：
→ 等待5秒后重试
→ 最多3次

503 Service Unavailable：
→ 等待30秒后重试
→ 启用降级方案
```

---

## 异常监控

### 错误率统计

```
监控指标：
📊 总请求数：10,000
❌ 失败数：150
✅ 成功率：98.5%

错误分布：
- 429 (限流)：80次 (53%)
- 超时：45次 (30%)
- 其他：25次 (17%)
```

### 告警规则

```yaml
alerts:
  - name: high_error_rate
    condition: error_rate > 5%
    action: send_email

  - name: cookie_expired
    condition: 401_count > 3
    action: send_notification
```

---

## 错误恢复

### 状态回滚

```
场景：批量操作中断

AI 智能恢复：
✓ 记录执行进度
✓ 失败时自动回滚
✓ 支持断点续传

你：批量发布100篇笔记

AI：
  进度：[████░░] 45/100

  ❌ 发生错误 (Cookie 失效)

  已完成：45篇 ✓
  待处理：55篇

  重新导入 Cookie 后可继续
```

### 数据一致性

```
关键操作事务化：
1. 上传图片
2. 创建草稿
3. 发布笔记

失败恢复：
- 图片已上传 → 清理临时文件
- 草稿已创建 → 可选择继续或删除
- 发布失败 → 保留草稿待重试
```

---

## 调试技巧

### 详细日志

```
你：启用调试模式

AI：
  ✓ 调试模式已开启

  将输出详细信息：
  - 完整请求参数
  - 响应内容
  - 执行耗时
  - 中间状态
```

### 错误复现

```
你：上次的错误能复现吗？

AI：
  根据日志分析：

  错误时间：15:30:25
  操作：searchNotes
  参数：keyword="咖啡"
  错误：Timeout after 10s

  尝试复现... ✓ 成功复现
  根因：搜索服务响应慢

  建议：增加超时时间至30s
```

---

## 异常处理检查清单

- [ ] 所有 API 调用已添加异常处理
- [ ] 敏感操作配置了重试策略
- [ ] 关键错误有降级方案
- [ ] 错误信息对用户友好
- [ ] 启用错误监控和告警
- [ ] 支持操作回滚
- [ ] 有详细的调试日志

---

[返回最佳实践目录](./03c-best-practices.md)
