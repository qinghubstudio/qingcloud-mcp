package com.qingcloud.mcp.autoclip.service;

import com.qingcloud.mcp.autoclip.model.*;
import com.qingcloud.mcp.autoclip.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 草稿管理服务
 */
@Service
public class DraftService {

    @Autowired
    private DraftCache draftCache;

    /**
     * 创建新草稿
     */
    public ScriptFile createDraft(int width, int height) {
        ScriptFile script = new ScriptFile(width, height);
        draftCache.put(script.getDraftId(), script);
        return script;
    }

    /**
     * 获取草稿
     */
    public ScriptFile getDraft(String draftId) {
        return draftCache.get(draftId);
    }

    /**
     * 获取或创建草稿
     */
    public ScriptFile getOrCreateDraft(String draftId, int width, int height) {
        if (draftId != null && draftCache.contains(draftId)) {
            return draftCache.get(draftId);
        }
        return createDraft(width, height);
    }

    /**
     * 添加视频到草稿
     */
    public Map<String, Object> addVideo(
            String draftId, String videoUrl,
            double start, double end, double targetStart,
            int width, int height, String trackName,
            double speed, double volume) {

        ScriptFile script = getOrCreateDraft(draftId, width, height);

        // 创建视频素材
        String materialName = "video_" + videoUrl.hashCode() + ".mp4";
        VideoMaterial material = VideoMaterial.createVideo(videoUrl, materialName);
        script.getMaterials().addVideo(material);

        // 确保轨道存在
        Track track = script.getTrack(trackName);
        if (track == null) {
            track = script.addTrack(TrackType.VIDEO, trackName);
        }

        // 计算时间范围
        double sourceDuration = end - start;
        double targetDuration = sourceDuration / speed;

        Timerange sourceRange = Timerange.fromSeconds(start, sourceDuration);
        Timerange targetRange = Timerange.fromSeconds(targetStart, targetDuration);

        // 创建视频片段
        VideoSegment segment = new VideoSegment(material.getMaterialId(), targetRange, sourceRange);
        segment.setSpeed(speed);
        segment.setVolume(volume);

        // 添加到轨道
        track.addSegment(segment);

        Map<String, Object> result = new HashMap<>();
        result.put("draftId", script.getDraftId());
        result.put("segmentId", segment.getSegmentId());
        return result;
    }

    /**
     * 添加音频到草稿
     */
    public Map<String, Object> addAudio(
            String draftId, String audioUrl,
            double start, double end, double targetStart,
            int width, int height, String trackName,
            double speed, double volume) {

        ScriptFile script = getOrCreateDraft(draftId, width, height);

        // 创建音频素材
        String materialName = "audio_" + audioUrl.hashCode() + ".mp3";
        AudioMaterial material = AudioMaterial.create(audioUrl, materialName);
        script.getMaterials().addAudio(material);

        // 确保轨道存在
        Track track = script.getTrack(trackName);
        if (track == null) {
            track = script.addTrack(TrackType.AUDIO, trackName);
        }

        // 计算时间范围
        double sourceDuration = end - start;
        double targetDuration = sourceDuration / speed;

        Timerange sourceRange = Timerange.fromSeconds(start, sourceDuration);
        Timerange targetRange = Timerange.fromSeconds(targetStart, targetDuration);

        // 创建音频片段
        AudioSegment segment = new AudioSegment(material.getMaterialId(), targetRange, sourceRange);
        segment.setSpeed(speed);
        segment.setVolume(volume);

        // 添加到轨道
        track.addSegment(segment);

        Map<String, Object> result = new HashMap<>();
        result.put("draftId", script.getDraftId());
        result.put("segmentId", segment.getSegmentId());
        return result;
    }

    /**
     * 添加图片到草稿
     */
    public Map<String, Object> addImage(
            String draftId, String imageUrl,
            double start, double end,
            int width, int height, String trackName) {

        ScriptFile script = getOrCreateDraft(draftId, width, height);

        // 创建图片素材（作为 VideoMaterial，type为photo）
        String materialName = "image_" + imageUrl.hashCode() + ".jpg";
        VideoMaterial material = VideoMaterial.createImage(imageUrl, materialName);
        script.getMaterials().addVideo(material);

        // 确保轨道存在
        Track track = script.getTrack(trackName);
        if (track == null) {
            track = script.addTrack(TrackType.VIDEO, trackName);
        }

        double duration = end - start;
        Timerange sourceRange = Timerange.fromSeconds(0, duration);
        Timerange targetRange = Timerange.fromSeconds(start, duration);

        VideoSegment segment = new VideoSegment(material.getMaterialId(), targetRange, sourceRange);
        track.addSegment(segment);

        Map<String, Object> result = new HashMap<>();
        result.put("draftId", script.getDraftId());
        result.put("segmentId", segment.getSegmentId());
        return result;
    }

