# Autoclip 自动化剪辑模块开发计划

## 1. 项目概述

### 1.1 功能定位

Autoclip 是一个基于 MCP 协议的自动化视频剪辑服务，为 AI Agents 提供精确的视频、音频、图片和文字素材的编辑控制能力。通过 HTTP API 和 MCP 协议接口，实现从素材到成品的全流程自动化剪辑。

### 1.2 核心能力

- **精确编辑控制**：对 AI 生成的素材（图片、音频、视频、文字）进行精确编辑
- **素材拼接**：支持原始 AI 输出的拼接和处理（如视频变速、图片镜像反转等）
- **视频生成**：根据素材自动生成视频
- **多协议支持**：同时提供 HTTP REST API 和 MCP 协议接口

### 1.3 技术特点

- 基于 Spring Boot 3.5.7 和 MCP Java SDK 0.17.0
- 使用 FFmpeg 进行底层媒体处理
- 模块化设计，易于扩展
- 支持 STDIO 和 HTTP 两种传输模式
- 支持剪映/CapCut 草稿格式导出（参考VectCutAPI实现）
- 支持高级文字效果（阴影、背景、描边、多样式文本）
- 支持视频/图片蒙版、转场、动画等高级功能

---

## 2. 技术架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────┐
│         MCP Client (Claude/AI Agent)        │
└─────────────────┬───────────────────────────┘
                  │ STDIO / HTTP+SSE
┌─────────────────▼───────────────────────────┐
│         MCP Server (Spring Boot)            │
│  ┌──────────────────────────────────────┐   │
│  │      Transport Layer                 │   │
│  │  - STDIO Handler                     │   │
│  │  - HTTP Streaming Handler            │   │
│  └──────────────┬───────────────────────┘   │
│                 │                            │
│  ┌──────────────▼───────────────────────┐   │
│  │      Autoclip Module                  │   │
│  │  ┌──────────────────────────────┐    │   │
│  │  │  Tool Layer (MCP Tools)      │    │   │
│  │  └──────────┬───────────────────┘    │   │
│  │             │                         │   │
│  │  ┌──────────▼───────────────────┐    │   │
│  │  │  Service Layer               │    │   │
│  │  │  - DraftService              │    │   │
│  │  │  - VideoEditService         │    │   │
│  │  │  - AudioService             │    │   │
│  │  │  - TextService              │    │   │
│  │  │  - ImageService             │    │   │
│  │  │  - EffectService            │    │   │
│  │  │  - TimelineService          │    │   │
│  │  │  - MediaAnalysisService     │    │   │
│  │  └──────────┬───────────────────┘    │   │
│  │             │                         │   │
│  │  ┌──────────▼───────────────────┐    │   │
│  │  │  Engine Layer                │    │   │
│  │  │  - FFmpegEngine              │    │   │
│  │  │  - TimelineEngine            │    │   │
│  │  │  - RenderEngine              │    │   │
│  │  └──────────┬───────────────────┘    │   │
│  └─────────────┼─────────────────────────┘   │
└─────────────────┼─────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│         External Services                    │
│  - FFmpeg (Media Processing)                 │
│  - File Storage (Local/OSS)                  │
└─────────────────────────────────────────────┘
```

### 2.2 模块结构

```
autoclip/
├── config/                          # 配置类
│   ├── AutoclipMcpConfig.java      # MCP配置（STDIO模式）
│   └── AutoclipHttpConfig.java     # HTTP配置
├── tools/                           # MCP工具工厂
│   ├── DraftToolFactory.java       # 草稿管理工具
│   ├── VideoEditToolFactory.java   # 视频编辑工具
│   ├── AudioToolFactory.java       # 音频处理工具
│   ├── TextToolFactory.java        # 文字字幕工具
│   ├── ImageToolFactory.java       # 图像处理工具
│   ├── EffectToolFactory.java      # 特效转场工具
│   ├── TimelineToolFactory.java    # 时间轴控制工具
│   └── MediaAnalysisToolFactory.java # 媒体分析工具
├── service/                         # 业务服务层
│   ├── DraftService.java           # 草稿管理服务
│   ├── VideoEditService.java       # 视频编辑服务
│   ├── AudioService.java           # 音频处理服务
│   ├── TextService.java            # 文字字幕服务
│   ├── ImageService.java           # 图像处理服务
│   ├── EffectService.java          # 特效转场服务
│   ├── TimelineService.java        # 时间轴服务
│   └── MediaAnalysisService.java   # 媒体分析服务
├── engine/                          # 引擎层
│   ├── FFmpegEngine.java           # FFmpeg引擎封装
│   ├── TimelineEngine.java         # 时间轴引擎
│   └── RenderEngine.java           # 渲染引擎
├── model/                           # 数据模型
│   ├── Draft.java                  # 草稿模型
│   ├── VideoClip.java              # 视频片段模型
│   ├── AudioTrack.java             # 音频轨道模型
│   ├── TextLayer.java              # 文字图层模型
│   ├── ImageLayer.java             # 图片图层模型
│   ├── Effect.java                 # 特效模型
│   ├── Transition.java             # 转场模型
│   ├── Keyframe.java               # 关键帧模型
│   └── Timeline.java               # 时间轴模型
├── util/                            # 工具类
│   ├── MediaFileUtil.java          # 媒体文件工具
│   ├── FFmpegUtil.java             # FFmpeg工具类
│   └── PathUtil.java               # 路径工具
└── controller/                      # HTTP REST API控制器
    └── AutoclipController.java     # REST API入口
```

---

## 3. 基于 pyJianYingDraft 的核心设计

### 3.0.1 草稿文件结构设计（参考 pyJianYingDraft）

基于对 `pyJianYingDraft` 源码的深入分析，Autoclip 的草稿系统将采用以下核心设计：

#### 3.0.1.1 草稿文件组成

剪映/CapCut 草稿文件由以下部分组成：

1. **draft_info.json**：草稿主文件，包含所有素材、轨道、片段信息
2. **draft_meta_info.json**：草稿元数据，包含草稿ID、名称、创建时间等
3. **assets/** 目录：存放所有媒体素材
   - `assets/audio/`：音频文件
   - `assets/video/`：视频文件
   - `assets/image/`：图片文件

#### 3.0.1.2 ScriptMaterial 素材管理系统

参考 `pyJianYingDraft` 的 `ScriptMaterial` 类设计，Autoclip 需要实现以下素材管理：

```java
public class ScriptMaterial {
    // 基础素材
    private List<AudioMaterial> audios;           // 音频素材列表
    private List<VideoMaterial> videos;           // 视频素材列表（包含图片）
    private List<StickerMaterial> stickers;        // 贴纸素材列表
    private List<TextMaterial> texts;              // 文本素材列表
    
    // 效果素材
    private List<AudioEffect> audioEffects;        // 音频特效列表
    private List<AudioFade> audioFades;            // 音频淡入淡出列表
    private List<SegmentAnimation> animations;     // 动画素材列表
    private List<VideoEffect> videoEffects;       // 视频特效列表
    private List<Speed> speeds;                   // 变速列表
    private List<Mask> masks;                     // 蒙版列表
    private List<Transition> transitions;          // 转场效果列表
    private List<Filter> filters;                 // 滤镜列表
    private List<BackgroundFilling> canvases;     // 背景填充列表
}
```

**关键设计要点**：
- 所有素材使用全局唯一的 `material_id`（UUID格式）
- 素材与片段分离：素材定义在 `ScriptMaterial` 中，片段引用素材ID
- 支持素材复用：多个片段可以引用同一个素材

#### 3.0.1.3 Track 和 Segment 层级结构

参考 `pyJianYingDraft` 的轨道系统设计：

```java
// 轨道类型枚举
public enum TrackType {
    VIDEO,      // 视频轨道
    AUDIO,      // 音频轨道
    EFFECT,     // 特效轨道
    FILTER,     // 滤镜轨道
    STICKER,    // 贴纸轨道
    TEXT        // 文字轨道
}

// 轨道基类
public abstract class BaseTrack {
    private String trackId;              // 轨道ID（UUID）
    private TrackType trackType;         // 轨道类型
    private int index;                   // 轨道索引（渲染顺序）
    private List<BaseSegment> segments;  // 片段列表
}

// 片段基类
public abstract class BaseSegment {
    private String segmentId;            // 片段ID（UUID）
    private String materialId;          // 关联的素材ID
    private Timerange targetTimerange;   // 目标时间范围（在时间轴上的位置）
    private KeyframeList commonKeyframes; // 通用关键帧（位置、缩放、旋转、透明度等）
}
```

**关键设计要点**：
- 轨道按类型分类，不同类型的轨道有不同的渲染顺序
- 片段通过 `material_id` 引用素材，实现素材与片段的解耦
- 时间使用微秒（microseconds）作为单位，确保精度
- 支持片段重叠检测和自动调整

#### 3.0.1.4 时间系统设计

参考 `pyJianYingDraft` 的 `Timerange` 和 `time_util` 设计：

```java
public class Timerange {
    private long start;    // 开始时间（微秒）
    private long duration; // 持续时间（微秒）
    
    public long getEnd() {
        return start + duration;
    }
}

// 时间工具类
public class TimeUtil {
    // 秒转微秒
    public static long secondsToMicroseconds(double seconds) {
        return (long)(seconds * 1_000_000);
    }
    
    // 微秒转秒
    public static double microsecondsToSeconds(long microseconds) {
        return microseconds / 1_000_000.0;
    }
    
