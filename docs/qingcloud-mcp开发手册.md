# Qingcloud MCP 开发手册

## 1. 项目概述

### 1.1 项目简介

Qingcloud MCP 是一个基于 Model Context Protocol (MCP) 的多平台工具服务,为 AI Agents 提供标准化的 API 接口。当前主要支持小红书(Xiaohongshu)平台的核心功能,未来将扩展支持 Comfy 和 Autoclip 等平台。

**项目特点:**

- 基于 MCP 标准协议,与 Claude Desktop 等 AI 客户端无缝集成
- 支持 STDIO 和 HTTP 两种传输模式
- 使用 Playwright 实现浏览器自动化,确保签名和 Cookie 的有效性
- 模块化架构设计,易于扩展新平台

### 1.2 核心功能

**小红书 (Xiaohongshu) 模块:**

- 用户认证: 二维码登录、Cookie 管理、登录状态检查
- 内容浏览: 搜索笔记、获取推荐、查看笔记详情、用户资料
- 内容创作: 发布图文笔记、发布视频笔记
- 社交互动: 发表评论

**未来规划:**

- Comfy 模块: AI 图像生成工作流管理
- Autoclip 模块: 视频自动剪辑服务

### 1.3 技术架构

```
┌─────────────────────────────────────────────┐
│         MCP Client (Claude/AI Agent)        │
└─────────────────┬───────────────────────────┘
                  │ STDIO / HTTP+SSE
┌─────────────────▼───────────────────────────┐
│            MCP Server (Spring Boot)         │
│  ┌──────────────────────────────────────┐   │
│  │      Transport Layer                 │   │
│  │  - STDIO Handler                     │   │
│  │  - HTTP Streaming Handler            │   │
│  └──────────────┬───────────────────────┘   │
│  ┌──────────────▼───────────────────────┐   │
│  │      Tool Router & Registry          │   │
│  └──────────────┬───────────────────────┘   │
│  ┌──────────────▼───────────────────────┐   │
│  │      Tool Factories                  │   │
│  │  - XHS Tools (10 tools)              │   │
│  │  - Comfy Tools (planned)             │   │
│  │  - Autoclip Tools (planned)          │   │
│  └──────────────┬───────────────────────┘   │
└─────────────────┼───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│         Browser Automation Layer            │
│  - Playwright Browser Manager              │
│  - Cookie Manager                           │
│  - Signature Injection                      │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│         Platform APIs                       │
│  - Xiaohongshu API                          │
│  - Comfy API (planned)                      │
│  - Autoclip API (planned)                   │
└─────────────────────────────────────────────┘
```

---

## 2. 技术栈

### 2.1 核心技术

| 技术         | 版本   | 用途         |
| ------------ | ------ | ------------ |
| Java         | 17     | 开发语言     |
| Spring Boot  | 3.5.7  | 应用框架     |
| MCP Java SDK | 0.17.0 | MCP 协议实现 |
| Playwright   | 1.40.0 | 浏览器自动化 |
| Jackson      | 2.19.2 | JSON 序列化  |
| Maven        | 3.x    | 构建工具     |

### 2.2 依赖说明

**Spring Boot 依赖:**

- `spring-boot-starter`: 核心启动器
- `spring-boot-starter-web`: Web 支持
- `spring-boot-starter-webflux`: 响应式 Web 支持 (用于 SSE)

**MCP SDK:**

- `mcp`: MCP 协议核心库
- `mcp-json-jackson2`: JSON 序列化支持

**浏览器自动化:**

- `playwright`: 浏览器控制和自动化

### 2.3 开发环境要求

- JDK 17 或更高版本
- Maven 3.6 或更高版本
- 至少 2GB 可用内存
- Windows/Linux/macOS 操作系统

---

## 3. 项目结构

