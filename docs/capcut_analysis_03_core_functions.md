# CapCut/VectCut 项目架构分析 - Part 3: 核心功能实现

## 1. 添加视频 (add_video_track)

### 核心流程

```python
def add_video_track(
    video_url: str,
    draft_id: Optional[str] = None,
    start: float = 0,
    end: Optional[float] = None,
    target_start: float = 0,
    speed: float = 1.0,
    transition: Optional[str] = None,
    mask_type: Optional[str] = None,
    background_blur: Optional[int] = None,
    # ... 更多参数
):
    # 1. 获取或创建草稿
    draft_id, script = get_or_create_draft(draft_id, width, height)

    # 2. 确保视频轨道存在
    try:
        script.get_track(draft.Track_type.video, track_name=None)
    except exceptions.TrackNotFound:
        script.add_track(draft.Track_type.video, relative_index=0)

    # 3. 创建视频素材
    material_name = f"video_{url_to_hash(video_url)}.mp4"
    video_material = draft.Video_material(
        material_type='video',
        remote_url=video_url,
        material_name=material_name,
        duration=0,  # 初始为0，保存时更新
        width=0,
        height=0
    )

    # 4. 计算时间范围
    source_duration = (end or 0) - start
    target_duration = source_duration / speed

    source_timerange = trange(f"{start}s", f"{source_duration}s")
    target_timerange = trange(f"{target_start}s", f"{target_duration}s")

    # 5. 创建视频片段
    video_segment = draft.Video_segment(
        video_material,
        target_timerange=target_timerange,
        source_timerange=source_timerange,
        speed=speed,
        clip_settings=Clip_settings(
            transform_x=transform_x,
            transform_y=transform_y,
            scale_x=scale_x,
            scale_y=scale_y
        ),
        volume=volume
    )

    # 6. 添加转场效果
    if transition:
        transition_type = getattr(draft.Transition_type, transition)
        video_segment.add_transition(
            transition_type,
            duration=int(transition_duration * 1e6)
        )

    # 7. 添加蒙版
    if mask_type:
        mask_type_enum = getattr(draft.Mask_type, mask_type)
        video_segment.add_mask(
            script,
            mask_type_enum,
            center_x=mask_center_x,
            center_y=mask_center_y,
            size=mask_size
        )

    # 8. 添加背景模糊
    if background_blur:
        blur_values = {1: 0.0625, 2: 0.375, 3: 0.75, 4: 1.0}
        video_segment.add_background_filling(
            "blur",
            blur=blur_values[background_blur]
        )

    # 9. 添加片段到轨道
    script.add_segment(video_segment, track_name=track_name)

    return {"draft_id": draft_id, "draft_url": generate_draft_url(draft_id)}
```

### 关键点

- **延迟下载**：只记录 URL，不下载文件
- **时间计算**：考虑速度因素调整时长
- **链式调用**：转场、蒙版、特效可叠加

## 2. 添加音频 (add_audio_track)

```python
def add_audio_track(
    audio_url: str,
    draft_id: Optional[str] = None,
    start: float = 0,
    end: Optional[float] = None,
    volume: float = 1.0,
    speed: float = 1.0,
    sound_effects: Optional[List[Tuple]] = None
):
    # 1. 获取草稿
    draft_id, script = get_or_create_draft(draft_id, width, height)

    # 2. 创建音频素材
    material_name = f"audio_{url_to_hash(audio_url)}.mp3"
    audio_material = draft.Audio_material(
        remote_url=audio_url,
        material_name=material_name,
        duration=0
    )

    # 3. 创建音频片段
    audio_segment = draft.Audio_segment(
        audio_material,
        target_timerange=target_timerange,
        source_timerange=source_timerange,
        speed=speed,
        volume=volume
    )

    # 4. 添加音效
    if sound_effects:
        for effect_type, params in sound_effects:
            effect_enum = getattr(draft.Audio_effect_type, effect_type)
            audio_segment.add_effect(effect_enum, params)

    # 5. 添加到轨道
    script.add_segment(audio_segment, track_name=track_name)

    return {"draft_id": draft_id}
```

## 3. 添加图片 (add_image_impl)