    // 时间戳格式转换（用于SRT字幕）
    public static String formatTimestamp(long microseconds) {
        // 格式：00:00:00,000
    }
}
```

**关键设计要点**：
- 内部使用微秒作为时间单位，确保高精度
- API 接口使用秒作为单位，便于用户理解
- 支持时间范围（Timerange）的运算和比较

#### 3.0.1.5 关键帧系统设计

参考 `pyJianYingDraft` 的 `Keyframe` 和 `KeyframeList` 设计：

```java
// 关键帧属性枚举
public enum KeyframeProperty {
    POSITION,      // 位置（X, Y）
    ROTATION,      // 旋转
    SCALE,         // 缩放（X, Y）
    ALPHA,         // 透明度
    SATURATION,    // 饱和度
    CONTRAST,      // 对比度
    BRIGHTNESS,    // 亮度
    VOLUME         // 音量（音频）
}

// 关键帧
public class Keyframe {
    private String keyframeId;           // 关键帧ID（UUID）
    private KeyframeProperty property;    // 属性类型
    private long time;                    // 时间点（微秒，相对于片段开始时间）
    private Object value;                 // 属性值（根据属性类型不同而不同）
    private String easing;                // 缓动函数（如 "linear", "ease_in_out" 等）
}

// 关键帧列表
public class KeyframeList {
    private Map<KeyframeProperty, List<Keyframe>> keyframes;
    
    public void addKeyframe(KeyframeProperty property, long time, Object value) {
        // 添加关键帧，自动排序
    }
    
    public Object interpolate(KeyframeProperty property, long time) {
        // 根据关键帧插值计算属性值
    }
}
```

**关键设计要点**：
- 关键帧按属性类型分组管理
- 支持线性插值、缓动函数等动画效果
- 关键帧时间相对于片段开始时间，便于片段移动时保持动画

#### 3.0.1.6 草稿生成流程

参考 `pyJianYingDraft` 的 `ScriptFile.dump()` 和 `save_draft_impl` 实现：

**步骤1：创建草稿结构**
```java
// 1. 创建 ScriptFile 对象
ScriptFile script = new ScriptFile(width, height);

// 2. 生成草稿ID（格式：dfd_cat_{timestamp}_{uuid8}）
String draftId = generateDraftId();

// 3. 创建草稿目录结构
createDraftDirectory(draftId);
```

**步骤2：添加素材和片段**
```java
// 1. 添加素材到 ScriptMaterial
VideoMaterial video = script.materials.addVideo(videoPath);
AudioMaterial audio = script.materials.addAudio(audioPath);

// 2. 创建轨道
Track videoTrack = script.addTrack(TrackType.VIDEO, "main");

// 3. 添加片段到轨道
VideoSegment segment = new VideoSegment(video.materialId, timerange);
videoTrack.addSegment(segment);
```

**步骤3：保存草稿文件**
```java
// 1. 下载/复制媒体文件到 assets 目录
downloadMediaFiles(draftId, script.materials);

// 2. 更新素材路径（replace_path）
updateMaterialPaths(script.materials, draftId);

// 3. 导出 draft_info.json
script.dump(draftInfoPath);

// 4. 生成 draft_meta_info.json
generateDraftMetaInfo(draftId, draftMetaInfoPath);
```

**步骤4：打包和上传（可选）**
```java
// 1. 压缩草稿目录为 ZIP
String zipPath = zipDraft(draftId);

// 2. 上传到 OSS（可选）
String draftUrl = uploadToOSS(zipPath);
```

#### 3.0.1.7 草稿导出方式

参考 `pyJianYingDraft` 的 `JianyingController` 设计，Autoclip 支持两种导出方式：

**方式1：生成草稿文件（推荐）**
- 直接生成剪映/CapCut 可导入的草稿文件
- 用户手动在剪映/CapCut 中打开草稿进行最终导出
- 优点：不依赖 UI 自动化，更稳定
- 实现：按照草稿格式生成 JSON 和文件结构

**方式2：UI 自动化导出（可选）**
- 使用 UI 自动化工具（如 Windows 的 UIAutomation）控制剪映/CapCut 导出
- 优点：完全自动化，无需人工干预
- 缺点：依赖剪映/CapCut 版本，需要维护 UI 自动化代码
- 实现：参考 `JianyingController.export_draft()` 的实现

**推荐方案**：
- 优先实现方式1（生成草稿文件）
- 方式2作为可选功能，仅在 Windows 环境下且用户明确需要时启用

#### 3.0.1.8 模板模式支持

参考 `pyJianYingDraft` 的 `load_template` 和 `duplicate_as_template` 设计：

```java
public class DraftService {
    // 从模板创建草稿
    public String createDraftFromTemplate(String templatePath, int width, int height) {
        // 1. 加载模板草稿
        ScriptFile template = ScriptFile.load(templatePath);
        
        // 2. 创建新草稿
        ScriptFile newDraft = new ScriptFile(width, height);
        
        // 3. 复制模板的轨道和片段结构（不复制素材文件）
        copyTrackStructure(template, newDraft);
        
        return newDraft.draftId;
    }
    
    // 将草稿保存为模板
    public void saveAsTemplate(String draftId, String templatePath) {
        ScriptFile draft = loadDraft(draftId);
        draft.saveAsTemplate(templatePath);
    }
}
```

**关键设计要点**：
- 模板只保存轨道和片段结构，不包含实际媒体文件
- 使用模板时，需要替换素材路径
- 支持模板参数化（如分辨率、时长等）

#### 3.0.1.9 改进和优化建议

基于对 `pyJianYingDraft` 的分析，Autoclip 可以在以下方面进行改进：

1. **更完善的错误处理**：
   - 添加素材验证（文件存在性、格式支持等）
   - 片段重叠检测和自动调整
   - 轨道索引冲突检测

2. **性能优化**：
   - 素材缓存机制（避免重复下载）
   - 异步下载媒体文件
   - 批量操作优化

3. **扩展性设计**：
   - 插件化的特效系统
   - 可配置的草稿格式版本支持
   - 多平台导出支持（剪映、CapCut、其他编辑器）

4. **用户体验**：
   - 草稿预览功能（生成缩略图）
   - 草稿版本管理
   - 草稿导入/导出进度反馈

---

## 3. 功能模块详细设计

### 3.1 草稿管理 (Draft Management)

#### 功能描述
- 创建、保存、加载、删除草稿（基于 pyJianYingDraft 的 ScriptFile 设计）
- 草稿导出为剪映/CapCut 格式（生成完整的草稿文件结构）
- 草稿模板支持（从模板创建草稿）
- 草稿缓存管理（内存缓存，提高性能）

#### MCP工具设计

**工具1: createDraft**
- **描述**: 创建新的剪辑草稿（参考 pyJianYingDraft 的 ScriptFile 初始化）
- **参数**:
  - `width` (number, 可选): 视频宽度，默认1080
  - `height` (number, 可选): 视频高度，默认1920
  - `name` (string, 可选): 草稿名称
  - `template` (string, 可选): 模板路径或模板ID（参考 load_template）
- **返回**: 
  - `draftId` (string): 草稿ID（格式：dfd_cat_{timestamp}_{uuid8}）
  - `width` (number): 视频宽度
  - `height` (number): 视频高度
  - `createdAt` (string): 创建时间

**工具2: saveDraft**
- **描述**: 保存草稿为剪映/CapCut 格式（参考 pyJianYingDraft 的 save_draft_impl）
- **参数**:
  - `draftId` (string, 必填): 草稿ID
  - `outputFolder` (string, 可选): 输出文件夹路径，不提供则使用默认路径
  - `uploadToOSS` (boolean, 可选): 是否上传到OSS，默认false
  - `compress` (boolean, 可选): 是否压缩为ZIP，默认true
- **返回**: 
  - `success` (boolean): 是否成功
  - `draftPath` (string): 草稿文件夹路径
  - `draftUrl` (string, 可选): OSS下载URL（如果上传）
  - `zipPath` (string, 可选): ZIP文件路径（如果压缩）

**工具3: loadDraft**
- **描述**: 从剪映/CapCut 草稿文件加载草稿（参考 pyJianYingDraft 的 ScriptFile.load）
- **参数**:
  - `draftPath` (string, 必填): 草稿文件夹路径或ZIP文件路径
- **返回**: 
  - `draftId` (string): 草稿ID
  - `name` (string): 草稿名称
  - `width` (number): 视频宽度
  - `height` (number): 视频高度
  - `duration` (number): 视频时长（秒）

**工具4: listDrafts**
- **描述**: 列出所有草稿（从缓存和文件系统）
- **参数**:
  - `page` (number, 可选): 页码，默认1
  - `pageSize` (number, 可选): 每页数量，默认20
- **返回**: 
  - `drafts` (array): 草稿列表
    - `draftId` (string): 草稿ID
    - `name` (string): 草稿名称
    - `createdAt` (string): 创建时间
    - `updatedAt` (string): 更新时间
  - `total` (number): 总数量

**工具5: deleteDraft**
- **描述**: 删除草稿（从缓存和文件系统）
- **参数**:
  - `draftId` (string, 必填): 草稿ID
  - `deleteFiles` (boolean, 可选): 是否删除文件，默认true
- **返回**: 
  - `success` (boolean): 是否成功

**工具6: exportDraftToJianying**
- **描述**: 导出草稿为剪映/CapCut格式（参考 pyJianYingDraft 的 save_draft_impl，生成完整草稿结构）
- **参数**:
  - `draftId` (string, 必填): 草稿ID
  - `outputFolder` (string, 可选): 输出文件夹路径
  - `format` (string, 可选): 格式类型（"jianying" 或 "capcut"），默认"jianying"
  - `uploadToOSS` (boolean, 可选): 是否上传到OSS，默认false
- **返回**: 
  - `draftPath` (string): 草稿文件夹路径
  - `draftUrl` (string, 可选): OSS下载URL
  - `zipPath` (string, 可选): ZIP文件路径

**工具7: exportDraftWithUIAutomation**（可选，仅Windows）
- **描述**: 使用UI自动化导出视频（参考 pyJianYingDraft 的 JianyingController.export_draft）
- **参数**:
  - `draftId` (string, 必填): 草稿ID
  - `outputPath` (string, 可选): 导出视频路径
  - `resolution` (string, 可选): 分辨率（"1080P", "720P", "4K"等），默认"1080P"
  - `framerate` (string, 可选): 帧率（"30fps", "60fps"等），默认"30fps"
  - `timeout` (number, 可选): 超时时间（秒），默认1200
- **返回**: 
  - `success` (boolean): 是否成功
  - `videoPath` (string): 导出视频路径
- **注意**: 此功能需要剪映/CapCut 已安装并运行，且仅支持Windows平台

**工具8: loadTemplate**
- **描述**: 从模板创建草稿（参考 pyJianYingDraft 的 load_template）
- **参数**:
  - `templatePath` (string, 必填): 模板路径
  - `width` (number, 可选): 视频宽度，默认1080
  - `height` (number, 可选): 视频高度，默认1920
- **返回**: 
  - `draftId` (string): 新创建的草稿ID

#### 数据模型

```java
// 草稿主类（对应 pyJianYingDraft 的 ScriptFile）
public class ScriptFile {
    private String draftId;              // 草稿ID（格式：dfd_cat_{timestamp}_{uuid8}）
    private int width;                   // 视频宽度
    private int height;                  // 视频高度
    private ScriptMaterial materials;    // 素材管理
    private List<Track> tracks;          // 轨道列表
    private LocalDateTime createdAt;     // 创建时间
    private LocalDateTime updatedAt;     // 更新时间
    
