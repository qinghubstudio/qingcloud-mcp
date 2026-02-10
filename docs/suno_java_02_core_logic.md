# Suno-API Java 版开发方案 (02) - 核心服务实现逻辑

本部分深入探讨 Java 版如何实现原项目中最复杂的认证、验证码自动化及任务流逻辑。

## 1. 认证管理实现 (Clerk Auth)

在 Java 中, 我们使用 `SunoAuthenticator` 服务来管理与 Clerk 的会话。

### 1.1 会话刷新 (RefreshToken)

利用 Spring 的 WebClient 发送异步请求:

```java
@Service
public class SunoAuthenticator {
    private final WebClient clerkClient;
    private String jwtToken;

    public Mono<String> refreshJwt(String sessionId, String clientCookie) {
        return clerkClient.post()
            .uri("/v1/client/sessions/{sid}/tokens", sessionId)
            .header("Authorization", clientCookie)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(json -> json.get("jwt").asText())
            .doOnNext(token -> this.jwtToken = token);
    }
}
```

## 2. 浏览器自动化逻辑 (Playwright for Java)

Java 环境下使用 Playwright 的优势在于其强一致性的 API 链。

### 2.1 任务隔离设计

每个生成任务会分配一个独立的 `BrowserContext`, 并在任务结束时销毁:

```java
public String triggerCaptchaAndGetToken(String prompt) {
    try (BrowserContext context = browser.newContext()) {
        // 注入 Cookie
        context.addCookies(parseSunoCookies());
        Page page = context.newPage();
        page.navigate("https://suno.com/create");

        // 执行模拟输入
        page.locator("textarea").fill(prompt);
        page.locator("button:has-text('Create')").click();

        // 监控后台 API 请求并拦截 Token
        AtomicReference<String> token = new AtomicReference<>();
        page.onResponse(resp -> {
            if (resp.url().contains("/api/generate/v2/")) {
                // 解析请求体中的 token
                token.set(extractTokenFromRequest(resp.request()));
            }
        });

        // 如果触发了 CAPTCHA, 调用求解算法
        if (isCaptchaPresent(page)) {
            solveCaptcha(page);
        }

        return token.get();
    }
}
```

## 3. CAPTCHA 解决算法适配

针对拖动坐标验证码, Java 版实现如下逻辑:

1. **图片捕获**: 使用 `Locator.screenshot()` 获取挑战框 Base64。
2. **2Captcha 集成**:
   ```java
   public List<Coordinate> solveWithTwoCaptcha(byte[] screenshot) {
       // 调用 2Captcha Java SDK 或直接 HTTP Request
       // 返回点击点或拖动轨迹点坐标列表
       return twoCaptchaClient.solveCoordinates(screenshot, "Drag instructions...");
   }
   ```
3. **模拟鼠标轨迹**:
   ```java
   public void performDrag(Page page, List<Coordinate> points) {
       Mouse mouse = page.mouse();
       Coordinate start = points.get(0);
       Coordinate end = points.get(1);

       mouse.move(start.x, start.y);
       mouse.down();
       // 模拟人类平滑移动, steps 参数决定平滑度
       mouse.move(end.x, end.y, new Mouse.MoveOptions().setSteps(30));
       mouse.up();
   }
   ```

## 4. 异步生成流 (@Async)

音乐生成是一个长时间运行的操作。Java 版利用 `CompletableFuture` 实现非阻塞响应:

```java
@Async("sunoTaskExecutor")
public CompletableFuture<List<AudioClip>> generateMusicAsync(String prompt) {
    // 1. 获取 Token (可能涉及浏览器自动化)
    String token = browserService.triggerCaptchaAndGetToken(prompt);

    // 2. 发起 API 生成请求
    List<String> ids = sunoClient.postGenerate(prompt, token);

    // 3. 轮询状态直到资源就绪
    return pollUntilComplete(ids);
}
```

## 5. 日志脱敏与安全性

- **自定义 Jackson 脱敏注解**: 对敏感字段(如 `cookie`, `jwt`)在序列化时进行屏蔽。
- **线程本地存储 (ThreadLocal)**: 在处理用户请求过程中, 使用 `ThreadLocal` 存储临时的 `DeviceId`, 提高日志追踪能力。

---

> [!TIP]
> 文档 2 解决了最头疼的“如何重现原有 Node.js 动态能力”问题。接下来的 **Java-03: API 接口与数据对象定义** 将规范项目对外暴露的接口协议。
