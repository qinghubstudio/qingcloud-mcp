# 监控与日志最佳实践

## 日志规范

### 日志级别

```
DEBUG：详细的调试信息
INFO：关键操作记录
WARN：潜在问题警告
ERROR：错误信息
```

### 日志格式

```
[时间] [级别] [模块] 消息 [上下文]

示例：
[2026-01-05 15:30:25] [INFO] [SearchTool]
  Search completed: keyword="咖啡", results=20, took=423ms

[2026-01-05 15:31:10] [WARN] [UploadTool]
  Image size exceeds limit: 6MB > 5MB, auto-compressing

[2026-01-05 15:32:05] [ERROR] [PublishTool]
  Publish failed: {"code":429, "message":"Rate limit"}
```

---

## 性能监控

### 关键指标

```yaml
metrics:
  # 响应时间
  response_time:
    p50: 500ms
    p95: 2s
    p99: 5s

  # 成功率
  success_rate:
    target: 99%
    alert_threshold: 95%

  # 吞吐量
  throughput:
    requests_per_minute: 100
    max_concurrent: 10
```

### 慢查询监控

```
[PERF] Slow operation detected:
  Tool: searchNotes
  Duration: 3.5s (threshold: 2s)
  Keyword: "咖啡"
  Suggestion: Add caching or optimize query
```

---

## 业务监控

### 操作统计

```
每日统计报告：
📊 2026-01-05

总操作数：1,256
├─ 搜索：456 (36.3%)
├─ 发布：89 (7.1%)
├─ 评论：234 (18.6%)
├─ 上传：178 (14.2%)
└─ 其他：299 (23.8%)

成功率：98.2%
平均响应：1.2s
```

### 用户行为分析

```
活跃时段：
⏰ 08:00-10:00: 23%
⏰ 12:00-14:00: 18%
⏰ 20:00-22:00: 35%

热门功能：
🔥 内容搜索: 36.3%
🔥 互动操作: 18.6%
🔥 内容发布: 7.1%
```

---

## 告警机制

### 告警规则

```yaml
alerts:
  - name: high_error_rate
    condition: error_rate > 5%
    severity: critical
    action:
      - send_email
      - send_sms

  - name: slow_response
    condition: p95_latency > 5s
    severity: warning
    action:
      - send_email

  - name: cookie_expired
    condition: 401_count > 3
    severity: critical
    action:
      - send_notification
      - auto_disable
```

### 告警示例

```
🚨 Critical Alert

时间：2026-01-05 15:45:30
问题：错误率超过阈值
当前值：7.2% (阈值 5%)

详情：
- 429错误：45次
- 超时错误：12次
- 其他：8次

建议：
1. 检查 Cookie 是否有效
2. 降低请求频率
3. 启用降级方案
```

---

## 链路追踪

### 请求追踪

```
Trace ID: trace_abc123

Timeline:
[0ms] Request received: searchNotes
[5ms] Validate parameters ✓
[10ms] Check cache ✗ (miss)
[15ms] Call Playwright
[450ms] Page loaded
[750ms] Extract data
[800ms] Format response
[820ms] Response sent ✓

Total: 820ms
```

### 错误追踪

```
Error Trace: error_xyz789

Call Stack:
1. publishPost()
2.   uploadImages()
3.     compressImage() ✓
4.     uploadToServer() ✗
       → TimeoutException after 30s

Context:
- Images: 3 files
- Total size: 12MB
- Network: unstable

Root Cause: 网络不稳定导致上传超时
```

---

## 日志管理

### 日志存储

```yaml
logging:
  path: /var/log/qingcloud-mcp
  max_size: 100MB
  max_files: 30
  compress: true

  levels:
    root: INFO
    com.qingcloud.mcp: DEBUG
    org.springframework: WARN
```

### 日志清理

```bash
# 自动清理策略
- 保留最近 30 天日志
- 压缩超过 7 天的日志
- 删除超过 90 天的日志

# 手动清理
find /var/log/qingcloud-mcp -name "*.log" -mtime +90 -delete
```

---

## 可视化监控

### Grafana 面板

```
仪表盘指标：
📊 请求量趋势图
📈 成功率曲线
⏱️ 响应时间分布
💾 资源使用情况
🔥 热门功能排行
```

### 实时监控

```
当前状态 (实时)：
━━━━━━━━━━━━━━━━━━━━

活跃请求：5
平均响应：1.2s
成功率：98.5%
内存使用：1.2GB / 2GB
CPU使用：45%

最近操作：
15:50:30 searchNotes ✓ 420ms
15:50:32 uploadImage ✓ 1.2s
15:50:35 publishPost ✓ 850ms
```

---

## 健康检查

### 端点配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info

  health:
    show-details: always
```

### 健康状态

```json
GET /actuator/health

{
  "status": "UP",
  "components": {
    "browser": {
      "status": "UP",
      "details": {
        "activePAs": 2,
        "maxPages": 5
      }
    },
    "xiaohongshu": {
      "status": "UP",
      "details": {
        "cookieValid": true,
        "lastCheck": "2026-01-05T15:50:00"
      }
    }
  }
}
```

---

## 监控检查清单

- [ ] 配置了结构化日志
- [ ] 关键操作有性能监控
- [ ] 设置了告警规则
- [ ] 启用链路追踪
- [ ] 日志定期清理
- [ ] 健康检查端点可访问
- [ ] 监控面板已部署

---

[返回最佳实践目录](./03c-best-practices.md)