```
qingcloud-mcp/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/qingcloud/mcp/
│   │   │       ├── McpApplication.java          # 应用入口
│   │   │       ├── config/                      # 配置类
│   │   │       │   ├── HttpMcpConfig.java       # HTTP 模式配置
│   │   │       │   └── StdioMcpConfig.java      # STDIO 模式配置
│   │   │       ├── xhs/                         # 小红书模块
│   │   │       │   ├── actions/                 # 业务操作
│   │   │       │   │   ├── LoginAction.java
│   │   │       │   │   ├── SearchAction.java
│   │   │       │   │   ├── FeedsAction.java
│   │   │       │   │   ├── PostDetailAction.java
│   │   │       │   │   ├── CommentAction.java
│   │   │       │   │   ├── UserProfileAction.java
│   │   │       │   │   └── PublishAction.java
│   │   │       │   ├── browser/                 # 浏览器管理
│   │   │       │   │   └── PlaywrightBrowserManager.java
│   │   │       │   ├── cookie/                  # Cookie 管理
│   │   │       │   │   └── CookieManager.java
│   │   │       │   ├── tools/                   # MCP 工具工厂
│   │   │       │   │   ├── LoginToolFactory.java
│   │   │       │   │   ├── CheckLoginStatusToolFactory.java
│   │   │       │   │   ├── SetCookiesToolFactory.java
│   │   │       │   │   ├── SearchToolFactory.java
│   │   │       │   │   ├── FeedsToolFactory.java
│   │   │       │   │   ├── PostDetailToolFactory.java
│   │   │       │   │   ├── CommentToolFactory.java
│   │   │       │   │   ├── UserProfileToolFactory.java
│   │   │       │   │   ├── PublishContentToolFactory.java
│   │   │       │   │   └── PublishVideoToolFactory.java
│   │   │       │   ├── model/                   # 数据模型
│   │   │       │   ├── util/                    # 工具类
│   │   │       │   └── config/                  # XHS 配置
│   │   │       ├── comfy/                       # Comfy 模块 (规划中)
│   │   │       └── autoclip/                    # Autoclip 模块 (规划中)
│   │   └── resources/
│   │       ├── application.yml                  # 应用配置
│   │       ├── inject.js                        # 浏览器注入脚本
│   │       └── cookies.json                     # Cookie 存储
│   └── test/                                    # 测试代码
├── docs/                                        # 文档
│   └── MCP_USAGE_EXAMPLES.md                    # 使用示例
├── pom.xml                                      # Maven 配置
├── Dockerfile                                   # Docker 镜像
├── docker-compose.yml                           # Docker Compose
└── README.md                                    # 项目说明
```

---

## 4. 开发指南

### 4.1 环境搭建

**1. 安装 JDK 17**

```bash
java -version  # 确认版本 >= 17
```

**2. 安装 Maven**

```bash
mvn -version  # 确认版本 >= 3.6
```

**3. 克隆项目**

```bash
git clone <repository-url>
cd qingcloud-mcp
```

**4. 安装 Playwright 浏览器**

```bash
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
```

### 4.2 本地开发

**1. 编译项目**

```bash
mvn clean compile
```

**2. 运行项目 (HTTP 模式)**

```bash
mvn spring-boot:run
```

服务将在 `http://localhost:8080` 启动,MCP 端点为 `http://localhost:8080/mcp`

**3. 运行项目 (STDIO 模式)**

```bash
java -Dmcp.transport.mode=stdio -jar target/qingcloud-mcp-0.1.0-SNAPSHOT.jar
```

**4. 打包项目**

```bash
mvn clean package -DskipTests
```

生成的 JAR 文件位于 `target/qingcloud-mcp-0.1.0-SNAPSHOT.jar`

### 4.3 开发工作流

**添加新的 MCP 工具步骤:**

1. **创建 Action 类** (业务逻辑层)

   - 位置: `src/main/java/com/qingcloud/mcp/xhs/actions/`
   - 实现具体的业务操作逻辑
   - 使用 Playwright 进行浏览器自动化

2. **创建 ToolFactory 类** (MCP 工具定义)

   - 位置: `src/main/java/com/qingcloud/mcp/xhs/tools/`
   - 定义工具的 schema (参数、描述等)
   - 使用 Builder 模式创建 `SyncToolSpecification`
   - 实现 `callHandler` 处理工具调用

3. **注册工具到配置类**

   - HTTP 模式: `HttpMcpConfig.java`
   - STDIO 模式: `StdioMcpConfig.java`
   - 将新工具添加到工具列表

4. **测试工具**
   - 编写单元测试
   - 使用 MCP 客户端测试实际调用

**示例: 创建一个新工具**

