package com.qingcloud.mcp.suno.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingcloud.mcp.suno.browser.CaptchaTokenExtractor;
import com.qingcloud.mcp.suno.common.SunoConstants;
import com.qingcloud.mcp.suno.config.SunoProperties;
import com.qingcloud.mcp.suno.dto.AudioClipResponse;
import com.qingcloud.mcp.suno.dto.GenerateRequest;
import com.qingcloud.mcp.suno.exception.SunoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Suno API 核心服务
 * 仅在配置了 suno.cookie 时启用
 * 
 * @author qingcloud-mcp
 */
@Service
@ConditionalOnProperty(name = "suno.cookie")
public class SunoApiService {

    private static final Logger logger = LoggerFactory.getLogger(SunoApiService.class);

    private final WebClient sunoClient;
    private final SunoAuthenticator authenticator;
    private final SunoProperties sunoProperties;
    private final ObjectMapper objectMapper;

    public SunoApiService(SunoAuthenticator authenticator,
            SunoProperties sunoProperties,
            ObjectMapper objectMapper) {
        this.authenticator = authenticator;
        this.sunoProperties = sunoProperties;
        this.objectMapper = objectMapper;

        // 初始化认证
        authenticator.initialize();

        // 创建 Suno API 客户端
        this.sunoClient = WebClient.builder()
                .baseUrl(SunoConstants.BASE_URL)
                .defaultHeader("User-Agent", "Mozilla/5.0")
                .defaultHeader("Device-Id", "\"" + authenticator.getDeviceId() + "\"")
                .defaultHeader("x-suno-client", "Android prerelease-4nt180t 1.0.42")
                .build();
    }

    /**
     * 生成音乐 (简单模式)
     */
    public List<AudioClipResponse> generate(GenerateRequest request) {
        logger.info("Generating music with prompt: {}", request.getPrompt());

        // 刷新 Token
        authenticator.refreshJwt();

        // 构建请求体
        Map<String, Object> payload = new HashMap<>();
        payload.put("gpt_description_prompt", request.getPrompt());
        payload.put("make_instrumental", request.getMakeInstrumental());
        payload.put("mv", request.getModel() != null ? request.getModel() : SunoConstants.DEFAULT_MODEL);
        payload.put("generation_type", "TEXT");

        // TODO: 实现 CAPTCHA 处理
        payload.put("token", null); // 暂时设为 null

        return executeGenerate(payload, request.getWaitAudio());
    }

    /**
     * 生成音乐 (自定义模式)
     */
    public List<AudioClipResponse> customGenerate(GenerateRequest request) {
        logger.info("Custom generating music: {}", request.getTitle());

        // 刷新 Token
        authenticator.refreshJwt();

        // 构建请求体
        Map<String, Object> payload = new HashMap<>();
        payload.put("prompt", request.getPrompt());
        payload.put("tags", request.getTags());
        payload.put("title", request.getTitle());
        payload.put("make_instrumental", request.getMakeInstrumental());
        payload.put("mv", request.getModel() != null ? request.getModel() : SunoConstants.DEFAULT_MODEL);
        payload.put("generation_type", "TEXT");

        if (request.getNegativeTags() != null) {
            payload.put("negative_tags", request.getNegativeTags());
        }

        // TODO: 实现 CAPTCHA 处理
        payload.put("token", null);

        return executeGenerate(payload, request.getWaitAudio());
    }

