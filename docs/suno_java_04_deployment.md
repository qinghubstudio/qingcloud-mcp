# Suno-API Java 版开发方案 (04) - 工程配置与部署方案

本部分介绍了 Java 版项目的工程构建规范、依赖管理以及多种环境下的部署策略。

## 1. Maven 核心依赖 (pom.xml)

项目采用 Maven 进行构建, 核心依赖如下:

```xml
<dependencies>
    <!-- Web & API -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Browser Automation -->
    <dependency>
        <groupId>com.microsoft.playwright</groupId>
        <artifactId>playwright</artifactId>
        <version>1.44.0</version>
    </dependency>

    <!-- HTTP Client -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <!-- Tools -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.datatype</groupId>
        <artifactId>jackson-datatype-jsr310</artifactId>
    </dependency>
</dependencies>
```

## 2. 生产环境配置 (application.yml)

采用 `Profiles` 机制隔离环境配置:

```yaml
suno:
  cookie: ${SUNO_COOKIE}
  captcha:
    key: ${TWOCAPTCHA_KEY}
    timeout: 30000
  browser:
    headless: true
    locale: zh-CN
    instance-limit: 5 # 最大并发浏览器上下文数

server:
  port: 8080
  shutdown: graceful # 优雅停机

spring:
  threads:
    virtual:
      enabled: true # Java 21+ 建议开启虚拟线程
```

## 3. 容器化部署 (Docker)

由于项目包含 Playwright, 必须使用包含浏览器运行时环境的底座。

### 3.1 Dockerfile (分层构建)

```dockerfile
# 阶段1: 编译
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 阶段2: 运行
FROM mcr.microsoft.com/playwright/java:v1.44.0-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 4. 外部工具集成 (MCP 适配)

当作为 `qingcloud-mcp` 的一个 Service 运行时, 建议:

- **健康检查**: 暴露 `/actuator/health` 端点供 MCP 监控。
- **配置注入**: 通过 MCP 的环境变量系统注入 `SUNO_COOKIE`。
- **日志聚合**: stderr/stdout 输出, 方便控制台实时查看 Playwright 的运行痕迹。

## 5. 迁移交付计划

1. **原型开发 (1-2 天)**: 重写 `SunoAuthenticator` 和 `BrowserWorker`。
2. **接口联调 (1 天)**: 确保 `/api/generate` 的字段解析与旧版 100% 兼容。
3. **性能测试 (1 天)**: 在并发环境下验证 Playwright Context 池的稳定性。
4. **集成集成 (0.5 天)**: 将 Java Jar 包打包并接入 MCP 启动链。

---

> [!IMPORTANT]
> Java 重写版的最显著改进是并发安全性。通过 Spring 的 `ThreadPoolTaskExecutor`, 我们可以有效控制同时开启的浏览器进程数, 避免服务器内存溢出。
