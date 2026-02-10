# Suno-API Java 版开发方案 (01) - 项目架构与技术选型

## 1. 重构目标

将 Node.js 版 Suno-API 迁移至 Java Spring Boot 架构, 旨在实现:

- **企业级稳定性**: 利用 Java 的强类型和成熟的异常处理机制。
- **高性能并发**: 通过 Spring 的线程池和异步处理能力, 优化 CAPTCHA 处理吞吐量。
- **无缝集成**: 深度适配 `qingcloud-mcp` 的 Java 底层生态。

## 2. 核心技术栈选型

针对原项目的技术挑战, Java 版采用以下技术方案:

| 维度           | Java 选型                  | 理由                                                   |
| :------------- | :------------------------- | :----------------------------------------------------- |
| **基础框架**   | Spring Boot 3.3.0          | 最新的 LTS 版本, 支持 Java 17+ 和 原生镜像 (GraalVM)。 |
| **自动化测试** | Playwright for Java        | 官方支持, 接口与 Node.js 保持高度一致, 易于业务迁移。  |
| **异步处理**   | CompletableFuture + @Async | 解决生成音乐时的长时间阻塞, 实现非阻塞 API 响应。      |
| **HTTP 通信**  | Spring WebClient           | 比 RestTemplate 更现代的响应式客户端, 适合处理长连接。 |
| **定时任务**   | Spring Task                | 用于实现 Session 保活 (keepAlive) 的定时刷新逻辑。     |
| **配置中心**   | Spring Boot YAML           | 提供多环境 (dev/prod) 灵活配置。                       |

## 3. Java 项目包结构设计 (Architecture)

遵循 Spring Boot 经典的分层架构, 结合本项目的工具属性进行调整:

```text
com.qingcloud.suno
├── common              # 通用模块
│   ├── constant        # 常量定义 (Suno API 路径, 超时阈值)
│   ├── dto             # 数据传输对象 (Request/Response)
│   ├── exception       # 全局异常处理
│   └── util            # 工具类 (Cookie解析, 时间处理)
├── config              # 工程配置
│   ├── PlaywrightConfig # Playwright 实例池配置
│   └── WebClientConfig  # 响应式 HTTP 客户端配置
├── core                # 核心业务逻辑
│   ├── browser         # Playwright 自动化逻辑实现
│   ├── auth            # Clerk 认证与 Session 刷新器
│   └── service         # 业务入口类 (SunoApiService)
├── integration         # 外部服务集成
│   └── captcha         # 2Captcha SDK 适配器
└── web                 # 接口定义
    ├── controller      # REST 接口 (SunoController)
    └── v1              # OpenAI 兼容接口实现
```

## 4. 关键组件生命周期设计

### 4.1 SunoApiContext (单例)

管理全局唯一或基于 Cookie 隔离的核心对象。负责维护 `BrowserContext` 和当前有效的 `JWT Token`。

### 4.2 BrowserWorker (池化管理)

由于 Playwright 启动浏览器开销较大, Java 环境下将采用**上下文池(Context Pool)** 模式。

- 系统启动时预热少量 Browser 实例。
- 任务到来时, 动态创建 `BrowserContext` 并加载 Cookie。

### 4.3 KeepAliveTask (后台守护)

使用 `@Scheduled` 每隔固定时间(如 10 分钟)执行一次 JWT 刷新, 确保存储在内存中的会话始终有效。

---

> [!NOTE]
> 本文档定义了项目的技术底座。接下来的文档 **Java-02: 核心服务实现逻辑** 将深入代码层面, 展示如何用 Java 重写认证和验证码处理算法。