    /**
     * 添加文字到草稿
     */
    public Map<String, Object> addText(
            String draftId, String text,
            double startTime, double endTime,
            String fontColor, double fontSize,
            double transformX, double transformY,
            int width, int height, String trackName) {

        ScriptFile script = getOrCreateDraft(draftId, width, height);

        // 确保文字轨道存在
        Track track = script.getTrack(trackName);
        if (track == null) {
            track = script.addTrack(TrackType.TEXT, trackName);
        }

        double duration = endTime - startTime;
        Timerange targetRange = Timerange.fromSeconds(startTime, duration);

        TextSegment segment = new TextSegment(text, targetRange);
        segment.setFontColor(fontColor);
        segment.setFontSize(fontSize);
        segment.setTransformX(transformX);
        segment.setTransformY(transformY);

        track.addSegment(segment);

        Map<String, Object> result = new HashMap<>();
        result.put("draftId", script.getDraftId());
        result.put("segmentId", segment.getSegmentId());
        return result;
    }

    /**
     * 获取草稿信息
     */
    public Map<String, Object> getDraftInfo(String draftId) {
        ScriptFile script = draftCache.get(draftId);
        if (script == null) {
            return null;
        }

        Map<String, Object> info = new HashMap<>();
        info.put("draftId", script.getDraftId());
        info.put("width", script.getWidth());
        info.put("height", script.getHeight());
        info.put("duration", script.getDuration() / 1_000_000.0);
        info.put("trackCount", script.getTracks().size());
        info.put("videoCount", script.getMaterials().getVideos().size());
        info.put("audioCount", script.getMaterials().getAudios().size());
        return info;
    }

    /**
     * 添加关键帧到片段
     */
    public Map<String, Object> addKeyframe(
            String draftId, String segmentId,
            KeyframeProperty property, double timeSec,
            double value, String easing) {

        ScriptFile script = draftCache.get(draftId);
        if (script == null) {
            return null;
        }

        // 查找片段
        for (Track track : script.getTracks().values()) {
            for (BaseSegment seg : track.getSegments()) {
                if (seg.getSegmentId().equals(segmentId)) {
                    Keyframe keyframe = new Keyframe(property, timeSec, value, easing);
                    // 添加关键帧到片段（这里简化处理，实际应存储到segment的keyframes列表）

                    Map<String, Object> result = new HashMap<>();
                    result.put("draftId", draftId);
                    result.put("segmentId", segmentId);
                    result.put("keyframeId", keyframe.getKeyframeId());
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * 设置片段转场效果
     */
    public Map<String, Object> setTransition(
            String draftId, String segmentId,
            TransitionType type, double duration) {

        ScriptFile script = draftCache.get(draftId);
        if (script == null) {
            return null;
        }

        // 查找片段并设置转场
        for (Track track : script.getTracks().values()) {
            for (BaseSegment seg : track.getSegments()) {
                if (seg.getSegmentId().equals(segmentId) && seg instanceof VideoSegment vs) {
                    vs.setTransition(type.name().toLowerCase());
                    vs.setTransitionDuration((long) (duration * 1_000_000));

                    Map<String, Object> result = new HashMap<>();
                    result.put("draftId", draftId);
                    result.put("segmentId", segmentId);
                    result.put("transition", type.name());
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * 设置片段蒙版效果
     */
    public Map<String, Object> setMask(
            String draftId, String segmentId,
            MaskType type, double centerX, double centerY,
            double size, double feather, boolean invert) {

        ScriptFile script = draftCache.get(draftId);
        if (script == null) {
            return null;
        }

        // 查找片段并设置蒙版
        for (Track track : script.getTracks().values()) {
            for (BaseSegment seg : track.getSegments()) {
                if (seg.getSegmentId().equals(segmentId) && seg instanceof VideoSegment vs) {
                    vs.setMaskType(type.name().toLowerCase());

                    Map<String, Object> result = new HashMap<>();
                    result.put("draftId", draftId);
                    result.put("segmentId", segmentId);
                    result.put("mask", type.name());
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * 设置片段滤镜
     */
    public Map<String, Object> setFilter(
            String draftId, String segmentId,
            FilterType type, double intensity) {

        ScriptFile script = draftCache.get(draftId);
        if (script == null) {
            return null;
        }

        for (Track track : script.getTracks().values()) {
            for (BaseSegment seg : track.getSegments()) {
                if (seg.getSegmentId().equals(segmentId) && seg instanceof VideoSegment vs) {
                    // 存储滤镜信息（简化处理）
                    Map<String, Object> result = new HashMap<>();
                    result.put("draftId", draftId);
                    result.put("segmentId", segmentId);
                    result.put("filter", type.name());
                    result.put("intensity", intensity);
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * 设置片段速度
     */
    public Map<String, Object> setSpeed(
            String draftId, String segmentId,
            double speed, boolean keepPitch) {

        ScriptFile script = draftCache.get(draftId);
        if (script == null) {
            return null;
        }

        for (Track track : script.getTracks().values()) {
            for (BaseSegment seg : track.getSegments()) {
                if (seg.getSegmentId().equals(segmentId)) {
                    if (seg instanceof VideoSegment vs) {
                        vs.setSpeed(speed);
                    } else if (seg instanceof AudioSegment as) {
                        as.setSpeed(speed);
                    }

                    Map<String, Object> result = new HashMap<>();
                    result.put("draftId", draftId);
                    result.put("segmentId", segmentId);
                    result.put("speed", speed);
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * 删除草稿
     */
    public boolean deleteDraft(String draftId) {
        if (draftCache.contains(draftId)) {
            draftCache.remove(draftId);
            return true;
        }
        return false;
    }
}