    /**
     * 执行生成请求
     */
    private List<AudioClipResponse> executeGenerate(Map<String, Object> payload, Boolean waitAudio) {
        try {
            String response = sunoClient.post()
                    .uri(SunoConstants.Endpoints.GENERATE_V2)
                    .header("Authorization", "Bearer " + authenticator.getJwtToken())
                    .header("Cookie", buildCookieHeader())
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 解析响应
            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode clipsNode = jsonNode.get("clips");

            List<AudioClipResponse> clips = new ArrayList<>();
            if (clipsNode != null && clipsNode.isArray()) {
                for (JsonNode clipNode : clipsNode) {
                    AudioClipResponse clip = objectMapper.treeToValue(clipNode, AudioClipResponse.class);
                    clips.add(clip);
                }
            }

            // 如果需要等待音频生成完成
            if (Boolean.TRUE.equals(waitAudio)) {
                return waitForCompletion(clips.stream()
                        .map(AudioClipResponse::getId)
                        .collect(Collectors.toList()));
            }

            return clips;

        } catch (Exception e) {
            logger.error("Failed to generate music", e);
            throw new SunoException("Music generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * 等待音频生成完成
     */
    private List<AudioClipResponse> waitForCompletion(List<String> ids) {
        logger.info("Waiting for audio generation to complete...");

        long startTime = System.currentTimeMillis();
        int maxWaitTime = SunoConstants.Timeouts.AUDIO_GENERATION_MAX;

        while (System.currentTimeMillis() - startTime < maxWaitTime) {
            try {
                Thread.sleep(5000); // 每 5 秒轮询一次

                List<AudioClipResponse> clips = getClips(String.join(",", ids), null);

                boolean allComplete = clips.stream()
                        .allMatch(clip -> "streaming".equals(clip.getStatus()) ||
                                "complete".equals(clip.getStatus()));

                if (allComplete) {
                    logger.info("Audio generation completed");
                    return clips;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SunoException("Wait interrupted", e);
            }
        }

        logger.warn("Audio generation timeout");
        return getClips(String.join(",", ids), null);
    }

    /**
     * 获取音频信息
     */
    public List<AudioClipResponse> getClips(String ids, Integer page) {
        authenticator.refreshJwt();

        try {
            String uri = SunoConstants.Endpoints.FEED_V2;
            if (ids != null && !ids.isEmpty()) {
                uri += "?ids=" + ids;
            }
            if (page != null) {
                uri += (ids != null ? "&" : "?") + "page=" + page;
            }

            String response = sunoClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + authenticator.getJwtToken())
                    .header("Cookie", buildCookieHeader())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode clipsNode = jsonNode.get("clips");

            List<AudioClipResponse> clips = new ArrayList<>();
            if (clipsNode != null && clipsNode.isArray()) {
                for (JsonNode clipNode : clipsNode) {
                    AudioClipResponse clip = objectMapper.treeToValue(clipNode, AudioClipResponse.class);
                    clips.add(clip);
                }
            }

            return clips;

        } catch (Exception e) {
            logger.error("Failed to get clips", e);
            throw new SunoException("Failed to get clips: " + e.getMessage(), e);
        }
    }

    /**
     * 获取音乐 (by IDs)
     */
    public List<AudioClipResponse> getMusic(String ids) {
        return getClips(ids, null);
    }

    /**
     * 获取音乐列表 (分页)
     */
    public List<AudioClipResponse> getMusicList(Integer page) {
        return getClips(null, page);
    }

    /**
     * 获取配额信息
     */
    public Map<String, Object> getCredits() {
        authenticator.refreshJwt();

        try {
            String response = sunoClient.get()
                    .uri(SunoConstants.Endpoints.BILLING_INFO)
                    .header("Authorization", "Bearer " + authenticator.getJwtToken())
                    .header("Cookie", buildCookieHeader())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode jsonNode = objectMapper.readTree(response);

            Map<String, Object> credits = new HashMap<>();
            credits.put("credits_left", jsonNode.get("total_credits_left").asInt());
            credits.put("period", jsonNode.get("period").asText());
            credits.put("monthly_limit", jsonNode.get("monthly_limit").asInt());
            credits.put("monthly_usage", jsonNode.get("monthly_usage").asInt());

            return credits;

        } catch (Exception e) {
            logger.error("Failed to get credits", e);
            throw new SunoException("Failed to get credits: " + e.getMessage(), e);
        }
    }

    /**
     * 获取配额 (别名)
     */
    public Map<String, Object> getQuota() {
        return getCredits();
    }

    /**
     * 构建 Cookie Header
     */
    private String buildCookieHeader() {
        return authenticator.getCookieMap().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("; "));
    }
}