    // 方法
    public void addMaterial(VideoMaterial material);
    public void addMaterial(AudioMaterial material);
    public Track addTrack(TrackType type, String name);
    public void addSegment(String trackId, BaseSegment segment);
    public void dump(String outputPath); // 导出 draft_info.json
    public static ScriptFile load(String draftPath); // 从文件加载
}

// 草稿元数据（对应 draft_meta_info.json）
public class DraftMetaInfo {
    private String draftId;
    private String draftName;
    private String draftType;
    private long tmDuration;              // 时长（微秒）
    private long tmDraftCloudModified;    // 云修改时间
    private List<DraftMaterialType> draftMaterials; // 素材类型列表
}
```

---

### 3.2 视频剪辑和合并 (Video Editing & Merging)

#### 功能描述
- 视频裁剪、分割、合并
- 视频变速（快放/慢放）
- 视频旋转、翻转
- 视频尺寸调整
- 视频格式转换

#### MCP工具设计

**工具1: cutVideo**
- **描述**: 裁剪视频片段
- **参数**:
  - `inputPath` (string, 必填): 输入视频路径
  - `startTime` (number, 必填): 开始时间（秒）
  - `duration` (number, 必填): 持续时间（秒）
  - `outputPath` (string, 可选): 输出路径
- **返回**: 输出文件路径

**工具2: splitVideo**
- **描述**: 分割视频为多个片段
- **参数**:
  - `inputPath` (string, 必填): 输入视频路径
  - `splitPoints` (array, 必填): 分割点数组（秒）
  - `outputDir` (string, 可选): 输出目录
- **返回**: 输出文件路径列表

**工具3: mergeVideos**
- **描述**: 合并多个视频
- **参数**:
  - `videoPaths` (array, 必填): 视频路径数组
  - `outputPath` (string, 必填): 输出路径
  - `transition` (object, 可选): 转场效果
- **返回**: 输出文件路径

**工具4: changeVideoSpeed**
- **描述**: 改变视频播放速度
- **参数**:
  - `inputPath` (string, 必填): 输入视频路径
  - `speed` (number, 必填): 速度倍数（0.5-2.0）
  - `outputPath` (string, 可选): 输出路径
- **返回**: 输出文件路径

**工具5: rotateVideo**
- **描述**: 旋转视频
- **参数**:
  - `inputPath` (string, 必填): 输入视频路径
  - `angle` (number, 必填): 旋转角度（90, 180, 270）
  - `outputPath` (string, 可选): 输出路径
- **返回**: 输出文件路径

**工具6: flipVideo**
- **描述**: 翻转视频（水平/垂直）
- **参数**:
  - `inputPath` (string, 必填): 输入视频路径
  - `direction` (string, 必填): "horizontal" 或 "vertical"
  - `outputPath` (string, 可选): 输出路径
- **返回**: 输出文件路径

**工具7: resizeVideo**
- **描述**: 调整视频尺寸
- **参数**:
  - `inputPath` (string, 必填): 输入视频路径
  - `width` (number, 必填): 目标宽度
  - `height` (number, 必填): 目标高度
  - `outputPath` (string, 可选): 输出路径
- **返回**: 输出文件路径

**工具8: convertVideoFormat**
- **描述**: 转换视频格式
- **参数**:
  - `inputPath` (string, 必填): 输入视频路径
  - `format` (string, 必填): 目标格式（mp4, avi, mov等）
  - `outputPath` (string, 可选): 输出路径
- **返回**: 输出文件路径

**工具9: addVideoToDraft**
- **描述**: 添加视频到草稿（参考VectCutAPI的add_video实现）
- **参数**:
  - `draftId` (string, 可选): 草稿ID，不提供则创建新草稿
  - `videoUrl` (string, 必填): 视频URL或本地路径
  - `start` (number, 可选): 源视频开始时间（秒），默认0
  - `end` (number, 可选): 源视频结束时间（秒）
  - `targetStart` (number, 可选): 目标时间轴开始时间（秒），默认0
  - `width` (number, 可选): 视频宽度，默认1080
  - `height` (number, 可选): 视频高度，默认1920
  - `transformX` (number, 可选): X轴位置偏移，默认0
  - `transformY` (number, 可选): Y轴位置偏移，默认0
  - `scaleX` (number, 可选): X轴缩放，默认1.0
  - `scaleY` (number, 可选): Y轴缩放，默认1.0
  - `speed` (number, 可选): 播放速度倍数，默认1.0
  - `volume` (number, 可选): 音量（0-1），默认1.0
  - `trackName` (string, 可选): 轨道名称，默认"main"
  - `transition` (string, 可选): 转场类型（如"Dissolve", "Fade"等）
  - `transitionDuration` (number, 可选): 转场时长（秒），默认0.5
  - `maskType` (string, 可选): 蒙版类型（"Circle", "Rectangle", "Linear", "Mirror", "Heart", "Star"等）
  - `maskCenterX` (number, 可选): 蒙版中心X坐标（0-1），默认0.5
  - `maskCenterY` (number, 可选): 蒙版中心Y坐标（0-1），默认0.5
  - `maskSize` (number, 可选): 蒙版大小（0-1），默认1.0
  - `maskRotation` (number, 可选): 蒙版旋转角度，默认0.0
  - `maskFeather` (number, 可选): 蒙版羽化程度（0-1），默认0.0
  - `maskInvert` (boolean, 可选): 是否反转蒙版，默认false
  - `maskRectWidth` (number, 可选): 矩形蒙版宽度（仅矩形蒙版）
  - `maskRoundCorner` (number, 可选): 矩形蒙版圆角（0-100，仅矩形蒙版）
  - `backgroundBlur` (number, 可选): 背景模糊级别（1-4），1=轻微，2=中等，3=强烈，4=最大
- **返回**: 草稿ID和更新结果

---

### 3.3 音频处理和混音 (Audio Processing & Mixing)

#### 功能描述
- 音频提取、裁剪、合并
- 音频变速、变调
- 音频混音
- 音频降噪、均衡器
- 音频格式转换

#### MCP工具设计

**工具1: extractAudio**
- **描述**: 从视频中提取音频
- **参数**:
  - `videoPath` (string, 必填): 视频路径
  - `outputPath` (string, 可选): 输出音频路径
  - `format` (string, 可选): 音频格式（mp3, wav等），默认mp3
- **返回**: 输出文件路径

**工具2: cutAudio**
- **描述**: 裁剪音频片段
- **参数**:
  - `inputPath` (string, 必填): 输入音频路径
  - `startTime` (number, 必填): 开始时间（秒）
  - `duration` (number, 必填): 持续时间（秒）
  - `outputPath` (string, 可选): 输出路径
- **返回**: 输出文件路径

**工具3: mergeAudios**
- **描述**: 合并多个音频
- **参数**:
  - `audioPaths` (array, 必填): 音频路径数组
  - `outputPath` (string, 必填): 输出路径
  - `fadeIn` (number, 可选): 淡入时长（秒）
  - `fadeOut` (number, 可选): 淡出时长（秒）
- **返回**: 输出文件路径

**工具4: changeAudioSpeed**
- **描述**: 改变音频播放速度
- **参数**:
  - `inputPath` (string, 必填): 输入音频路径
  - `speed` (number, 必填): 速度倍数（0.5-2.0）
  - `outputPath` (string, 可选): 输出路径
- **返回**: 输出文件路径

**工具5: changeAudioPitch**
- **描述**: 改变音频音调
- **参数**:
  - `inputPath` (string, 必填): 输入音频路径
  - `pitch` (number, 必填): 音调变化（-12到+12半音）
  - `outputPath` (string, 可选): 输出路径
- **返回**: 输出文件路径

**工具6: mixAudios**
- **描述**: 混音（多个音频叠加）
- **参数**:
  - `audioTracks` (array, 必填): 音频轨道数组
    - `path` (string): 音频路径
    - `volume` (number): 音量（0-1）
    - `startTime` (number): 开始时间（秒）
  - `outputPath` (string, 必填): 输出路径
- **返回**: 输出文件路径

**工具7: addAudioToVideo**
- **描述**: 为视频添加/替换音频
- **参数**:
  - `videoPath` (string, 必填): 视频路径
  - `audioPath` (string, 必填): 音频路径
  - `outputPath` (string, 必填): 输出路径
  - `replace` (boolean, 可选): 是否替换原音频，默认false
  - `volume` (number, 可选): 音频音量（0-1），默认1.0
- **返回**: 输出文件路径

**工具8: normalizeAudio**
- **描述**: 音频标准化（音量归一化）
- **参数**:
  - `inputPath` (string, 必填): 输入音频路径
  - `outputPath` (string, 可选): 输出路径
- **返回**: 输出文件路径

**工具9: addAudioToDraft**
- **描述**: 添加音频到草稿（参考VectCutAPI的add_audio实现）
- **参数**:
  - `draftId` (string, 可选): 草稿ID，不提供则创建新草稿
  - `audioUrl` (string, 必填): 音频URL或本地路径
  - `start` (number, 可选): 源音频开始时间（秒），默认0
  - `end` (number, 可选): 源音频结束时间（秒）
  - `targetStart` (number, 可选): 目标时间轴开始时间（秒），默认0
  - `volume` (number, 可选): 音量（0-1），默认1.0
  - `speed` (number, 可选): 播放速度倍数，默认1.0
  - `trackName` (string, 可选): 轨道名称，默认"audio_main"
  - `effectType` (string, 可选): 音频特效类型（如"Tremble", "Big_House"等）
  - `effectParams` (array, 可选): 音频特效参数数组
  - `width` (number, 可选): 视频宽度，默认1080
  - `height` (number, 可选): 视频高度，默认1920
- **返回**: 草稿ID和更新结果

---

### 3.4 文字和字幕添加 (Text & Subtitle)

#### 功能描述
- 添加文字图层
- 字幕生成和管理
- 文字样式设置（字体、颜色、大小、位置）
- 文字动画效果

#### MCP工具设计

**工具1: addTextLayer**
- **描述**: 添加文字图层到时间轴（参考VectCutAPI的add_text实现，支持高级文字效果）
- **参数**:
  - `draftId` (string, 可选): 草稿ID，不提供则创建新草稿
  - `text` (string, 必填): 文字内容
  - `startTime` (number, 必填): 开始时间（秒）
  - `endTime` (number, 必填): 结束时间（秒）
  - `font` (string, 可选): 字体名称（如"思源中宋", "挥墨体"等）
  - `fontColor` (string, 可选): 字体颜色（hex格式），默认"#ffffff"
  - `fontSize` (number, 可选): 字体大小，默认24
  - `fontAlpha` (number, 可选): 字体透明度（0-1），默认1.0
  - `transformX` (number, 可选): X轴位置（-1到1，0为居中），默认0
  - `transformY` (number, 可选): Y轴位置（-1到1，-0.8为底部），默认-0.8
  - `trackName` (string, 可选): 轨道名称，默认"text_main"
  - `vertical` (boolean, 可选): 是否竖排显示，默认false
  - `width` (number, 可选): 视频宽度，默认1080
  - `height` (number, 可选): 视频高度，默认1920
  - **文字描边参数**:
    - `borderColor` (string, 可选): 描边颜色（hex格式）
    - `borderWidth` (number, 可选): 描边宽度，默认0.0（无描边）
    - `borderAlpha` (number, 可选): 描边透明度（0-1），默认1.0
  - **文字背景参数**:
    - `backgroundColor` (string, 可选): 背景颜色（hex格式）
    - `backgroundAlpha` (number, 可选): 背景透明度（0-1），默认0.0（无背景）
    - `backgroundStyle` (number, 可选): 背景样式（0=气泡，1=矩形等），默认1
    - `backgroundRoundRadius` (number, 可选): 背景圆角半径（0-1），默认0.0
    - `backgroundHeight` (number, 可选): 背景高度比例（0-1），默认0.14
    - `backgroundWidth` (number, 可选): 背景宽度比例（0-1），默认0.14
    - `backgroundHorizontalOffset` (number, 可选): 背景水平偏移（0-1），默认0.5
    - `backgroundVerticalOffset` (number, 可选): 背景垂直偏移（0-1），默认0.5
  - **文字阴影参数**:
    - `shadowEnabled` (boolean, 可选): 是否启用阴影，默认false
    - `shadowColor` (string, 可选): 阴影颜色（hex格式），默认"#000000"
    - `shadowAlpha` (number, 可选): 阴影透明度（0-1），默认0.9
    - `shadowAngle` (number, 可选): 阴影角度（-180到180度），默认-45.0
    - `shadowDistance` (number, 可选): 阴影距离，默认5.0
    - `shadowSmoothing` (number, 可选): 阴影平滑度（0-1），默认0.15
  - **文字动画参数**:
    - `introAnimation` (string, 可选): 入场动画类型（如"弹入", "Blur", "Fade_In"等）
    - `introDuration` (number, 可选): 入场动画时长（秒），默认0.5
    - `outroAnimation` (string, 可选): 出场动画类型（如"晕开", "Fade_Out", "Throw_Back"等）
    - `outroDuration` (number, 可选): 出场动画时长（秒），默认0.5
  - **多样式文本参数**:
    - `textStyles` (array, 可选): 文本多样式配置数组
      - `start` (number): 样式开始字符位置
      - `end` (number): 样式结束字符位置
      - `fontColor` (string, 可选): 该段文字颜色
      - `fontSize` (number, 可选): 该段文字大小
      - `bold` (boolean, 可选): 是否粗体
      - `italic` (boolean, 可选): 是否斜体
      - `underline` (boolean, 可选): 是否下划线
      - `border` (object, 可选): 该段文字描边配置
        - `color` (string): 描边颜色
        - `width` (number): 描边宽度
        - `alpha` (number): 描边透明度
      - `font` (string, 可选): 该段文字字体
  - **其他参数**:
    - `fixedWidth` (number, 可选): 文字固定宽度比例（0-1），默认-1（不固定）
    - `fixedHeight` (number, 可选): 文字固定高度比例（0-1），默认-1（不固定）
    - `bubbleEffectId` (string, 可选): 气泡特效ID
    - `bubbleResourceId` (string, 可选): 气泡资源ID
    - `effectEffectId` (string, 可选): 文字特效ID
- **返回**: 草稿ID和图层ID

**工具2: addSubtitle**
- **描述**: 添加字幕（参考VectCutAPI的add_subtitle实现，支持SRT文件）
- **参数**:
  - `draftId` (string, 可选): 草稿ID，不提供则创建新草稿
  - `srtPath` (string, 必填): SRT字幕文件路径或URL
  - `trackName` (string, 可选): 轨道名称，默认"subtitle"
  - `timeOffset` (number, 可选): 时间偏移（秒），默认0
  - `font` (string, 可选): 字体名称
  - `fontSize` (number, 可选): 字体大小，默认8.0
  - `fontColor` (string, 可选): 字体颜色（hex格式），默认"#FFFFFF"
  - `bold` (boolean, 可选): 是否粗体，默认false
  - `italic` (boolean, 可选): 是否斜体，默认false
  - `underline` (boolean, 可选): 是否下划线，默认false
  - `borderWidth` (number, 可选): 边框宽度，默认0.0
  - `borderColor` (string, 可选): 边框颜色（hex格式），默认"#000000"
  - `backgroundColor` (string, 可选): 背景颜色（hex格式），默认"#000000"
  - `backgroundAlpha` (number, 可选): 背景透明度（0-1），默认0.0
  - `transformX` (number, 可选): X轴位置，默认0.0
  - `transformY` (number, 可选): Y轴位置，默认-0.8（底部）
  - `width` (number, 可选): 视频宽度，默认1080
  - `height` (number, 可选): 视频高度，默认1920
- **返回**: 草稿ID和字幕ID列表

**工具3: generateSubtitleFromAudio**
- **描述**: 从音频自动生成字幕（需要语音识别）
- **参数**:
  - `audioPath` (string, 必填): 音频路径
  - `language` (string, 可选): 语言代码，默认"zh-CN"
  - `outputPath` (string, 可选): 字幕文件输出路径
- **返回**: 字幕文件路径

**工具4: updateTextLayer**
- **描述**: 更新文字图层
- **参数**:
  - `draftId` (string, 必填): 草稿ID
  - `layerId` (string, 必填): 图层ID
  - `text` (string, 可选): 新文字内容
  - `style` (object, 可选): 新样式
  - `startTime` (number, 可选): 新开始时间
  - `duration` (number, 可选): 新持续时间
- **返回**: 更新结果

**工具5: removeTextLayer**
- **描述**: 删除文字图层
- **参数**:
  - `draftId` (string, 必填): 草稿ID
  - `layerId` (string, 必填): 图层ID
- **返回**: 删除结果

---

### 3.5 图像处理 (Image Processing)

#### 功能描述
- 图片导入和转换
- 图片动画（缩放、移动、旋转）
- 图片蒙版和遮罩
- 图片滤镜效果
- 图片合成

#### MCP工具设计

**工具1: importImage**
- **描述**: 导入图片到项目
- **参数**:
  - `imagePath` (string, 必填): 图片路径（本地或URL）
  - `draftId` (string, 可选): 关联草稿ID
- **返回**: 图片ID和路径

**工具2: addImageLayer**
- **描述**: 添加图片图层到时间轴（参考VectCutAPI的add_image实现，支持蒙版、转场、动画）
- **参数**:
  - `draftId` (string, 可选): 草稿ID，不提供则创建新草稿
  - `imageUrl` (string, 必填): 图片URL或本地路径
  - `startTime` (number, 可选): 开始时间（秒），默认0
  - `endTime` (number, 可选): 结束时间（秒），默认3.0
  - `width` (number, 可选): 视频宽度，默认1080
  - `height` (number, 可选): 视频高度，默认1920
  - `transformX` (number, 可选): X轴位置偏移，默认0
  - `transformY` (number, 可选): Y轴位置偏移，默认0
  - `scaleX` (number, 可选): X轴缩放，默认1.0
  - `scaleY` (number, 可选): Y轴缩放，默认1.0
  - `trackName` (string, 可选): 轨道名称，默认"main"
  - **转场参数**:
    - `transition` (string, 可选): 转场类型（如"Dissolve", "Fade"等）
    - `transitionDuration` (number, 可选): 转场时长（秒），默认0.5
  - **蒙版参数**:
    - `maskType` (string, 可选): 蒙版类型（"Circle", "Rectangle", "Linear", "Mirror", "Heart", "Star"等）
    - `maskCenterX` (number, 可选): 蒙版中心X坐标（0-1），默认0.0
    - `maskCenterY` (number, 可选): 蒙版中心Y坐标（0-1），默认0.0
    - `maskSize` (number, 可选): 蒙版大小（0-1），默认0.5
    - `maskRotation` (number, 可选): 蒙版旋转角度，默认0.0
    - `maskFeather` (number, 可选): 蒙版羽化程度（0-1），默认0.0
    - `maskInvert` (boolean, 可选): 是否反转蒙版，默认false
    - `maskRectWidth` (number, 可选): 矩形蒙版宽度（仅矩形蒙版）
    - `maskRoundCorner` (number, 可选): 矩形蒙版圆角（0-100，仅矩形蒙版）
  - **动画参数**:
    - `introAnimation` (string, 可选): 入场动画类型
    - `outroAnimation` (string, 可选): 出场动画类型
  - **背景模糊参数**:
    - `backgroundBlur` (number, 可选): 背景模糊级别（1-4）
- **返回**: 草稿ID和图层ID

**工具3: applyImageFilter**
- **描述**: 应用图片滤镜
- **参数**:
  - `imagePath` (string, 必填): 图片路径
  - `filter` (string, 必填): 滤镜类型（blur, brightness, contrast, saturation等）
  - `intensity` (number, 可选): 强度（0-1），默认0.5
  - `outputPath` (string, 可选): 输出路径
- **返回**: 输出文件路径

**工具4: flipImage**
- **描述**: 翻转图片
- **参数**:
  - `imagePath` (string, 必填): 图片路径
  - `direction` (string, 必填): "horizontal" 或 "vertical"
  - `outputPath` (string, 可选): 输出路径
- **返回**: 输出文件路径

**工具5: rotateImage**
- **描述**: 旋转图片
- **参数**:
  - `imagePath` (string, 必填): 图片路径
  - `angle` (number, 必填): 旋转角度
  - `outputPath` (string, 可选): 输出路径
- **返回**: 输出文件路径

**工具6: resizeImage**
- **描述**: 调整图片尺寸
- **参数**:
  - `imagePath` (string, 必填): 图片路径
  - `width` (number, 可选): 目标宽度
  - `height` (number, 可选): 目标高度
  - `keepAspectRatio` (boolean, 可选): 保持宽高比，默认true
  - `outputPath` (string, 可选): 输出路径
- **返回**: 输出文件路径

**工具7: addImageMask**
- **描述**: 添加图片蒙版
- **参数**:
  - `imagePath` (string, 必填): 图片路径
  - `maskPath` (string, 必填): 蒙版图片路径
  - `outputPath` (string, 可选): 输出路径
- **返回**: 输出文件路径

**工具8: animateImage**
- **描述**: 为图片添加动画效果
- **参数**:
  - `draftId` (string, 必填): 草稿ID
  - `layerId` (string, 必填): 图层ID
  - `animation` (object, 必填): 动画配置
    - `type` (string): 动画类型（zoom, pan, rotate等）
    - `keyframes` (array): 关键帧数组
- **返回**: 动画ID

**工具9: compositeImages**
- **描述**: 合成多个图片
- **参数**:
  - `images` (array, 必填): 图片配置数组
    - `path` (string): 图片路径
    - `position` (object): 位置 {x, y}
    - `size` (object): 尺寸 {width, height}
    - `opacity` (number): 透明度
  - `outputPath` (string, 必填): 输出路径
  - `canvasSize` (object, 必填): 画布尺寸 {width, height}
- **返回**: 输出文件路径

---

### 3.6 特效和转场效果 (Effects & Transitions)

#### 功能描述
- 视频特效（模糊、锐化、色彩调整等）
- 转场效果（淡入淡出、滑动、缩放等）
- 特效参数调整
- 特效时间控制

#### MCP工具设计

**工具1: addVideoEffect**
- **描述**: 添加视频特效（参考VectCutAPI的add_effect实现）
- **参数**:
  - `draftId` (string, 可选): 草稿ID，不提供则创建新草稿
  - `effectType` (string, 必填): 特效类型名称（如"Gold_Sparkles", "Like"等）
  - `startTime` (number, 可选): 开始时间（秒），默认0
  - `endTime` (number, 可选): 结束时间（秒），默认3.0
  - `trackName` (string, 可选): 轨道名称，默认"effect_01"
  - `params` (array, 可选): 特效参数数组（根据特效类型不同而不同）
  - `effectCategory` (string, 可选): 特效分类（如"character"角色特效等）
  - `width` (number, 可选): 视频宽度，默认1080
  - `height` (number, 可选): 视频高度，默认1920
- **返回**: 草稿ID和特效ID

**工具2: addTransition**
- **描述**: 添加转场效果（支持在视频/图片添加时直接指定转场）
- **参数**:
  - `draftId` (string, 必填): 草稿ID
  - `fromClipId` (string, 必填): 前一个片段ID
  - `toClipId` (string, 必填): 后一个片段ID
  - `transition` (object, 必填): 转场配置
    - `type` (string): 转场类型（"Dissolve", "Fade", "Slide", "Zoom"等）
    - `duration` (number): 转场时长（秒），默认0.5
    - `params` (object, 可选): 转场参数
- **返回**: 转场ID

**注意**: 转场也可以在添加视频/图片时通过`transition`和`transitionDuration`参数直接指定，这样更便捷。

**工具3: updateEffect**
- **描述**: 更新特效参数
- **参数**:
  - `draftId` (string, 必填): 草稿ID
  - `effectId` (string, 必填): 特效ID
  - `params` (object, 可选): 新参数
  - `startTime` (number, 可选): 新开始时间
  - `duration` (number, 可选): 新持续时间
- **返回**: 更新结果

**工具4: removeEffect**
- **描述**: 删除特效
- **参数**:
  - `draftId` (string, 必填): 草稿ID
  - `effectId` (string, 必填): 特效ID
- **返回**: 删除结果

**工具5: listAvailableEffects**
- **描述**: 列出所有可用的特效类型
- **参数**: 无
- **返回**: 特效类型列表

**工具6: listAvailableTransitions**
- **描述**: 列出所有可用的转场类型
- **参数**: 无
- **返回**: 转场类型列表

---

### 3.7 贴纸和装饰元素 (Stickers & Decorations)

#### 功能描述
- 添加贴纸
- 贴纸动画
- 装饰元素管理

#### MCP工具设计

**工具1: addSticker**
- **描述**: 添加贴纸到时间轴（参考VectCutAPI的add_sticker实现）
- **参数**:
  - `draftId` (string, 可选): 草稿ID，不提供则创建新草稿
  - `resourceId` (string, 必填): 贴纸资源ID（剪映/CapCut内置贴纸ID）
  - `startTime` (number, 必填): 开始时间（秒）
  - `endTime` (number, 必填): 结束时间（秒）
  - `transformX` (number, 可选): X轴位置偏移，默认0
  - `transformY` (number, 可选): Y轴位置偏移，默认0
  - `scaleX` (number, 可选): X轴缩放，默认1.0
  - `scaleY` (number, 可选): Y轴缩放，默认1.0
  - `alpha` (number, 可选): 透明度（0-1），默认1.0
  - `rotation` (number, 可选): 旋转角度，默认0.0
  - `flipHorizontal` (boolean, 可选): 水平翻转，默认false
  - `flipVertical` (boolean, 可选): 垂直翻转，默认false
  - `trackName` (string, 可选): 轨道名称，默认"sticker_main"
  - `relativeIndex` (number, 可选): 相对索引，默认0
  - `width` (number, 可选): 视频宽度，默认1080
  - `height` (number, 可选): 视频高度，默认1920
- **返回**: 草稿ID和贴纸ID

**工具2: animateSticker**
- **描述**: 为贴纸添加动画
- **参数**:
  - `draftId` (string, 必填): 草稿ID
  - `stickerId` (string, 必填): 贴纸ID
  - `animation` (object, 必填): 动画配置
    - `type` (string): 动画类型（bounce, rotate, scale等）
    - `keyframes` (array): 关键帧数组
- **返回**: 动画ID

**工具3: updateSticker**
- **描述**: 更新贴纸属性
- **参数**:
  - `draftId` (string, 必填): 草稿ID
  - `stickerId` (string, 必填): 贴纸ID
  - `position` (object, 可选): 新位置
  - `size` (object, 可选): 新尺寸
  - `rotation` (number, 可选): 新旋转角度
  - `opacity` (number, 可选): 新透明度
- **返回**: 更新结果

**工具4: removeSticker**
- **描述**: 删除贴纸
- **参数**:
  - `draftId` (string, 必填): 草稿ID
  - `stickerId` (string, 必填): 贴纸ID
- **返回**: 删除结果

---

### 3.8 批量处理和自动化 (Batch Processing & Automation)

#### 功能描述
- 批量处理多个文件
- 模板应用
- 自动化工作流

#### MCP工具设计

**工具1: batchProcess**
- **描述**: 批量处理媒体文件
- **参数**:
  - `files` (array, 必填): 文件路径数组
  - `operation` (string, 必填): 操作类型（resize, convert, addEffect等）
  - `params` (object, 必填): 操作参数
  - `outputDir` (string, 可选): 输出目录
- **返回**: 处理结果列表

**工具2: applyTemplate**
- **描述**: 应用模板到草稿
- **参数**:
  - `draftId` (string, 必填): 草稿ID
  - `templateId` (string, 必填): 模板ID
  - `replaceMedia` (object, 可选): 替换的媒体文件映射
- **返回**: 应用结果

**工具3: createTemplate**
- **描述**: 从草稿创建模板
- **参数**:
  - `draftId` (string, 必填): 草稿ID
  - `templateName` (string, 必填): 模板名称
  - `description` (string, 可选): 模板描述
- **返回**: 模板ID

**工具4: generateVideoFromAssets**
- **描述**: 根据素材自动生成视频
- **参数**:
  - `assets` (object, 必填): 素材配置
    - `images` (array, 可选): 图片数组
    - `videos` (array, 可选): 视频数组
    - `audio` (string, 可选): 背景音乐路径
    - `text` (array, 可选): 文字配置数组
  - `outputPath` (string, 必填): 输出路径
  - `duration` (number, 可选): 视频总时长（秒）
  - `transition` (string, 可选): 默认转场类型
- **返回**: 输出文件路径

---

### 3.9 关键帧、属性动画、时间轴控制 (Keyframes & Timeline)

#### 功能描述
- 关键帧管理
- 属性动画（位置、大小、透明度等）
- 时间轴精确控制

#### MCP工具设计

**工具1: addKeyframe**
- **描述**: 添加关键帧（支持单个和批量模式，参考VectCutAPI的add_video_keyframe实现）
- **参数**:
  - `draftId` (string, 必填): 草稿ID
  - `trackName` (string, 可选): 轨道名称，默认"main"
  - **单个关键帧模式**:
    - `propertyType` (string, 可选): 属性类型（position_x, position_y, rotation, scale_x, scale_y, uniform_scale, alpha, saturation, contrast, brightness, volume等）
    - `time` (number, 可选): 时间点（秒），默认0.0
    - `value` (string, 可选): 属性值（字符串格式）
  - **批量关键帧模式**:
    - `propertyTypes` (array, 可选): 属性类型数组
    - `times` (array, 可选): 时间点数组（秒）
    - `values` (array, 可选): 属性值数组（字符串格式）
- **返回**: 关键帧ID或关键帧ID数组

**工具2: updateKeyframe**
- **描述**: 更新关键帧
- **参数**:
  - `draftId` (string, 必填): 草稿ID
  - `keyframeId` (string, 必填): 关键帧ID
  - `time` (number, 可选): 新时间点
  - `value` (object, 可选): 新属性值
- **返回**: 更新结果

**工具3: removeKeyframe**
- **描述**: 删除关键帧
- **参数**:
  - `draftId` (string, 必填): 草稿ID
  - `keyframeId` (string, 必填): 关键帧ID
- **返回**: 删除结果

**工具4: setAnimationEasing**
- **描述**: 设置动画缓动函数
- **参数**:
  - `draftId` (string, 必填): 草稿ID
  - `layerId` (string, 必填): 图层ID
  - `property` (string, 必填): 属性名称
  - `easing` (string, 必填): 缓动类型（linear, easeIn, easeOut等）
- **返回**: 设置结果

**工具5: getTimeline**
- **描述**: 获取时间轴数据
- **参数**:
  - `draftId` (string, 必填): 草稿ID
- **返回**: 时间轴完整数据

**工具6: updateTimeline**
- **描述**: 更新时间轴
- **参数**:
  - `draftId` (string, 必填): 草稿ID
  - `timeline` (object, 必填): 时间轴数据
- **返回**: 更新结果

**工具7: seekTimeline**
- **描述**: 时间轴定位（预览用）
- **参数**:
  - `draftId` (string, 必填): 草稿ID
  - `time` (number, 必填): 时间点（秒）
- **返回**: 定位结果

---

### 3.10 媒体分析 (Media Analysis)

#### 功能描述
- 视频信息检测（时长、分辨率、格式等）
- 音频信息检测
- 图片信息检测
- 媒体文件验证

#### MCP工具设计

**工具1: analyzeVideo**
- **描述**: 分析视频信息
- **参数**:
  - `videoPath` (string, 必填): 视频路径
- **返回**: 视频信息对象
  - `duration` (number): 时长（秒）
  - `width` (number): 宽度
  - `height` (number): 高度
  - `format` (string): 格式
  - `codec` (string): 编码格式
  - `bitrate` (number): 比特率
  - `fps` (number): 帧率
  - `size` (number): 文件大小（字节）

**工具2: analyzeAudio**
- **描述**: 分析音频信息
- **参数**:
  - `audioPath` (string, 必填): 音频路径
- **返回**: 音频信息对象
  - `duration` (number): 时长（秒）
  - `format` (string): 格式
  - `codec` (string): 编码格式
  - `sampleRate` (number): 采样率
  - `channels` (number): 声道数
  - `bitrate` (number): 比特率
  - `size` (number): 文件大小（字节）

**工具3: analyzeImage**
- **描述**: 分析图片信息
- **参数**:
  - `imagePath` (string, 必填): 图片路径
- **返回**: 图片信息对象
  - `width` (number): 宽度
  - `height` (number): 高度
  - `format` (string): 格式
  - `size` (number): 文件大小（字节）
  - `colorSpace` (string): 色彩空间

**工具4: validateMediaFile**
- **描述**: 验证媒体文件是否有效
- **参数**:
  - `filePath` (string, 必填): 文件路径
- **返回**: 验证结果和错误信息（如有）

**工具5: getVideoThumbnail**
- **描述**: 获取视频缩略图
- **参数**:
  - `videoPath` (string, 必填): 视频路径
  - `time` (number, 可选): 时间点（秒），默认0
  - `outputPath` (string, 可选): 输出路径
- **返回**: 缩略图路径

---

## 4. HTTP REST API 设计

### 4.1 API 基础路径

```
/api/autoclip/v1
```

### 4.2 主要端点

#### 草稿管理
- `POST /drafts` - 创建草稿
- `GET /drafts` - 列出草稿
- `GET /drafts/{draftId}` - 获取草稿
- `PUT /drafts/{draftId}` - 更新草稿
- `DELETE /drafts/{draftId}` - 删除草稿
- `POST /drafts/{draftId}/export` - 导出草稿
- `POST /drafts/import` - 导入草稿

#### 视频编辑
- `POST /video/cut` - 裁剪视频
- `POST /video/split` - 分割视频
- `POST /video/merge` - 合并视频
- `POST /video/speed` - 改变速度
- `POST /video/rotate` - 旋转视频
- `POST /video/flip` - 翻转视频
- `POST /video/resize` - 调整尺寸
- `POST /video/convert` - 格式转换

#### 音频处理
- `POST /audio/extract` - 提取音频
- `POST /audio/cut` - 裁剪音频
- `POST /audio/merge` - 合并音频
- `POST /audio/speed` - 改变速度
- `POST /audio/mix` - 混音
- `POST /audio/add-to-video` - 添加音频到视频

#### 文字和字幕
- `POST /text/layer` - 添加文字图层
- `POST /text/subtitle` - 添加字幕
- `PUT /text/layer/{layerId}` - 更新文字图层
- `DELETE /text/layer/{layerId}` - 删除文字图层

#### 图像处理
- `POST /image/import` - 导入图片
- `POST /image/layer` - 添加图片图层
- `POST /image/filter` - 应用滤镜
- `POST /image/flip` - 翻转图片
- `POST /image/resize` - 调整尺寸

#### 特效和转场
- `POST /effect/video` - 添加视频特效
- `POST /effect/transition` - 添加转场
- `PUT /effect/{effectId}` - 更新特效
- `DELETE /effect/{effectId}` - 删除特效

#### 时间轴和关键帧
- `GET /timeline/{draftId}` - 获取时间轴
- `PUT /timeline/{draftId}` - 更新时间轴
- `POST /keyframe` - 添加关键帧
- `PUT /keyframe/{keyframeId}` - 更新关键帧
- `DELETE /keyframe/{keyframeId}` - 删除关键帧

#### 媒体分析
- `POST /analyze/video` - 分析视频
- `POST /analyze/audio` - 分析音频
- `POST /analyze/image` - 分析图片
- `POST /validate` - 验证媒体文件

#### 渲染和导出
- `POST /render` - 渲染视频
- `GET /render/{jobId}` - 查询渲染状态
- `GET /render/{jobId}/download` - 下载渲染结果

---

## 5. 数据模型设计

### 5.1 核心模型

```java
// 草稿模型
public class Draft {
    private String draftId;
    private String name;
    private String description;
    private Timeline timeline;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String version;
}

