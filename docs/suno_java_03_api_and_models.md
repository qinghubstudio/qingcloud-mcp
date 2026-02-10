# Suno-API Java 版开发方案 (03) - API 接口与数据模型设计

本部分定义了 Spring Boot 暴露的 RESTful 接口协议以及内部使用的数据结构。

## 1. 全局配置与异常拦截

在 Java 版中, 我们通过 `@RestControllerAdvice` 统一处理异常和 CORS。

### 1.1 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(SunoException.class)
    public ResponseEntity<ErrorResponse> handleSunoException(SunoException e) {
        return ResponseEntity.status(e.getStatus())
            .body(new ErrorResponse(e.getMessage()));
    }
}
```

## 2. 核心数据模型 (DTO)

### 2.1 音乐生成请求 (GenerateRequest)

```java
@Data
public class GenerateRequest {
    @NotBlank(message = "Prompt cannot be empty")
    private String prompt;

    private Boolean makeInstrumental = false;
    private String model = "chirp-v3.5";
    private Boolean waitAudio = false;

    // 自定义模式特有字段
    private String tags;
    private String title;
    private String negativeTags;
}
```

### 2.2 响应对象 (AudioClipResponse)

使用 Jackson 注解映射字段名以保持与原 API 兼容:

```java
@Data
public class AudioClipResponse {
    private String id;
    private String title;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("audio_url")
    private String audioUrl;

    private String status; // submitted, queued, streaming, complete, error

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    private String lyric;
}
```

## 3. 控制层接口设计 (Controller)

### 3.1 核心音乐接口 (SunoController)

```java
@RestController
@RequestMapping("/api")
public class SunoController {

    @Autowired
    private SunoApiService sunoService;

    @PostMapping("/generate")
    public List<AudioClipResponse> generate(@Valid @RequestBody GenerateRequest request) {
        return sunoService.generate(request);
    }

    @PostMapping("/custom_generate")
    public List<AudioClipResponse> customGenerate(@Valid @RequestBody GenerateRequest request) {
        return sunoService.customGenerate(request);
    }

    @GetMapping("/get")
    public List<AudioClipResponse> getInfo(@RequestParam(required = false) String ids,
                                          @RequestParam(defaultValue = "1") Integer page) {
        return sunoService.getClips(ids, page);
    }
}
```

### 3.2 进阶处理接口

| Endpoint              | Method | 说明                                              |
| :-------------------- | :----- | :------------------------------------------------ |
| `/api/extend_audio`   | POST   | 续写逻辑, 接收 `audio_id` 和 `continue_at` 参数。 |
| `/api/generate_stems` | POST   | 音轨分离任务派发。                                |
| `/api/get_limit`      | GET    | 返回积分余额 DTO。                                |

## 4. OpenAI 兼容层设计 (V1 API)

此接口允许将本项目作为 AI 工具插件集成:

```java
@PostMapping("/v1/chat/completions")
public String chatCompletions(@RequestBody String body) {
    // 逻辑: 1. 解析 Chat 格式 Prompt -> 2. 调用生成服务 -> 3. 构造 MD 格式回复
    return openaiService.processChatRequest(body);
}
```

## 5. 序列化优化

- **时间格式化**: 统一使用 ISO-8601。
- **Null 值忽略**: `@JsonInclude(JsonInclude.Include.NON_NULL)` 确保响应报文精简。
- **枚举处理**: 对 `status` 字段使用强类型 Enum 并在序列化时转换为小写字符串。

---

> [!NOTE]
> 本文档完成了系统“面子”工程的搭建。最后的文档 **Java-04: 工程配置与部署方案** 将介绍如何将整套系统打包并作为生产环境运行。