```java
// 1. Action 类
public class MyAction {
    private final Page page;

    public MyAction(Page page) {
        this.page = page;
    }

    public String execute(String param) {
        // 实现业务逻辑
        return "result";
    }
}

// 2. ToolFactory 类
public class MyToolFactory {
    public static SyncToolSpecification create(PlaywrightBrowserManager browserManager) {
        // 定义参数 schema
        Map<String, Object> paramProperty = Map.of(
            "type", "string",
            "description", "Parameter description"
        );

        JsonSchema inputSchema = new JsonSchema(
            "object",
            Map.of("param", paramProperty),
            List.of("param"),  // required fields
            null, null, null
        );

        Tool tool = new Tool(
            "myTool",
            "Tool description",
            null,
            inputSchema,
            null, null, null
        );

        return SyncToolSpecification.builder()
            .tool(tool)
            .callHandler((exchange, request) -> {
                try {
                    String param = (String) request.arguments().get("param");
                    Page page = browserManager.newPage();
                    MyAction action = new MyAction(page);
                    String result = action.execute(param);
                    page.close();

                    return CallToolResult.builder()
                        .content(List.of(new TextContent(result)))
                        .isError(false)
                        .build();
                } catch (Exception e) {
                    return CallToolResult.builder()
                        .content(List.of(new TextContent(
                            "{\"error\":\"" + e.getMessage() + "\"}")))
                        .isError(true)
                        .build();
                }
            })
            .build();
    }
}

// 3. 注册到配置类
@Bean
public McpServer httpMcpServer(...) {
    return McpServer.builder()
        .tools(List.of(
            // ... 其他工具
            MyToolFactory.create(browserManager)
        ))
        .build();
}
```

### 4.4 代码规范

**1. 命名规范**

- 类名: PascalCase (例: `LoginToolFactory`)
- 方法名: camelCase (例: `executeLogin`)
- 常量: UPPER_SNAKE_CASE (例: `MAX_RETRY_COUNT`)
- 包名: 小写 (例: `com.qingcloud.mcp.xhs.tools`)

**2. Builder 模式使用**

- `SyncToolSpecification` 必须使用 `.builder()...build()`
- `CallToolResult` 必须使用 `.builder()...build()`
- 不要使用已废弃的构造函数

**3. 异常处理**

- 所有工具调用必须有 try-catch 块
- 错误信息返回 JSON 格式
- 使用 `isError(true)` 标记错误结果

**4. 日志记录**

```java
private static final Logger logger = LoggerFactory.getLogger(ClassName.class);

logger.info("Operation started");
logger.error("Operation failed", exception);
```

### 4.5 调试技巧

**1. 启用详细日志**

在 `application.yml` 中:

```yaml
logging:
  level:
    com.qingcloud.mcp: DEBUG
    com.microsoft.playwright: DEBUG
```

**2. 浏览器调试模式**

设置 Playwright 为非 headless 模式:

```java
browserManager.setHeadless(false);
```

**3. 查看 MCP 通信**

使用 `curl` 测试 HTTP 端点:

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","id":1}'
```

**4. Cookie 调试**

检查 Cookie 文件:

```bash
cat cookies.json
```

---

## 5. 构建与部署

### 5.1 本地构建

**Maven 构建**

```bash
# 完整构建
mvn clean package

# 跳过测试
mvn clean package -DskipTests

# 仅编译
mvn clean compile
```

**使用构建脚本**

Windows:

```powershell
.\build.ps1
```

Linux/macOS:

```bash
./build.sh
```

### 5.2 Docker 部署

**1. 构建 Docker 镜像**

```bash
docker build -t qingcloud-mcp:latest .
```

**2. 运行容器**

```bash
docker run -d \
  -p 8080:8080 \
  -v $(pwd)/cookies.json:/app/cookies.json \
  --name qingcloud-mcp \
  qingcloud-mcp:latest
```

**3. 使用 Docker Compose**

```bash
docker-compose up -d
```

**4. 查看日志**

```bash
docker logs -f qingcloud-mcp
```

### 5.3 生产环境部署

**1. 环境变量配置**

```bash
export SERVER_PORT=8080
export MCP_TRANSPORT_MODE=http
export BROWSER_HEADLESS=true
```

**2. 使用 systemd (Linux)**

创建 `/etc/systemd/system/qingcloud-mcp.service`:

```ini
[Unit]
Description=Qingcloud MCP Service
After=network.target

