# CapCut/VectCut 项目架构分析 - Part 4: Java 移植方案

## 核心移植策略

基于对 Python 项目的深入分析，Java 移植方案的核心策略是：

**不移植 pyJianYingDraft 库，而是直接操作剪映草稿的 JSON 结构**

### 为什么不移植 pyJianYingDraft？

1. **复杂度高**：该库有大量 Python 特性（动态类型、装饰器、元类等）
2. **维护成本**：需要跟随剪映更新
3. **不必要**：我们只需要生成正确的 JSON 结构

### 核心思路

```
Python 方式:
pyJianYingDraft → 生成 JSON → 剪映草稿

Java 方式:
直接生成 JSON → 剪映草稿
```

## 技术方案

### 1. JSON 模板 + 数据填充

使用剪映草稿的 JSON 模板，通过 Jackson 进行数据填充：

```java
// 1. 加载模板
ObjectMapper mapper = new ObjectMapper();
JsonNode template = mapper.readTree(
    getClass().getResourceAsStream("/templates/draft_template.json")
);

// 2. 修改数据
ObjectNode draftNode = (ObjectNode) template;
draftNode.put("duration", totalDuration);
draftNode.put("fps", 30);

// 3. 添加素材
ArrayNode videos = (ArrayNode) draftNode.path("materials").path("videos");
videos.add(createVideoMaterial(videoUrl, duration, width, height));

// 4. 添加轨道和片段
ArrayNode tracks = (ArrayNode) draftNode.path("tracks");
tracks.add(createVideoTrack(segments));
```

### 2. 使用 POJO 类

定义 Java 类映射 JSON 结构：

```java
@Data
public class DraftInfo {
    private String id;
    private int duration;
    private int fps;
    private CanvasConfig canvas_config;
    private Materials materials;
    private List<Track> tracks;
}

@Data
public class Materials {
    private List<VideoMaterial> videos;
    private List<AudioMaterial> audios;
    private List<Object> texts;
    private List<Object> stickers;
    // ...
}

@Data
public class VideoMaterial {
    private String id;
    private String material_name;
    private String path;
    private String type;  // "video" or "photo"
    private long duration;
    private int width;
    private int height;
    // ...
}
```

## 包结构设计

```
com.qingcloud.mcp.autoclip/
├── model/
│   ├── draft/
│   │   ├── DraftInfo.java
│   │   ├── CanvasConfig.java
│   │   ├── Materials.java
│   │   └── Track.java
│   ├── material/
│   │   ├── VideoMaterial.java
│   │   ├── AudioMaterial.java
│   │   ├── ImageMaterial.java
│   │   └── TextMaterial.java
│   ├── segment/
│   │   ├── VideoSegment.java
│   │   ├── AudioSegment.java
│   │   ├── TextSegment.java
│   │   └── Timerange.java
│   └── effect/
│       ├── Transition.java
│       ├── Filter.java
│       ├── Animation.java
│       └── Mask.java
├── template/
│   └── DraftTemplateManager.java
├── builder/
│   ├── DraftBuilder.java
│   ├── VideoSegmentBuilder.java
│   └── AudioSegmentBuilder.java
├── actions/
│   ├── CreateDraftAction.java
│   ├── AddVideoAction.java
│   ├── AddAudioAction.java
│   └── SaveDraftAction.java
├── tools/
│   └── (MCP Tool Factories)
└── util/
    ├── TimeUtil.java
    ├── MediaUtil.java
    └── FileUtil.java
```

## 关键类设计

### 1. DraftBuilder