// 时间轴模型
public class Timeline {
    private String timelineId;
    private List<VideoTrack> videoTracks;
    private List<AudioTrack> audioTracks;
    private List<TextLayer> textLayers;
    private List<ImageLayer> imageLayers;
    private double duration;
    private int fps;
    private Resolution resolution;
}

// 视频轨道模型
public class VideoTrack {
    private String trackId;
    private List<VideoClip> clips;
    private int index;
}

// 视频片段模型
public class VideoClip {
    private String clipId;
    private String filePath;
    private double startTime;
    private double duration;
    private double inPoint;
    private double outPoint;
    private Position position;
    private Scale scale;
    private double rotation;
    private double opacity;
    private List<Effect> effects;
}

// 音频轨道模型
public class AudioTrack {
    private String trackId;
    private List<AudioClip> clips;
    private double volume;
    private int index;
}

// 音频片段模型
public class AudioClip {
    private String clipId;
    private String filePath;
    private double startTime;
    private double duration;
    private double volume;
    private double fadeIn;
    private double fadeOut;
}

// 文字图层模型
public class TextLayer {
    private String layerId;
    private String text;
    private double startTime;
    private double duration;
    private TextStyle style;
    private Position position;
    private List<Keyframe> keyframes;
    private Animation animation;
}