[Service]
Type=simple
User=mcp
WorkingDirectory=/opt/qingcloud-mcp
ExecStart=/usr/bin/java -jar /opt/qingcloud-mcp/qingcloud-mcp-0.1.0-SNAPSHOT.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动服务:

```bash
sudo systemctl enable qingcloud-mcp
sudo systemctl start qingcloud-mcp
sudo systemctl status qingcloud-mcp
```

**3. 反向代理 (Nginx)**

```nginx
server {
    listen 80;
    server_name mcp.example.com;

    location /mcp {
        proxy_pass http://localhost:8080/mcp;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_buffering off;
    }
}
```

**4. 健康检查**

```bash
curl http://localhost:8080/actuator/health
```

### 5.4 性能优化

**1. JVM 参数调优**

```bash
java -Xms512m -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -jar qingcloud-mcp-0.1.0-SNAPSHOT.jar
```

**2. 浏览器资源管理**

- 限制同时打开的浏览器页面数量
- 及时关闭不用的页面
- 定期清理浏览器缓存

**3. Cookie 缓存**

- Cookie 自动保存到文件
- 启动时自动加载
- 减少登录次数

---

## 6. 测试指南

### 6.1 单元测试

**测试框架**

- JUnit 5
- Mockito (用于 Mock)

**运行测试**

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=LoginToolFactoryTest

# 运行特定测试方法
mvn test -Dtest=LoginToolFactoryTest#testLoginSuccess
```

**测试示例**

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LoginToolFactoryTest {

    @Test
    void testLoginToolCreation() {
        PlaywrightBrowserManager browserManager = new PlaywrightBrowserManager();
        SyncToolSpecification tool = LoginToolFactory.create(browserManager);

        assertNotNull(tool);
        assertEquals("login", tool.tool().name());
    }

    @Test
    void testLoginWithValidCredentials() {
        // 测试逻辑
    }
}
```

### 6.2 集成测试

**使用 MCP 客户端测试**

**1. 测试工具列表**

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/list",
    "id": 1
  }'
```

**2. 测试工具调用**

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
      "name": "checkLoginStatus",
      "arguments": {}
    },
    "id": 2
  }'
```

**3. 测试搜索功能**

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
      "name": "searchNotes",
      "arguments": {
        "keyword": "测试",
        "page": 1,
        "page_size": 5
      }
    },
    "id": 3
  }'
```

### 6.3 PowerShell 测试脚本

**创建测试脚本 `test_tools.ps1`**

```powershell
# 测试基础连接
function Test-Connection {
    $body = @{
        jsonrpc = "2.0"
        method = "initialize"
        params = @{
            protocolVersion = "2024-11-05"
            capabilities = @{}
            clientInfo = @{
                name = "test-client"
                version = "1.0"
            }
        }
        id = 1
    } | ConvertTo-Json -Depth 10

    $response = Invoke-RestMethod -Uri "http://localhost:8080/mcp" `
        -Method Post -Body $body -ContentType "application/json"

    Write-Host "Connection test: $($response.result.serverInfo.name)"
}

# 测试工具列表
function Test-ToolsList {
    $body = @{
        jsonrpc = "2.0"
        method = "tools/list"
        id = 2
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "http://localhost:8080/mcp" `
        -Method Post -Body $body -ContentType "application/json"

    Write-Host "Available tools: $($response.result.tools.Count)"
    $response.result.tools | ForEach-Object { Write-Host "  - $($_.name)" }
}

