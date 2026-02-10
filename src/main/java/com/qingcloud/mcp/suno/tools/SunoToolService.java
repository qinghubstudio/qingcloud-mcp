package com.qingcloud.mcp.suno.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingcloud.mcp.suno.dto.AudioClipResponse;
import com.qingcloud.mcp.suno.dto.GenerateRequest;
import com.qingcloud.mcp.suno.service.SunoApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Suno MCP Tools 服务 - Spring AI Tool 注解方式
 * 使用 @Tool 注解实现所有 Suno 音乐生成功能
 * 仅在 SunoApiService 可用时启用
 */
@Service
@ConditionalOnBean(SunoApiService.class)
public class SunoToolService {

    private static final Logger log = LoggerFactory.getLogger(SunoToolService.class);

    @Autowired
    private SunoApiService sunoApiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成音乐
     */
    @Tool(name = "suno_generate_music", description = "Generate music using Suno AI based on a text prompt")
    public String generateMusic(String prompt, Boolean makeInstrumental, String model, Boolean waitAudio) {
        try {
            log.info("=== Suno Generate Music Tool Called ===");

            GenerateRequest genRequest = new GenerateRequest();
            genRequest.setPrompt(prompt);
            genRequest.setMakeInstrumental(makeInstrumental != null ? makeInstrumental : false);
            genRequest.setModel(model != null ? model : "chirp-v3.5");
            genRequest.setWaitAudio(waitAudio != null ? waitAudio : false);

            List<AudioClipResponse> clips = sunoApiService.generate(genRequest);

            return objectMapper.writeValueAsString(clips);

        } catch (Exception e) {
            log.error("Suno generate music tool failed", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 自定义生成音乐
     */
    @Tool(name = "suno_custom_generate", description = "Generate custom music with lyrics and style using Suno AI")
    public String customGenerateMusic(String prompt, String tags, String title, Boolean makeInstrumental, String model,
            Boolean waitAudio) {
        try {
            log.info("=== Suno Custom Generate Music Tool Called ===");

            GenerateRequest genRequest = new GenerateRequest();
            genRequest.setPrompt(prompt);
            genRequest.setTags(tags);
            genRequest.setTitle(title);
            genRequest.setMakeInstrumental(makeInstrumental != null ? makeInstrumental : false);
            genRequest.setModel(model != null ? model : "chirp-v3.5");
            genRequest.setWaitAudio(waitAudio != null ? waitAudio : false);

            List<AudioClipResponse> clips = sunoApiService.customGenerate(genRequest);

            return objectMapper.writeValueAsString(clips);

        } catch (Exception e) {
            log.error("Suno custom generate music tool failed", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 获取音乐详情
     */
    @Tool(name = "suno_get_music", description = "Get music details by audio IDs from Suno AI")
    public String getMusic(String audioIds) {
        try {
            log.info("=== Suno Get Music Tool Called ===");

            if (audioIds == null || audioIds.trim().isEmpty()) {
                return "{\"error\":\"Audio IDs are required\"}";
            }

            // SunoApiService.getMusic 接受逗号分隔的 ID 字符串
            List<AudioClipResponse> clips = sunoApiService.getMusic(audioIds);

            return objectMapper.writeValueAsString(clips);

        } catch (Exception e) {
            log.error("Suno get music tool failed", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 获取配额信息
     */
    @Tool(name = "suno_get_quota", description = "Get current quota information from Suno AI account")
    public String getQuota() {
        try {
            log.info("=== Suno Get Quota Tool Called ===");

            Object quota = sunoApiService.getQuota();

            return objectMapper.writeValueAsString(quota);

        } catch (Exception e) {
            log.error("Suno get quota tool failed", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}
