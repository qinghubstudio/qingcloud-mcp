# Autoclip MCP 使用手册

## 概述

Autoclip 是一个基于 MCP 协议的自动化视频剪辑服务，允许 AI Agent 通过标准化接口创建和编辑视频草稿。

## 启用方式

在 `application.yml` 中配置:

```yaml
spring:
  profiles:
    active: autoclip
mcp:
  transport:
    mode: http
```

## MCP 工具列表

### 草稿管理

#### autoclip_createDraft

创建新草稿

```json
{
  "width": 1080,
  "height": 1920
}
```

#### autoclip_getDraftInfo

获取草稿信息

```json
{
  "draftId": "dfd_cat_xxx"
}
```

#### autoclip_saveDraft

保存草稿为剪映格式

```json
{
  "draftId": "dfd_cat_xxx",
  "outputFolder": "./output"
}
```

#### autoclip_deleteDraft

删除草稿

```json
{
  "draftId": "dfd_cat_xxx"
}
```

### 媒体添加

#### autoclip_addVideo

添加视频

```json
{
  "videoUrl": "https://example.com/video.mp4",
  "draftId": "dfd_cat_xxx",
  "start": 0,
  "end": 10,
  "targetStart": 0,
  "speed": 1.0,
  "volume": 1.0
}
```

#### autoclip_addAudio

添加音频

```json
{
  "audioUrl": "https://example.com/audio.mp3",
  "draftId": "dfd_cat_xxx",
  "start": 0,
  "end": 30,
  "volume": 0.8
}
```

#### autoclip_addImage

添加图片

```json
{
  "imageUrl": "https://example.com/image.jpg",
  "draftId": "dfd_cat_xxx",
  "start": 0,
  "end": 3
}
```

#### autoclip_addText

添加文字

```json
{
  "text": "标题文字",
  "draftId": "dfd_cat_xxx",
  "startTime": 0,
  "endTime": 5,
  "fontColor": "#ffffff",
  "fontSize": 8.0,
  "transformY": -0.8
}
```

#### autoclip_addSubtitle

导入 SRT 字幕

```json
{
  "srtPath": "./subtitles.srt",
  "draftId": "dfd_cat_xxx",
  "timeOffset": 0,
  "fontColor": "#ffffff"
}
```

### 效果设置

#### autoclip_addKeyframe

添加关键帧

```json
{
  "draftId": "dfd_cat_xxx",
  "segmentId": "seg_xxx",
  "property": "scale_x",
  "time": 1.5,
  "value": 1.2,
  "easing": "ease_out"
}
```

#### autoclip_setTransition

设置转场

```json
{
  "draftId": "dfd_cat_xxx",
  "segmentId": "seg_xxx",
  "transitionType": "dissolve",
  "duration": 0.5
}
```

#### autoclip_setMask

设置蒙版

```json
{
  "draftId": "dfd_cat_xxx",
  "segmentId": "seg_xxx",
  "maskType": "circle",
  "size": 0.8,
  "feather": 0.1
}
```

#### autoclip_setFilter

设置滤镜

```json
{
  "draftId": "dfd_cat_xxx",
  "segmentId": "seg_xxx",
  "filterType": "cinematic",
  "intensity": 0.8
}
```

#### autoclip_setSpeed

设置速度

```json
{
  "draftId": "dfd_cat_xxx",
  "segmentId": "seg_xxx",
  "speed": 2.0,
  "keepPitch": true
}
```

## 典型工作流

```
1. createDraft          创建草稿
2. addVideo/addImage    添加素材
3. addText/addSubtitle  添加文字
4. setTransition        设置转场
5. setFilter            设置滤镜
6. saveDraft            保存草稿
```

## 注意事项

- 时间单位：API 接口使用秒，内部使用微秒
- 草稿存储在内存，需要调用 saveDraft 持久化
- 位置参数范围：-1 到 1（0 为中心）