# 测试搜索工具
function Test-SearchTool {
    param([string]$Keyword = "测试")

    $body = @{
        jsonrpc = "2.0"
        method = "tools/call"
        params = @{
            name = "searchNotes"
            arguments = @{
                keyword = $Keyword
                page = 1
                page_size = 5
            }
        }
        id = 3
    } | ConvertTo-Json -Depth 10

    $response = Invoke-RestMethod -Uri "http://localhost:8080/mcp" `
        -Method Post -Body $body -ContentType "application/json"

    Write-Host "Search results: $($response.result.content[0].text)"
}

# 运行所有测试
Test-Connection
Test-ToolsList
Test-SearchTool -Keyword "咖啡"
```

**运行测试**

```powershell
.\test_tools.ps1
```

### 6.4 性能测试

**使用 Apache Bench**

```bash
# 测试并发请求
ab -n 100 -c 10 -p request.json -T application/json \
  http://localhost:8080/mcp
```

**使用 JMeter**

1. 创建 HTTP Request Sampler
2. 设置 URL: `http://localhost:8080/mcp`
3. 设置 Method: POST
4. 添加 JSON 请求体
5. 配置线程组 (用户数、循环次数)
6. 运行测试并查看结果

### 6.5 测试最佳实践

**1. 测试隔离**

- 每个测试独立运行
- 不依赖其他测试的状态
- 使用 `@BeforeEach` 和 `@AfterEach` 清理

**2. Mock 外部依赖**

```java
@Mock
private PlaywrightBrowserManager browserManager;

@Mock
private Page page;

@BeforeEach
void setUp() {
    MockitoAnnotations.openMocks(this);
    when(browserManager.newPage()).thenReturn(page);
}
```

**3. 测试覆盖率**

```bash
# 生成覆盖率报告
mvn clean test jacoco:report

# 查看报告
open target/site/jacoco/index.html
```

**4. 测试数据管理**

- 使用测试专用的 Cookie
- 不要使用生产环境数据
- 准备测试夹具 (fixtures)

---

## 7. 故障排除

### 7.1 常见问题

#### 问题 1: 编译失败 - 找不到 MCP SDK

**症状:**

```
Could not find artifact io.modelcontextprotocol.sdk:mcp:jar:0.17.0
```

**解决方案:**

1. 检查 Maven 仓库配置
2. 确认网络连接正常
3. 清理 Maven 缓存:

```bash
rm -rf ~/.m2/repository/io/modelcontextprotocol
mvn clean install
```

#### 问题 2: Playwright 浏览器未安装

**症状:**

```
Error: Executable doesn't exist at /path/to/chromium
```

**解决方案:**

```bash
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
```

#### 问题 3: Cookie 失效

**症状:**

```json
{ "code": -100, "message": "需要登录" }
```

**解决方案:**

1. 重新从浏览器获取 Cookie
2. 调用 `setCookies` 工具更新
3. 或使用 `login` 工具重新登录

#### 问题 4: 签名错误

**症状:**

```json
{ "code": -51, "message": "签名错误" }
```

**解决方案:**

1. 确认 `inject.js` 文件存在
2. 检查浏览器注入是否成功
3. 查看浏览器控制台日志
4. 尝试使用非 headless 模式调试

#### 问题 5: 端口被占用

**症状:**

```
Port 23000 is already in use
```

**解决方案:**

1. 修改配置文件中的端口:

```yaml
server:
  port: 23001
```

2. 或停止占用端口的进程:

```bash
# Windows
netstat -ano | findstr :23000
taskkill /PID <PID> /F

# Linux/macOS
lsof -i :23000
kill -9 <PID>
```

#### 问题 6: 内存不足

**症状:**

```
java.lang.OutOfMemoryError: Java heap space
```

**解决方案:**
增加 JVM 内存:

```bash
java -Xms512m -Xmx2g -jar qingcloud-mcp-0.1.0-SNAPSHOT.jar
```

#### 问题 7: MCP 客户端连接失败

**症状:**
Claude Desktop 无法连接到 MCP 服务

**解决方案:**

1. 确认服务已启动:

```bash
curl http://localhost:23000/mcp
```

2. 检查配置文件格式:

```json
{
  "mcpServers": {
    "xhs-mcp": {
      "url": "http://localhost:23000/mcp"
    }
  }
}
```

3. 查看 Claude Desktop 日志
4. 重启 Claude Desktop

### 7.2 日志分析

**启用详细日志**

在 `application.yml` 中:

```yaml
logging:
  level:
    root: INFO
    com.qingcloud.mcp: DEBUG
    com.microsoft.playwright: DEBUG
  file:
    name: logs/qingcloud-mcp.log
```

**日志位置**

- 控制台输出
- 文件: `logs/qingcloud-mcp.log`

**关键日志信息**

```
# 工具调用
=== Search Tool Called ===
keyword: 咖啡, page: 1

# 浏览器操作
Navigating to: https://www.xiaohongshu.com/search_result

# Cookie 管理
Loaded 15 cookies from file

# 错误信息
ERROR: Failed to execute search: timeout
```

### 7.3 性能问题

#### 问题: 响应速度慢

**诊断步骤:**

1. 检查网络延迟
2. 查看浏览器资源使用
3. 分析日志中的耗时操作

**优化方案:**

1. 启用 Cookie 缓存
2. 减少浏览器页面数量
3. 使用连接池
4. 调整超时设置

#### 问题: 内存占用高

**诊断:**

```bash
# 查看 Java 进程内存
jps -l
jmap -heap <PID>
```

**优化:**

1. 及时关闭浏览器页面
2. 定期清理缓存
3. 调整 JVM 参数
4. 限制并发请求数

### 7.4 调试工具

**1. JConsole**

```bash
jconsole <PID>
```

监控:

- 内存使用
- 线程状态
- CPU 使用率

**2. VisualVM**

```bash
jvisualvm
```

功能:

- 性能分析
- 内存快照
- 线程转储

**3. Chrome DevTools**

- 连接到 Playwright 浏览器
- 查看网络请求
- 调试 JavaScript 注入

**4. MCP Inspector**

```bash
# 使用 MCP 官方调试工具
npx @modelcontextprotocol/inspector http://localhost:23000/mcp
```

### 7.5 获取帮助

**文档资源:**

- 项目 README: `README.md`
- 使用示例: `docs/MCP_USAGE_EXAMPLES.md`
- 开发手册: `docs/DEVELOPMENT_MANUAL.md`

**社区支持:**

- GitHub Issues
- 技术论坛
- 开发者社区

**报告 Bug:**
提供以下信息:

1. 错误描述
2. 复现步骤
3. 日志输出
4. 环境信息 (OS, Java 版本等)
5. 配置文件

---

## 8. 附录

### 8.1 配置参考

**application.yml 完整配置**

```yaml
server:
  port: 23000

spring:
  application:
    name: qingcloud-mcp

# MCP 配置
mcp:
  transport:
    mode: http # stdio 或 http
  http:
    endpoint: /mcp

# 浏览器配置
browser:
  headless: true
  timeout: 30000

# Cookie 配置
cookie:
  file: cookies.json
  auto-save: true

# 日志配置
logging:
  level:
    root: INFO
    com.qingcloud.mcp: DEBUG
  file:
    name: logs/qingcloud-mcp.log
    max-size: 10MB
    max-history: 30
```

### 8.2 API 参考

**小 X 书 API 端点**

| 端点                           | 方法 | 说明     |
| ------------------------------ | ---- | -------- |
| `/api/sns/web/v1/search/notes` | GET  | 搜索笔记 |
| `/api/sns/web/v1/feed`         | GET  | 获取推荐 |
| `/api/sns/web/v1/note/{id}`    | GET  | 笔记详情 |
| `/api/sns/web/v1/comment/post` | POST | 发表评论 |
| `/api/sns/web/v1/user/{id}`    | GET  | 用户资料 |

**请求头要求**

```
Cookie: a1=xxx; webId=yyy; web_session=zzz
X-S: <signature>
X-T: <timestamp>
User-Agent: Mozilla/5.0 ...
```

### 8.3 MCP 工具清单

| 工具名               | 参数                           | 返回值                         | 说明         |
| -------------------- | ------------------------------ | ------------------------------ | ------------ |
| `login`              | 无                             | `{status, qrcodeUrl, message}` | 二维码登录   |
| `checkLoginStatus`   | 无                             | `{loggedIn, message}`          | 检查登录状态 |
| `setCookies`         | `cookieString`                 | `{success, message}`           | 设置 Cookie  |
| `searchNotes`        | `keyword, page, page_size`     | `{code, data: {items, total}}` | 搜索笔记     |
| `getFeeds`           | 无                             | `{code, data: {items, total}}` | 获取推荐     |
| `getPostDetail`      | `noteId, xsecToken`            | `{code, data: {...}}`          | 笔记详情     |
| `postComment`        | `noteId, xsecToken, content`   | `{code, message}`              | 发表评论     |
| `getUserProfile`     | `userId, xsecToken`            | `{code, data: {...}}`          | 用户资料     |
| `publish_content`    | `title, content, images, tags` | `{code, data: {...}}`          | 发布图文     |
| `publish_with_video` | `title, content, video, tags`  | `{code, data: {...}}`          | 发布视频     |

### 8.4 错误码参考

| 错误码 | 说明         | 解决方案                     |
| ------ | ------------ | ---------------------------- |
| -1     | 参数错误     | 检查参数格式和必填项         |
| -51    | 签名错误     | 重新获取签名或检查 inject.js |
| -100   | 未登录       | 调用 login 或 setCookies     |
| -200   | Cookie 失效  | 重新登录获取新 Cookie        |
| -300   | 请求频率过高 | 降低请求频率,添加延迟        |
| -400   | 内容违规     | 修改内容,避免敏感词          |
| -500   | 服务器错误   | 稍后重试                     |

### 8.5 环境变量

| 变量名               | 默认值       | 说明            |
| -------------------- | ------------ | --------------- |
| `SERVER_PORT`        | 23000        | HTTP 服务端口   |
| `MCP_TRANSPORT_MODE` | http         | 传输模式        |
| `BROWSER_HEADLESS`   | true         | 无头浏览器模式  |
| `COOKIE_FILE`        | cookies.json | Cookie 文件路径 |
| `LOG_LEVEL`          | INFO         | 日志级别        |

### 8.6 快捷命令

**开发常用命令**

```bash
# 编译
mvn compile

# 运行
mvn spring-boot:run

# 打包
mvn package -DskipTests

# 清理
mvn clean

# 测试
mvn test

# 安装浏览器
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
```

**Docker 命令**

```bash
# 构建镜像
docker build -t qingcloud-mcp .

# 运行容器
docker run -d -p 23000:23000 qingcloud-mcp

# 查看日志
docker logs -f qingcloud-mcp

# 停止容器
docker stop qingcloud-mcp

# 删除容器
docker rm qingcloud-mcp
```

### 8.7 相关资源

**官方文档**

- [MCP 协议规范](https://modelcontextprotocol.io/)
- [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk)
- [Playwright Java](https://playwright.dev/java/)
- [Spring Boot](https://spring.io/projects/spring-boot)

**工具和库**

- [Claude Desktop](https://claude.ai/desktop)
- [Maven](https://maven.apache.org/)
- [Docker](https://www.docker.com/)

**社区资源**

- GitHub Repository
- 技术博客
- 开发者论坛
- [QingHubStudio](https://qinghub.net)

### 8.8 版本历史

**v0.1.0 (当前版本)**

- ✅ 实现 MCP 协议支持
- ✅ 支持 STDIO 和 HTTP 两种传输模式
- ✅ 实现小红书 10 个核心工具
- ✅ 集成 Playwright 浏览器自动化
- ✅ Cookie 持久化管理
- ✅ Docker 部署支持

**未来规划**

- 🔄 Comfy 模块集成
- 🔄 Autoclip 模块集成
- 🔄 性能优化
- 🔄 更多测试覆盖
- 🔄 监控和告警

### 8.9 贡献指南

**代码贡献流程**

1. Fork 项目
2. 创建特性分支: `git checkout -b feature/my-feature`
3. 提交更改: `git commit -am 'Add new feature'`
4. 推送分支: `git push origin feature/my-feature`
5. 创建 Pull Request

**代码审查标准**

- 遵循代码规范
- 添加单元测试
- 更新文档
- 通过 CI 检查

**提交信息格式**

```
<type>(<scope>): <subject>

<body>

<footer>
```

类型:

- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式
- `refactor`: 重构
- `test`: 测试
- `chore`: 构建/工具

### 8.10 许可证

本项目采用 MIT 许可证。

---

## 结语

本开发手册涵盖了 Qingcloud MCP 项目的完整开发流程,从环境搭建到部署上线。希望能帮助开发者快速上手并高效开发。

如有问题或建议,欢迎通过 GitHub Issues 反馈。

**祝开发愉快! 🚀**

---

_文档版本: v1.0_  
_最后更新: 2025-12-23_
