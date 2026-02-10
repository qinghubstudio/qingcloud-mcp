package com.qingcloud.mcp.autoclip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingcloud.mcp.autoclip.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 草稿导出服务 - 使用模板方式
 */
@Service
public class DraftExportService {

    private static final Logger logger = LoggerFactory.getLogger(DraftExportService.class);

    @Value("${autoclip.storage.drafts-dir:./autoclip-data/drafts}")
    private String draftsDir;

    @Autowired
    private DraftCache draftCache;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 保存草稿 - 使用模板填充
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> saveDraft(String draftId, String outputFolder) throws IOException {
        ScriptFile script = draftCache.get(draftId);
        if (script == null) {
            throw new IllegalArgumentException("Draft not found: " + draftId);
        }

        // 确定输出目录
        String targetFolder = outputFolder != null ? outputFolder : draftsDir;
        Path draftFolder = Path.of(targetFolder, draftId);
        Files.createDirectories(draftFolder);

        // 读取模板
        ClassPathResource templateResource = new ClassPathResource("autoclip-template/draft_info.json");
        Map<String, Object> template = objectMapper.readValue(templateResource.getInputStream(), Map.class);

        // 填充模板数据
        fillTemplate(template, script);

        // 保存 draft_info.json
        Path draftInfoPath = draftFolder.resolve("draft_info.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(draftInfoPath.toFile(), template);

        // 生成 draft_meta_info.json
        Map<String, Object> metaInfo = buildMetaInfo(script, draftFolder.toString());
        Path metaInfoPath = draftFolder.resolve("draft_meta_info.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(metaInfoPath.toFile(), metaInfo);

        // 复制其他模板文件
        copyTemplateFiles(draftFolder);

        logger.info("Draft saved to: {}", draftFolder);

        Map<String, Object> result = new HashMap<>();
        result.put("draftId", draftId);
        result.put("draftPath", draftFolder.toString());
        return result;
    }

    @SuppressWarnings("unchecked")
    private void fillTemplate(Map<String, Object> template, ScriptFile script) {
        // 更新基本信息
        template.put("id", script.getDraftId());
        template.put("duration", script.getDuration());
        template.put("fps", (double) script.getFps());

        // 更新画布配置
        Map<String, Object> canvas = (Map<String, Object>) template.get("canvas_config");
        canvas.put("width", script.getWidth());
        canvas.put("height", script.getHeight());

        // 填充素材
        Map<String, Object> materials = (Map<String, Object>) template.get("materials");
        fillMaterials(materials, script.getMaterials());

        // 填充轨道
        List<Map<String, Object>> tracks = new ArrayList<>();
        for (Track track : script.getTracks().values()) {
            tracks.add(buildTrack(track));
        }
        template.put("tracks", tracks);
    }

    @SuppressWarnings("unchecked")
    private void fillMaterials(Map<String, Object> materials, ScriptMaterial scriptMaterials) {
        // 视频素材
        List<Map<String, Object>> videos = new ArrayList<>();
        for (VideoMaterial v : scriptMaterials.getVideos()) {
            Map<String, Object> vm = new LinkedHashMap<>();
            vm.put("id", v.getMaterialId());
            vm.put("path", v.getRemoteUrl());
            vm.put("type", v.getMaterialType());
            vm.put("duration", v.getDuration());
            vm.put("width", v.getWidth());
            vm.put("height", v.getHeight());
            videos.add(vm);
        }
        materials.put("videos", videos);

        // 音频素材
        List<Map<String, Object>> audios = new ArrayList<>();
        for (AudioMaterial a : scriptMaterials.getAudios()) {
            Map<String, Object> am = new LinkedHashMap<>();
            am.put("id", a.getMaterialId());
            am.put("path", a.getRemoteUrl());
            am.put("duration", a.getDuration());
            audios.add(am);
        }
        materials.put("audios", audios);
    }

    private Map<String, Object> buildTrack(Track track) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("attribute", 0);
        t.put("flag", 0);
        t.put("id", track.getTrackId());
        t.put("is_default_name", true);
        t.put("name", track.getName());
        t.put("type", track.getTrackType().name().toLowerCase());

        List<Map<String, Object>> segments = new ArrayList<>();
        for (BaseSegment seg : track.getSegments()) {
            segments.add(buildSegment(seg));
        }
        t.put("segments", segments);
        return t;
    }

    private Map<String, Object> buildSegment(BaseSegment seg) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("id", seg.getSegmentId());
        s.put("material_id", seg.getMaterialId());
        s.put("enable_adjust", true);
        s.put("enable_color_curves", true);
        s.put("enable_color_match_adjust", false);
        s.put("enable_color_wheels", true);
        s.put("enable_lut", true);
        s.put("enable_smart_color_adjust", false);

        // 时间范围
        if (seg.getTargetTimerange() != null) {
            s.put("target_timerange", Map.of(
                    "duration", seg.getTargetTimerange().getDuration(),
                    "start", seg.getTargetTimerange().getStart()));
        }
        if (seg.getSourceTimerange() != null) {
            s.put("source_timerange", Map.of(
                    "duration", seg.getSourceTimerange().getDuration(),
                    "start", seg.getSourceTimerange().getStart()));
        }

        // 特定类型属性
        if (seg instanceof VideoSegment vs) {
            s.put("speed", vs.getSpeed());
            s.put("volume", vs.getVolume());
        } else if (seg instanceof AudioSegment as) {
            s.put("speed", as.getSpeed());
            s.put("volume", as.getVolume());
        } else if (seg instanceof TextSegment ts) {
            s.put("content", ts.getText());
            Map<String, Object> style = new LinkedHashMap<>();
            style.put("color", ts.getFontColor());
            style.put("size", ts.getFontSize());
            s.put("style", style);
        }

        return s;
    }

    private Map<String, Object> buildMetaInfo(ScriptFile script, String draftPath) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("draft_id", script.getDraftId());
        meta.put("draft_name", "Autoclip Draft");
        meta.put("draft_fold_path", draftPath);
        meta.put("draft_cover", "");
        meta.put("tm_draft_create", System.currentTimeMillis() * 1000);
        meta.put("tm_draft_modified", System.currentTimeMillis() * 1000);
        meta.put("tm_duration", script.getDuration());
        meta.put("draft_materials", new ArrayList<>());
        return meta;
    }

    private void copyTemplateFiles(Path draftFolder) throws IOException {
        String[] files = { "draft_settings", "draft_agency_config.json" };
        for (String file : files) {
            try {
                ClassPathResource resource = new ClassPathResource("autoclip-template/" + file);
                if (resource.exists()) {
                    Files.copy(resource.getInputStream(), draftFolder.resolve(file));
                }
            } catch (Exception e) {
                logger.warn("Failed to copy template file: {}", file);
            }
        }
    }
}
