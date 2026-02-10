# 安全与合规最佳实践

## Cookie 安全管理

### 安全存储

```yaml
❌ 不推荐：
cookie: "a1=xxx; webId=yyy; ..."  # 明文存储

✅ 推荐：
cookie_encrypted: "AES256_ENCRYPTED_DATA"
encryption_key: env.COOKIE_SECRET_KEY
```

### Cookie 示例配置

```properties
# application.properties
xhs.cookie.storage=encrypted
xhs.cookie.key.location=file:///secure/cookie.key
xhs.cookie.auto-refresh=true
xhs.cookie.expires-check=true
```

### 定期刷新

```
AI 自动管理：
✓ 每24小时检查有效性
✓ 过期前自动提醒
✓ 支持自动登录刷新

你：检查 Cookie 状态

AI：
  ⚠️ Cookie 将在3天后过期
  建议：重新导入最新 Cookie
```

---

## 敏感信息保护

### 日志脱敏

```java
// 自动脱敏
logger.info("User login: {}", maskUserId(userId));
// 输出: User login: 5f8a****3d

logger.info("Cookie: {}", maskCookie(cookie));
// 输出: Cookie: a1=***; webId=***
```

### 数据加密

```
敏感数据加密存储：
✓ 用户 Cookie
✓ 账号密码
✓ API Token
✓ 个人信息

加密方式：
- 算法：AES-256-GCM
- 密钥：环境变量
- 轮换：每90天
```

---

## 风控应对

### 频率限制

```
平台风控指标：
⚠️ 1分钟内评论 > 5条
⚠️ 1小时内点赞 > 100次
⚠️ IP地址频繁切换
⚠️ 设备指纹异常

AI 自动预防：
✓ 智能限速
✓ 随机间隔
✓ 模拟人工行为
```

### 异常行为检测

```
你：批量点赞1000条笔记

AI 安全提示：
  ⚠️ 检测到高风险操作

  风险评估：
  - 数量过大 (建议<100)
  - 可能触发风控
  - 账号封禁风险

  建议方案：
  1. 分10天执行，每天100条
  2. 随机间隔 3-8秒
  3. 仅工作时间执行

  是否继续？
```

---

## 账号安全

### 多账号隔离

```yaml
accounts:
  - name: account_1
    cookie: encrypted_cookie_1
    rate_limit: conservative

  - name: account_2
    cookie: encrypted_cookie_2
    rate_limit: normal
# 不同账号独立配置
```

### 登录状态监控

```
AI 主动监控：
✓ 每小时检查登录状态
✓ 异常登出立即告警
✓ 自动切换备用账号

告警示例：
🚨 账号 account_1 已登出
   原因：Cookie 失效
   已自动切换到 account_2
```

---

## 合规建议

### 内容审核

```
发布前自动检查：
✓ 敏感词过滤
✓ 违禁内容识别
✓ 版权风险提示

你：发布一篇包含"最好"的笔记

AI：
  ⚠️ 检测到广告法敏感词："最好"

  建议替换：
  - "很棒的"
  - "推荐的"
  - "优质的"

  已自动替换为："很棒的"
```

### 数据合规

```
GDPR/个人信息保护：
✓ 用户数据加密
✓ 访问日志记录
✓ 数据保留期限(90天)
✓ 支持数据导出/删除
```

### 接口使用规范

```
遵守平台规则：
✓ 尊重 robots.txt
✓ 遵守 API 频率限制
✓ 不进行恶意爬取
✓ 声明自动化工具身份
```

---

## 安全检查清单

- [ ] Cookie 已加密存储
- [ ] 敏感信息已脱敏
- [ ] 日志不包含明文密码
- [ ] 配置了频率限制
- [ ] 启用登录状态监控
- [ ] 内容发布前审核
- [ ] 多账号已隔离
- [ ] 备份恢复机制已建立

---

[返回最佳实践目录](./03c-best-practices.md)