```java
public class DraftBuilder {
    private DraftInfo draft;
    private Map<String, Object> cache;

    public DraftBuilder(int width, int height) {
        this.draft = loadTemplate();
        this.draft.getCanvas_config().setWidth(width);
        this.draft.getCanvas_config().setHeight(height);
        this.cache = new HashMap<>();
    }

    public DraftBuilder addVideo(
        String videoUrl,
        double start,
        double end,
        double targetStart,
        double speed
    ) {
        // 1. 创建素材
        VideoMaterial material = VideoMaterial.builder()
            .id(UUID.randomUUID().toString())
            .material_name(generateMaterialName(videoUrl))
            .remote_url(videoUrl)
            .type("video")
            .duration(0L)  // 稍后更新
            .build();

        draft.getMaterials().getVideos().add(material);

        // 2. 创建片段
        VideoSegment segment = VideoSegment.builder()
            .id(UUID.randomUUID().toString())
            .material_id(material.getId())
            .target_timerange(new Timerange(
                (long)(targetStart * 1_000_000),
                (long)((end - start) / speed * 1_000_000)
            ))
            .source_timerange(new Timerange(
                (long)(start * 1_000_000),
                (long)((end - start) * 1_000_000)
            ))
            .speed(speed)
            .build();

        // 3. 添加到轨道
        addSegmentToTrack("video", segment);

        return this;
    }

    public DraftInfo build() {
        return draft;
    }
}
```

### 2. DraftTemplateManager

```java
public class DraftTemplateManager {
    private static final String TEMPLATE_PATH = "/templates/";

    public static DraftInfo loadTemplate(String templateName) {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = DraftTemplateManager.class
                .getResourceAsStream(TEMPLATE_PATH + templateName + ".json")) {
            return mapper.readValue(is, DraftInfo.class);
        }
    }

    public static void saveTemplate(DraftInfo draft, String outputPath) {
        ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(new File(outputPath), draft);
    }
}
```

### 3. TimeUtil

```java
public class TimeUtil {
    /**
     * 将秒转换为微秒
     */
    public static long secondsToMicroseconds(double seconds) {
        return (long)(seconds * 1_000_000);
    }

    /**
     * 解析时间字符串 "1.5s" -> 1500000
     */
    public static long parseTime(String timeStr) {
        if (timeStr.endsWith("s")) {
            double seconds = Double.parseDouble(
                timeStr.substring(0, timeStr.length() - 1)
            );
            return secondsToMicroseconds(seconds);
        }
        throw new IllegalArgumentException("Invalid time format: " + timeStr);
    }
}
```

## 实施路径

### 阶段 1：基础框架（1 周）

1. 创建数据模型类（POJO）
2. 准备 JSON 模板文件
3. 实现 DraftBuilder
4. 实现基础工具类

### 阶段 2：核心功能（2 周）

1. 实现 CreateDraftAction
2. 实现 AddVideoAction
3. 实现 AddAudioAction
4. 实现 AddImageAction
5. 实现 AddTextAction
6. 实现 SaveDraftAction

### 阶段 3：高级功能（2 周）

1. 转场效果
2. 蒙版
3. 特效
4. 关键帧动画
5. 字幕（SRT）

### 阶段 4：MCP 集成（1 周）

1. 创建 MCP Tool Factories
2. 注册到 HttpMcpConfig
3. 测试集成

## 关键技术点

### 1. JSON 模板管理

```
resources/
└── templates/
    ├── draft_template.json          # 基础模板
    ├── video_material_template.json # 视频素材模板
    ├── audio_material_template.json # 音频素材模板
    └── text_segment_template.json   # 文本片段模板
```

### 2. ID 生成策略

```java
public class IdGenerator {
    public static String generateMaterialId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String generateSegmentId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
```

### 3. 媒体元数据获取

使用 JavaCV 或调用 ffprobe：

```java
public class MediaMetadataExtractor {
    public static MediaInfo getVideoInfo(String url) {
        // 使用 ProcessBuilder 调用 ffprobe
        ProcessBuilder pb = new ProcessBuilder(
            "ffprobe",
            "-v", "error",
            "-select_streams", "v:0",
            "-show_entries", "stream=width,height,duration",
            "-of", "json",
            url
        );

        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());

        // 解析 JSON
        JsonNode info = mapper.readTree(output);
        return MediaInfo.fromJson(info);
    }
}
```

## 优势

1. **简单直接**：不需要复杂的库移植
2. **易于维护**：只需要维护 JSON 模板
3. **灵活性高**：可以快速适配剪映更新
4. **性能好**：纯 JSON 操作，无额外开销

## 下一步

基于此方案，开始实施 Autoclip MCP 服务开发。