// 图片图层模型
public class ImageLayer {
    private String layerId;
    private String imagePath;
    private double startTime;
    private double duration;
    private Position position;
    private Size size;
    private double rotation;
    private double opacity;
    private List<Keyframe> keyframes;
    private Animation animation;
}

// 特效模型
public class Effect {
    private String effectId;
    private String type;
    private Map<String, Object> params;
    private double startTime;
    private double duration;
}

// 转场模型
public class Transition {
    private String transitionId;
    private String type;
    private String fromClipId;
    private String toClipId;
    private double duration;
    private Map<String, Object> params;
}

// 关键帧模型
public class Keyframe {
    private String keyframeId;
    private String property;
    private double time;
    private Object value;
    private String easing;
}

// 动画模型
public class Animation {
    private String animationId;
    private String type;
    private List<Keyframe> keyframes;
    private String easing;
}
```

---

## 6. 技术选型和依赖

### 6.1 核心依赖

```xml
<!-- FFmpeg Java 封装 -->
<dependency>
    <groupId>org.bytedeco</groupId>
    <artifactId>ffmpeg-platform</artifactId>
    <version>6.0-1.5.9</version>
</dependency>

<!-- 或者使用更轻量的 FFmpeg CLI 调用 -->
<!-- 需要系统安装 FFmpeg -->

