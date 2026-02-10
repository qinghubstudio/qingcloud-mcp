# Suno 可插拔 CAPTCHA 解决方案架构

## ✅ 实现完成

已成功实现可插拔的 CAPTCHA 解决方案架构,支持灵活切换不同的解决策略。

## 📐 架构设计

### 核心接口: ICaptchaSolver

```java
public interface ICaptchaSolver {
    CaptchaSolution solve(byte[] screenshot, String instructions);
    void reportBad(String taskId);
    String getName();
}
```

### 三种实现

#### 1. TwoCaptchaSolver (生产环境)

**文件**: [`TwoCaptchaSolver.java`](file:///c:/workspace/jinghui/backend/common/qingcloud-mcp/src/main/java/com/qingcloud/mcp/suno/browser/TwoCaptchaSolver.java)

**特点**:

- ✅ 使用 2Captcha API
- ✅ 高准确率 (~90-95%)
- ✅ 完全自动化
- ❌ 付费服务 (~$2.99/1000 次)

**激活条件**:

```yaml
suno:
  captcha:
    solver: 2captcha # 默认值
    key: ${TWOCAPTCHA_KEY}
```

#### 2. ManualCaptchaSolver (开发环境)

**文件**: [`ManualCaptchaSolver.java`](file:///c:/workspace/jinghui/backend/common/qingcloud-mcp/src/main/java/com/qingcloud/mcp/suno/browser/ManualCaptchaSolver.java)

**特点**:

- ✅ 完全免费
- ✅ 100% 准确率
- ❌ 需要人工输入坐标
- ❌ 不适合批量处理

**激活条件**:

```yaml
suno:
  captcha:
    solver: manual
```

**使用方式**:

```
=== CAPTCHA DETECTED - Manual Input Required ===
Enter coordinates in format: x1,y1;x2,y2;x3,y3
For drag type: x1,y1;x2,y2 (pairs of start and end points)
Or press ENTER to skip and let browser handle it
Coordinates: 100,150;200,250;300,350
```

#### 3. MockCaptchaSolver (测试环境)

**文件**: [`MockCaptchaSolver.java`](file:///c:/workspace/jinghui/backend/common/qingcloud-mcp/src/main/java/com/qingcloud/mcp/suno/browser/MockCaptchaSolver.java)

**特点**:

- ✅ 完全免费
- ✅ 快速响应
- ❌ 返回随机坐标
- ❌ 仅用于测试代码流程

**激活条件**:

```yaml
suno:
  captcha:
    solver: mock
```

## 🔧 配置方式

### 方法 1: 环境变量

```bash
# 使用 2Captcha (生产)
export SUNO_CAPTCHA_SOLVER=2captcha
export TWOCAPTCHA_KEY=your_api_key_here

# 使用手动模式 (开发)
export SUNO_CAPTCHA_SOLVER=manual

# 使用 Mock (测试)
export SUNO_CAPTCHA_SOLVER=mock
```

### 方法 2: application.yml

```yaml
suno:
  captcha:
    solver: manual # 或 2captcha, mock
    key: ${TWOCAPTCHA_KEY:}
```

### 方法 3: Spring Profile

```yaml
# application-dev.yml
suno:
  captcha:
    solver: manual

# application-prod.yml
suno:
  captcha:
    solver: 2captcha
    key: ${TWOCAPTCHA_KEY}

# application-test.yml
suno:
  captcha:
    solver: mock
```

## 🎯 使用场景

| 场景      | 推荐方案    | 原因            |
| --------- | ----------- | --------------- |
| 本地开发  | Manual      | 免费,可控       |
| 单元测试  | Mock        | 快速,无依赖     |
| 集成测试  | Manual/Mock | 根据需求选择    |
| 生产环境  | 2Captcha    | 自动化,高准确率 |
| Demo 演示 | Manual      | 可展示流程      |

## 💡 扩展新的解决器

### 步骤 1: 实现接口

```java
@Service
@ConditionalOnProperty(name = "suno.captcha.solver", havingValue = "anticaptcha")
public class AntiCaptchaSolver implements ICaptchaSolver {

    @Override
    public CaptchaSolution solve(byte[] screenshot, String instructions) {
        // 调用 Anti-Captcha API
    }

    @Override
    public void reportBad(String taskId) {
        // 报告错误
    }

    @Override
    public String getName() {
        return "AntiCaptcha";
    }
}
```

### 步骤 2: 配置激活

```yaml
suno:
  captcha:
    solver: anticaptcha
```

## 🔍 工作原理

### Spring 条件化 Bean 加载

```java
@ConditionalOnProperty(name = "suno.captcha.solver", havingValue = "2captcha", matchIfMissing = true)
public class TwoCaptchaSolver implements ICaptchaSolver { ... }

@ConditionalOnProperty(name = "suno.captcha.solver", havingValue = "manual")
public class ManualCaptchaSolver implements ICaptchaSolver { ... }

@ConditionalOnProperty(name = "suno.captcha.solver", havingValue = "mock")
public class MockCaptchaSolver implements ICaptchaSolver { ... }
```

**原理**:

- Spring 根据 `suno.captcha.solver` 配置值
- 只加载匹配的 Bean
- `CaptchaTokenExtractor` 自动注入正确的实现

### 依赖注入

```java
public class CaptchaTokenExtractor {
    private final ICaptchaSolver captchaSolver;

    public CaptchaTokenExtractor(ICaptchaSolver captchaSolver) {
        this.captchaSolver = captchaSolver;
        logger.info("Using {} solver", captchaSolver.getName());
    }
}
```

## 📊 性能对比

| 解决器   | 响应时间 | 准确率 | 成本       | 自动化 |
| -------- | -------- | ------ | ---------- | ------ |
| 2Captcha | 10-30 秒 | ~95%   | $2.99/1000 | ✅     |
| Manual   | 即时     | 100%   | 免费       | ❌     |
| Mock     | <1 秒    | ~0%    | 免费       | ✅     |

## 🎉 优势总结

1. **灵活切换**: 一行配置即可切换解决方案
2. **环境隔离**: 开发/测试/生产使用不同策略
3. **易于扩展**: 新增解决器只需实现接口
4. **成本控制**: 开发时免费,生产时付费
5. **测试友好**: Mock 模式支持快速测试

## 📝 最佳实践

1. **开发阶段**: 使用 `manual` 模式,节省成本
2. **CI/CD**: 使用 `mock` 模式,快速验证流程
3. **生产环境**: 使用 `2captcha` 模式,保证可用性
4. **降级策略**: 2Captcha 失败时可切换到 Manual
5. **监控告警**: 记录解决器使用情况和成功率
