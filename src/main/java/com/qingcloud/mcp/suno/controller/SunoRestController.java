package com.qingcloud.mcp.suno.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingcloud.mcp.suno.dto.AudioClipResponse;
import com.qingcloud.mcp.suno.dto.GenerateRequest;
import com.qingcloud.mcp.suno.dto.openai.ChatCompletionRequest;
import com.qingcloud.mcp.suno.dto.openai.ChatCompletionResponse;
import com.qingcloud.mcp.suno.dto.openai.ChatMessage;
import com.qingcloud.mcp.suno.dto.rest.CustomGenerateRestRequest;
import com.qingcloud.mcp.suno.dto.rest.GenerateRestRequest;
import com.qingcloud.mcp.suno.service.SunoApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Suno REST API Controller
 * 提供 RESTful API 端点访问 Suno 服务
 * 仅在 SunoApiService 可用时启用
 * 
 * @author qingcloud-mcp
 */
@RestController
@RequestMapping("/api")
@ConditionalOnBean(SunoApiService.class)
public class SunoRestController {

    private static final Logger logger = LoggerFactory.getLogger(SunoRestController.class);

    private final SunoApiService sunoApiService;
    private final ObjectMapper objectMapper;

    public SunoRestController(SunoApiService sunoApiService, ObjectMapper objectMapper) {
        this.sunoApiService = sunoApiService;
        this.objectMapper = objectMapper;
    }

    /**
     * 生成音乐 (简单模式)
     * POST /api/generate
     */
    @PostMapping("/generate")
    public List<AudioClipResponse> generate(@RequestBody GenerateRestRequest request) {
        logger.info("REST API: generate music with prompt: {}", request.getPrompt());

        GenerateRequest genRequest = new GenerateRequest();
        genRequest.setPrompt(request.getPrompt());
        genRequest.setMakeInstrumental(request.getMakeInstrumental());
        genRequest.setModel(request.getModel());
        genRequest.setWaitAudio(request.getWaitAudio());

        return sunoApiService.generate(genRequest);
    }

    /**
     * 自定义生成音乐
     * POST /api/custom_generate
     */
    @PostMapping("/custom_generate")
    public List<AudioClipResponse> customGenerate(@RequestBody CustomGenerateRestRequest request) {
        logger.info("REST API: custom generate music: {}", request.getTitle());

        GenerateRequest genRequest = new GenerateRequest();
        genRequest.setPrompt(request.getPrompt());
        genRequest.setTags(request.getTags());
        genRequest.setTitle(request.getTitle());
        genRequest.setMakeInstrumental(request.getMakeInstrumental());
        genRequest.setNegativeTags(request.getNegativeTags());
        genRequest.setModel(request.getModel());
        genRequest.setWaitAudio(request.getWaitAudio());

        return sunoApiService.customGenerate(genRequest);
    }

    /**
     * 获取音乐列表或详情
     * GET /api/get
     * GET /api/get?ids=id1,id2
     */
    @GetMapping("/get")
    public List<AudioClipResponse> getMusic(
            @RequestParam(required = false) String ids,
            @RequestParam(defaultValue = "1") Integer page) {

        if (ids != null && !ids.isEmpty()) {
            logger.info("REST API: get music by ids: {}", ids);
            return sunoApiService.getMusic(ids);
        } else {
            logger.info("REST API: get music list, page: {}", page);
            return sunoApiService.getMusicList(page);
        }
    }

    /**
     * 获取配额信息
     * GET /api/get_limit
     */
    @GetMapping("/get_limit")
    public Map<String, Object> getLimit() {
        logger.info("REST API: get quota limit");
        return sunoApiService.getQuota();
    }

    /**
     * OpenAI 兼容端点
     * POST /v1/chat/completions
     */
    @PostMapping("/v1/chat/completions")
    public ChatCompletionResponse chatCompletions(@RequestBody ChatCompletionRequest request) {
        logger.info("REST API: OpenAI chat completions");

        // 从 messages 中提取 prompt
        String prompt = extractPromptFromMessages(request.getMessages());

        // 调用生成 API
        GenerateRequest genRequest = new GenerateRequest();
        genRequest.setPrompt(prompt);
        genRequest.setModel(request.getModel());
        genRequest.setWaitAudio(true); // OpenAI 格式默认等待完成

        List<AudioClipResponse> clips = sunoApiService.generate(genRequest);

        // 转换为 OpenAI 格式
        return convertToOpenAIFormat(clips, request.getModel());
    }

    /**
     * 从消息列表中提取 prompt
     */
    private String extractPromptFromMessages(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        // 获取最后一条用户消息
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if ("user".equals(msg.getRole())) {
                return msg.getContent();
            }
        }

        return messages.get(messages.size() - 1).getContent();
    }

    /**
     * 转换为 OpenAI 格式响应
     */
    private ChatCompletionResponse convertToOpenAIFormat(List<AudioClipResponse> clips, String model) {
        ChatCompletionResponse response = new ChatCompletionResponse();
        response.setId("chatcmpl-suno-" + System.currentTimeMillis());
        response.setCreated(System.currentTimeMillis() / 1000);
        response.setModel(model != null ? model : "suno-v3.5");

        // 构建响应内容
        StringBuilder content = new StringBuilder();
        content.append("🎵 Generated ").append(clips.size()).append(" music track(s):\n\n");

        for (int i = 0; i < clips.size(); i++) {
            AudioClipResponse clip = clips.get(i);
            content.append(i + 1).append(". **").append(clip.getTitle()).append("**\n");
            content.append("   - ID: `").append(clip.getId()).append("`\n");
            content.append("   - Status: ").append(clip.getStatus()).append("\n");
            if (clip.getAudioUrl() != null) {
                content.append("   - Audio: ").append(clip.getAudioUrl()).append("\n");
            }
            if (clip.getVideoUrl() != null) {
                content.append("   - Video: ").append(clip.getVideoUrl()).append("\n");
            }
            content.append("\n");
        }

        // 构建 choice
        ChatCompletionResponse.Choice choice = new ChatCompletionResponse.Choice();
        choice.setIndex(0);
        choice.setMessage(new ChatMessage("assistant", content.toString()));
        choice.setFinishReason("stop");

        response.setChoices(List.of(choice));

        // 构建 usage (估算)
        ChatCompletionResponse.Usage usage = new ChatCompletionResponse.Usage();
        usage.setPromptTokens(50);
        usage.setCompletionTokens(100);
        usage.setTotalTokens(150);
        response.setUsage(usage);

        return response;
    }
}