<!-- JSON 处理（已有） -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>${jackson.version}</version>
</dependency>

<!-- 文件处理 -->
<dependency>
    <groupId>commons-io</groupId>
    <artifactId>commons-io</artifactId>
    <version>2.15.1</version>
</dependency>

<!-- 异步处理 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
    <version>${spring.boot.version}</version>
</dependency>
```

### 6.2 FFmpeg 集成方案

**方案1: 使用 FFmpeg CLI（推荐）**
- 优点：稳定、功能完整、性能好
- 缺点：需要系统安装 FFmpeg
- 实现：通过 `ProcessBuilder` 调用 FFmpeg 命令

**方案2: 使用 JavaCV（备选）**
- 优点：纯 Java 实现，无需外部依赖
- 缺点：包体积大，性能略差
- 实现：使用 JavaCV API

**推荐使用方案1**，通过命令行调用 FFmpeg，更灵活且性能更好。

---

## 7. 开发阶段规划

### 阶段一：基础架构搭建（1-2周）

**目标**：搭建项目基础架构和核心引擎

**任务**：
1. 创建 autoclip 模块目录结构
2. 实现 FFmpegEngine 基础封装
3. 实现 MediaAnalysisService（媒体分析）
4. 实现基础的数据模型
5. 配置 MCP 工具注册（空实现）
6. 编写单元测试

**交付物**：
- 基础代码框架
- FFmpeg 集成示例
- 媒体分析功能

---

### 阶段二：核心编辑功能（2-3周）

**目标**：实现视频和音频的基础编辑功能

**任务**：
1. 实现 VideoEditService
   - 视频裁剪、分割、合并
   - 视频变速、旋转、翻转
   - 视频尺寸调整、格式转换
2. 实现 AudioService
   - 音频提取、裁剪、合并
   - 音频变速、变调
   - 音频混音
3. 实现对应的 MCP 工具
4. 编写集成测试

**交付物**：
- 视频编辑功能完整实现
- 音频处理功能完整实现
- 对应的 MCP 工具

---

### 阶段三：图层和时间轴（2-3周）

**目标**：实现图层管理和时间轴控制

**任务**：
1. 实现 TimelineService 和 TimelineEngine
2. 实现 TextService（文字图层）
3. 实现 ImageService（图片图层）
4. 实现关键帧和动画系统
5. 实现草稿管理（DraftService）
6. 实现对应的 MCP 工具

**交付物**：
- 时间轴系统
- 文字和图片图层功能
- 关键帧和动画系统
- 草稿管理功能

---

### 阶段四：特效和渲染（2-3周）

**目标**：实现特效系统和视频渲染

**任务**：
1. 实现 EffectService
   - 视频特效
   - 转场效果
2. 实现 RenderEngine
   - 视频合成渲染
   - 批量处理
3. 实现贴纸功能
4. 实现模板系统
5. 实现对应的 MCP 工具

**交付物**：
- 特效和转场功能
- 视频渲染引擎
- 贴纸和模板功能

---

### 阶段五：HTTP API 和优化（1-2周）

**目标**：实现 HTTP REST API 和性能优化

**任务**：
1. 实现 AutoclipController（REST API）
2. 实现异步渲染任务队列
3. 性能优化和缓存
4. 错误处理和日志完善
5. API 文档编写

**交付物**：
- 完整的 HTTP REST API
- 异步渲染系统
- API 文档

---

### 阶段六：测试和文档（1周）

**目标**：完善测试和文档

**任务**：
1. 编写完整的单元测试
2. 编写集成测试
3. 编写使用文档
4. 编写开发文档
5. 性能测试和优化

**交付物**：
- 测试套件
- 完整文档

---

## 8. 关键技术实现要点

### 8.1 FFmpeg 命令构建

```java
public class FFmpegCommandBuilder {
    // 视频裁剪
    public String buildCutCommand(String input, double startTime, double duration, String output) {
        return String.format(
            "ffmpeg -i %s -ss %.2f -t %.2f -c copy %s",
            input, startTime, duration, output
        );
    }
    
