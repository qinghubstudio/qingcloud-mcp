# CapCut/VectCut 项目架构分析 - Part 1: 整体架构

## 项目概述

CapCutAPI-Complete 和 VectCutAPI 是两个基于 Python 的视频编辑 API 项目，它们通过操作剪映（JianYing）/CapCut 的草稿文件格式来实现视频编辑功能。

**核心特点**：

- **无需剪映软件**：直接生成剪映可识别的草稿文件
- **基于 pyJianYingDraft 库**：操作剪映草稿的 JSON 结构
- **支持 MCP 协议**：可与 AI Agent 集成
- **支持 HTTP API**：Flask REST API 服务

## 技术栈

| 组件     | 技术               | 说明                   |
| -------- | ------------------ | ---------------------- |
| 语言     | Python 3.x         | 主要开发语言           |
| 核心库   | pyJianYingDraft    | 剪映草稿操作库         |
| Web 框架 | Flask              | HTTP API 服务          |
| MCP 协议 | mcp (Python SDK)   | Model Context Protocol |
| 媒体处理 | ffprobe, imageio   | 获取媒体元数据         |
| 文件下载 | requests           | 下载远程媒体文件       |
| 并发处理 | ThreadPoolExecutor | 多线程下载             |

## 核心架构模式

### 1. 草稿文件结构

剪映草稿是一个文件夹，包含：

```
draft_id/
├── draft_info.json          # 草稿元数据（核心文件）
└── assets/
    ├── video/               # 视频素材
    ├── audio/               # 音频素材
    └── image/               # 图片素材
```

### 2. 工作流程

```
1. 创建草稿 (create_draft)
   ↓
2. 添加素材到草稿 (add_video/audio/image/text)
   - 素材信息存储在内存中的 Script 对象
   - 使用 remote_url 引用远程媒体
   ↓
3. 保存草稿 (save_draft)
   - 复制模板文件夹
   - 下载所有远程媒体到本地
   - 更新媒体元数据（时长、分辨率等）
   - 生成 draft_info.json
   - 打包成 ZIP（可选）
   - 上传到 OSS（可选）
```

### 3. 内存管理模式

**草稿缓存**：

```python
# draft_cache.py
DRAFT_CACHE = {}  # {draft_id: Script_file}

def update_cache(draft_id, script):
    DRAFT_CACHE[draft_id] = script
```

**任务缓存**：

```python
# save_task_cache.py
DRAFT_TASKS = OrderedDict()  # {task_id: task_status}
```

## pyJianYingDraft 核心概念

### Script_file（草稿文件）

```python
class Script_file:
    width: int              # 视频宽度
    height: int             # 视频高度
    fps: int                # 帧率
    duration: int           # 总时长（微秒）
    materials: Script_material  # 素材集合
    tracks: Dict[str, Track]    # 轨道字典
```

### Script_material（素材集合）

```python
class Script_material:
    audios: List[Audio_material]
    videos: List[Video_material]
    stickers: List[Dict]
    texts: List[Dict]
    animations: List[Segment_animations]
    transitions: List[Transition]
    filters: List[Filter]
    speeds: List[Speed]
    masks: List[Dict]
```

### Track（轨道）

```python
class Track:
    track_type: Track_type      # video/audio/text/effect
    name: str                   # 轨道名称
    render_index: int           # 渲染层级
    segments: List[Base_segment] # 片段列表
```

### Segment（片段）

```python
class Video_segment:
    material_instance: Video_material
    target_timerange: Timerange  # 在时间轴上的位置
    source_timerange: Timerange  # 素材裁剪范围
    speed: Speed                 # 播放速度
    clip_settings: Clip_settings # 变换设置
    volume: float                # 音量
    transition: Transition       # 转场
    mask: Mask                   # 蒙版
    effects: List[Video_effect]  # 特效
```

## 关键设计模式

### 1. 延迟下载模式

素材添加时只记录 `remote_url`，不立即下载：

```python
video_material = draft.Video_material(
    material_type='video',
    remote_url=video_url,      # 只记录URL
    material_name=material_name,
    duration=0,                # 初始为0
    width=0,
    height=0
)
```

保存时才下载并更新元数据：

```python
def save_draft_impl(draft_id):
    # 1. 收集所有下载任务
    download_tasks = []
    for video in script.materials.videos:
        download_tasks.append({
            'func': download_file,
            'args': (video.remote_url, local_path)
        })

    # 2. 并发下载
    with ThreadPoolExecutor(max_workers=16) as executor:
        futures = [executor.submit(task['func'], *task['args'])
                   for task in download_tasks]

    # 3. 更新元数据
    update_media_metadata(script)
```

### 2. 模板复制模式

使用预制模板文件夹，避免从零创建：

```python
# 复制模板
draft_folder.duplicate_as_template("template", draft_id)

# 模板包含基础结构和必要的元数据
template/
├── draft_content.json
├── draft_info.json
├── draft_meta_info.json
└── assets/
```

### 3. 时间轴管理

所有时间使用微秒（microseconds）：

```python
# 时间范围对象
class Timerange:
    start: int      # 开始时间（微秒）
    duration: int   # 持续时间（微秒）

    @property
    def end(self):
        return self.start + self.duration

# 时间工具函数
def tim(time_str):
    """将时间字符串转为微秒"""
    # "1.5s" -> 1500000
    # "500ms" -> 500000
```

## 下一步

继续阅读：

- Part 2: MCP 集成方式
- Part 3: 核心功能实现
- Part 4: Java 移植方案