```python
def add_image_impl(
    image_url: str,
    start: float = 0,
    end: float = 3.0,
    intro_animation: Optional[str] = None,
    outro_animation: Optional[str] = None,
    transition: Optional[str] = None
):
    # 1. 创建图片素材（作为 Video_material）
    material_name = f"image_{url_to_hash(image_url)}.jpg"
    image_material = draft.Video_material(
        material_type='photo',  # 注意：图片类型是 'photo'
        remote_url=image_url,
        material_name=material_name,
        duration=int((end - start) * 1e6),  # 图片时长
        width=0,
        height=0
    )

    # 2. 创建视频片段
    duration = end - start
    video_segment = draft.Video_segment(
        image_material,
        target_timerange=trange(f"{start}s", f"{duration}s"),
        source_timerange=trange("0s", f"{duration}s")
    )

    # 3. 添加入场动画
    if intro_animation:
        intro_type = getattr(draft.Intro_type, intro_animation)
        video_segment.add_intro_animation(intro_type, duration=500000)

    # 4. 添加出场动画
    if outro_animation:
        outro_type = getattr(draft.Outro_type, outro_animation)
        video_segment.add_outro_animation(outro_type, duration=500000)

    script.add_segment(video_segment, track_name=track_name)
```

## 4. 添加文本 (add_text_impl)

```python
def add_text_impl(
    text: str,
    start: float,
    end: float,
    font_color: str = "#ffffff",
    font_size: float = 8.0,
    shadow_enabled: bool = False,
    background_color: Optional[str] = None,
    text_styles: Optional[List[TextStyleRange]] = None
):
    # 1. 创建文本样式
    style = Text_style(
        size=font_size,
        color=hex_to_rgb(font_color),
        alpha=font_alpha,
        bold=False,
        italic=False
    )

    # 2. 创建边框（可选）
    border = None
    if border_width > 0:
        border = Text_border(
            width=border_width,
            color=hex_to_rgb(border_color),
            alpha=border_alpha
        )

    # 3. 创建背景（可选）
    background = None
    if background_color:
        background = Text_background(
            color=hex_to_rgb(background_color),
            alpha=background_alpha,
            style=background_style,
            round_radius=background_round_radius
        )

    # 4. 创建文本片段
    duration = end - start
    text_segment = Text_segment(
        text,
        target_timerange=trange(f"{start}s", f"{duration}s"),
        style=style,
        border=border,
        background=background,
        clip_settings=Clip_settings(
            transform_x=transform_x,
            transform_y=transform_y
        )
    )

    # 5. 添加文本样式范围（多样式文本）
    if text_styles:
        for style_range in text_styles:
            text_segment.add_style_range(style_range)

    script.add_segment(text_segment, track_name=track_name)
```

## 5. 保存草稿 (save_draft_impl)

### 完整流程

```python
def save_draft_impl(draft_id: str, draft_folder: str = None):
    # 1. 获取草稿对象
    script = DRAFT_CACHE[draft_id]

    # 2. 复制模板
    template_dir = "template" if IS_CAPCUT_ENV else "template_jianying"
    draft_folder_for_duplicate.duplicate_as_template(template_dir, draft_id)

    # 3. 更新媒体元数据
    update_media_metadata(script, task_id)

    # 4. 收集下载任务
    download_tasks = []
    for video in script.materials.videos:
        if video.remote_url:
            download_tasks.append({
                'type': 'video',
                'func': download_file,
                'args': (video.remote_url, local_path)
            })

    # 5. 并发下载
    with ThreadPoolExecutor(max_workers=16) as executor:
        futures = {executor.submit(task['func'], *task['args']): task
                   for task in download_tasks}

        for future in as_completed(futures):
            local_path = future.result()
            # 更新进度

    # 6. 保存 draft_info.json
    script.dump(os.path.join(draft_id, "draft_info.json"))

    # 7. 压缩并上传（可选）
    if IS_UPLOAD_DRAFT:
        zip_path = zip_draft(draft_id)
        draft_url = upload_to_oss(zip_path)

    return {"draft_url": draft_url}
```

### 元数据更新

```python
def update_media_metadata(script, task_id=None):
    # 处理视频
    for video in script.materials.videos:
        if video.material_type == 'video':
            # 使用 ffprobe 获取信息
            info = get_video_info(video.remote_url)
            video.width = info['width']
            video.height = info['height']
            video.duration = int(info['duration'] * 1e6)

            # 更新片段时间范围
            for segment in find_segments_by_material(video):
                if segment.source_timerange.end > video.duration:
                    # 调整时间范围
                    adjust_timerange(segment, video.duration)
```

## 下一步

继续阅读：

- Part 4: Java 移植方案