    // 视频变速
    public String buildSpeedCommand(String input, double speed, String output) {
        return String.format(
            "ffmpeg -i %s -filter:v \"setpts=%.2f*PTS\" -filter:a \"atempo=%.2f\" %s",
            input, 1.0/speed, speed, output
        );
    }
    
    // 视频合并
    public String buildMergeCommand(List<String> inputs, String output) {
        // 使用 concat demuxer
        // 需要先创建文件列表
    }
}
```

### 8.2 时间轴引擎设计

```java
public class TimelineEngine {
    // 计算图层在时间轴上的实际位置
    public Position calculateLayerPosition(String layerId, double time) {
        // 根据关键帧插值计算
    }
    
    // 渲染时间轴到视频
    public void renderTimeline(Timeline timeline, String outputPath) {
        // 构建复杂的 FFmpeg filter_complex 命令
    }
}
```

### 8.3 异步渲染任务

```java
@Service
public class RenderService {
    private final ExecutorService renderExecutor;
    private final Map<String, RenderJob> jobs;
    
    public String submitRenderJob(String draftId, RenderConfig config) {
        String jobId = UUID.randomUUID().toString();
        RenderJob job = new RenderJob(jobId, draftId, config);
        jobs.put(jobId, job);
        
        renderExecutor.submit(() -> {
            try {
                job.setStatus(RenderStatus.PROCESSING);
                renderVideo(job);
                job.setStatus(RenderStatus.COMPLETED);
            } catch (Exception e) {
                job.setStatus(RenderStatus.FAILED);
                job.setError(e.getMessage());
            }
        });
        
        return jobId;
    }
}
```

---

## 9. 配置项设计

### 9.1 application.yml 配置

```yaml
autoclip:
  ffmpeg:
    path: ffmpeg  # FFmpeg 可执行文件路径
    timeout: 3600  # 命令执行超时时间（秒）
  
  storage:
    base-dir: ./autoclip-data  # 数据存储基础目录
    drafts-dir: ${autoclip.storage.base-dir}/drafts  # 草稿目录
    output-dir: ${autoclip.storage.base-dir}/output  # 输出目录
    temp-dir: ${autoclip.storage.base-dir}/temp  # 临时文件目录
  
  render:
    max-concurrent-jobs: 2  # 最大并发渲染任务数
    default-resolution:
      width: 1920
      height: 1080
    default-fps: 30
  
  limits:
    max-draft-size: 100MB  # 最大草稿大小
    max-video-duration: 3600  # 最大视频时长（秒）
    max-concurrent-operations: 5  # 最大并发操作数
```

---

## 10. 错误处理策略

### 10.1 错误码定义

```java
public enum AutoclipErrorCode {
    FILE_NOT_FOUND(1001, "文件不存在"),
    INVALID_FORMAT(1002, "不支持的格式"),
    INVALID_PARAMETER(1003, "参数无效"),
    FFMPEG_ERROR(1004, "FFmpeg 处理失败"),
    RENDER_FAILED(1005, "渲染失败"),
    DRAFT_NOT_FOUND(1006, "草稿不存在"),
    OPERATION_TIMEOUT(1007, "操作超时"),
    INSUFFICIENT_RESOURCES(1008, "资源不足");
}
```

### 10.2 异常处理

```java
@ControllerAdvice
public class AutoclipExceptionHandler {
    @ExceptionHandler(AutoclipException.class)
    public ResponseEntity<ErrorResponse> handleAutoclipException(AutoclipException e) {
        ErrorResponse response = new ErrorResponse(
            e.getErrorCode().getCode(),
            e.getErrorCode().getMessage(),
            e.getMessage()
        );
        return ResponseEntity.status(e.getHttpStatus()).body(response);
    }
}
```

---

## 11. 测试策略

### 11.1 单元测试
- 每个 Service 类编写单元测试
- 使用 Mock 对象模拟 FFmpeg 调用
- 测试边界条件和异常情况

### 11.2 集成测试
- 使用真实的 FFmpeg 进行集成测试
- 测试完整的编辑流程
- 测试 MCP 工具调用

### 11.3 性能测试
- 测试大文件处理性能
- 测试并发渲染能力
- 测试内存使用情况

---

## 12. 部署和运维

### 12.1 系统要求
- JDK 17+
- FFmpeg 4.0+（需要系统安装）
- 至少 4GB 可用内存
- 足够的磁盘空间（用于临时文件和输出）

### 12.2 Docker 部署

```dockerfile
FROM openjdk:17-jdk-slim

# 安装 FFmpeg
RUN apt-get update && apt-get install -y ffmpeg && rm -rf /var/lib/apt/lists/*

COPY target/qingcloud-mcp-*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 12.3 监控指标
- 渲染任务队列长度
- 渲染任务成功率
- 平均渲染时间
- 系统资源使用率

---

## 13. 后续扩展计划

### 13.1 高级功能
- AI 驱动的自动剪辑
- 语音识别和字幕生成
- 人脸识别和跟踪
- 场景检测和自动分段

### 13.2 云服务集成
- 对象存储集成（OSS/S3）
- CDN 加速
- 分布式渲染

### 13.3 性能优化
- GPU 加速渲染
- 分布式处理
- 缓存优化

---

## 14. 风险评估和应对

### 14.1 技术风险
- **FFmpeg 依赖**：需要确保系统安装 FFmpeg
  - 应对：提供 Docker 镜像，包含 FFmpeg
- **大文件处理**：可能内存不足
  - 应对：流式处理，限制并发数
- **渲染性能**：复杂项目渲染时间长
  - 应对：异步任务，进度反馈

### 14.2 业务风险
- **文件格式兼容性**：某些格式可能不支持
  - 应对：格式转换，错误提示
- **操作复杂度**：API 参数多，易出错
  - 应对：参数验证，详细文档，示例代码

---

## 15. 参考实现说明

### 15.1 VectCutAPI 参考

本开发计划参考了 [VectCutAPI](https://github.com/sun-guannan/VectCutAPI) 项目的实际实现，该项目是一个基于 Python 的视频剪辑 API，主要特点：

- **支持剪映/CapCut 草稿格式**：可以直接生成剪映或 CapCut 可导入的草稿文件
- **11 个核心 MCP 工具**：create_draft, add_video, add_audio, add_image, add_text, add_subtitle, add_effect, add_sticker, add_video_keyframe, get_video_duration, save_draft
- **高级文字功能**：支持文字阴影、背景、描边、多样式文本、入场/出场动画
- **视频/图片蒙版**：支持圆形、矩形、线性、镜像、心形、星形等多种蒙版类型
- **转场效果**：支持多种转场类型，可在添加素材时直接指定
- **音频特效**：支持多种音频特效（如 Tremble, Big_House 等）
- **批量关键帧**：支持批量添加关键帧，实现复杂动画效果

### 15.2 功能对齐

本 Autoclip 开发计划在以下方面参考了 VectCutAPI 的实现：

1. **工具参数设计**：参考了 VectCutAPI 的实际参数设计，确保功能完整性
2. **草稿格式支持**：计划支持剪映/CapCut 草稿格式导出（需要实现相应的格式转换）
3. **高级功能**：补充了文字阴影、背景、描边、蒙版、转场等高级功能
4. **批量操作**：支持批量关键帧等批量操作模式

### 15.3 技术差异

与 VectCutAPI 的主要技术差异：

- **语言**：VectCutAPI 使用 Python，Autoclip 使用 Java
- **底层实现**：VectCutAPI 直接操作剪映草稿格式，Autoclip 使用 FFmpeg 进行媒体处理
- **草稿格式**：Autoclip 需要实现剪映/CapCut 草稿格式的生成（可参考 VectCutAPI 的 pyJianYingDraft 库）

### 15.4 基于 pyJianYingDraft 的核心设计改进

通过对 `pyJianYingDraft` 源码的深入分析，Autoclip 开发计划在以下方面进行了重要改进：

#### 15.4.1 草稿文件结构设计

- **完整的草稿文件结构**：参考 `pyJianYingDraft` 的设计，Autoclip 将生成完整的剪映/CapCut 草稿文件结构，包括：
  - `draft_info.json`：包含所有素材、轨道、片段信息
  - `draft_meta_info.json`：包含草稿元数据
  - `assets/` 目录：存放所有媒体素材（audio、video、image）

#### 15.4.2 素材管理系统

- **ScriptMaterial 设计**：采用与 `pyJianYingDraft` 相同的素材管理方式：
  - 素材与片段分离：素材定义在 `ScriptMaterial` 中，片段通过 `material_id` 引用
  - 支持素材复用：多个片段可以引用同一个素材
  - 全局唯一ID：所有素材使用 UUID 格式的 `material_id`

#### 15.4.3 轨道和片段层级结构

- **Track 和 Segment 设计**：参考 `pyJianYingDraft` 的轨道系统：
  - 轨道按类型分类（VIDEO、AUDIO、EFFECT、FILTER、STICKER、TEXT）
  - 片段通过 `material_id` 引用素材，实现素材与片段的解耦
  - 支持片段重叠检测和自动调整

#### 15.4.4 时间系统

- **微秒精度**：内部使用微秒作为时间单位，确保高精度
- **API 接口使用秒**：对外接口使用秒作为单位，便于用户理解
- **Timerange 支持**：支持时间范围的运算和比较

#### 15.4.5 关键帧系统

- **KeyframeList 设计**：参考 `pyJianYingDraft` 的关键帧系统：
  - 关键帧按属性类型分组管理（POSITION、ROTATION、SCALE、ALPHA等）
  - 支持线性插值、缓动函数等动画效果
  - 关键帧时间相对于片段开始时间，便于片段移动时保持动画

#### 15.4.6 草稿生成流程

- **完整的生成流程**：参考 `pyJianYingDraft` 的 `save_draft_impl` 实现：
  1. 创建草稿结构
  2. 添加素材和片段
  3. 下载/复制媒体文件到 assets 目录
  4. 更新素材路径（replace_path）
  5. 导出 draft_info.json 和 draft_meta_info.json
  6. 可选：压缩为ZIP并上传到OSS

#### 15.4.7 导出方式

- **方式1：生成草稿文件（推荐）**：直接生成剪映/CapCut 可导入的草稿文件
- **方式2：UI 自动化导出（可选）**：使用 UI 自动化工具控制剪映/CapCut 导出（仅Windows，参考 `JianyingController`）

#### 15.4.8 模板模式支持

- **模板功能**：参考 `pyJianYingDraft` 的 `load_template` 和 `duplicate_as_template`：
  - 支持从模板创建草稿
  - 模板只保存轨道和片段结构，不包含实际媒体文件
  - 支持模板参数化（如分辨率、时长等）

#### 15.4.9 改进和优化

基于对 `pyJianYingDraft` 的分析，Autoclip 计划在以下方面进行改进：

1. **更完善的错误处理**：素材验证、片段重叠检测、轨道索引冲突检测
2. **性能优化**：素材缓存机制、异步下载媒体文件、批量操作优化
3. **扩展性设计**：插件化的特效系统、可配置的草稿格式版本支持
4. **用户体验**：草稿预览功能、草稿版本管理、进度反馈

---

## 16. 总结

本开发计划详细规划了 Autoclip 自动化剪辑模块的完整实现方案，包括：

1. **功能模块**：10 大核心功能模块，70+ MCP 工具（参考 VectCutAPI 和 pyJianYingDraft 后补充完善）
2. **技术架构**：分层设计，易于扩展和维护
3. **开发计划**：6 个阶段，预计 9-13 周完成
4. **技术选型**：基于 FFmpeg 的成熟方案，支持剪映/CapCut 草稿格式
5. **API 设计**：同时支持 MCP 协议和 HTTP REST API
6. **参考实现**：
   - 参考 VectCutAPI 的实际功能，确保功能完整性和实用性
   - **深入分析 pyJianYingDraft 源码**，采用其核心设计理念：
     - 完整的草稿文件结构（draft_info.json、draft_meta_info.json、assets目录）
     - ScriptMaterial 素材管理系统（素材与片段分离、全局唯一ID）
     - Track 和 Segment 层级结构（轨道类型分类、片段引用素材）
     - 微秒精度的时间系统（Timerange 支持）
     - KeyframeList 关键帧系统（属性分组、插值计算）
     - 完整的草稿生成流程（下载媒体、更新路径、导出JSON）
     - 模板模式支持（从模板创建、参数化）
7. **核心改进**：基于 pyJianYingDraft 的分析，在错误处理、性能优化、扩展性、用户体验等方面进行了改进设计

该计划为下一步的具体开发工作提供了清晰的路线图和实施指南，特别是基于 pyJianYingDraft 核心实现的详细设计，确保了草稿生成、保存和导出功能的完整性和可靠性。

