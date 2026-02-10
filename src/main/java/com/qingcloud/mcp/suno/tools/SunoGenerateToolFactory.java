package com.qingcloud.mcp.suno.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingcloud.mcp.suno.dto.AudioClipResponse;
import com.qingcloud.mcp.suno.dto.GenerateRequest;
import com.qingcloud.mcp.suno.service.SunoApiService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Suno 音乐生成工具工厂
 * 
 * @author qingcloud-mcp
 */
public class SunoGenerateToolFactory {

    private static final Logger logger = LoggerFactory.getLogger(SunoGenerateToolFactory.class);

    /**
     * 创建音乐生成工具
     */
    public static McpServerFeatures.SyncToolSpecification create(SunoApiService sunoApiService,
            ObjectMapper objectMapper) {
        Tool generateTool = new Tool(
                "suno_generate_music",
                "Generate music using Suno AI based on a text prompt",
                null,
                new JsonSchema("object",
                        Map.of(
                                "prompt", Map.of(
                                        "type", "string",
                                        "description",
                                        "Text description of the desired music (e.g., 'upbeat jazz about coding')"),
                                "make_instrumental", Map.of(
                                        "type", "boolean",
                                        "description", "Whether to generate instrumental music without vocals"),
                                "model", Map.of(
                                        "type", "string",
                                        "description", "Model version to use (default: chirp-v3.5)"),
                                "wait_audio", Map.of(
                                        "type", "boolean",
                                        "description", "Whether to wait until audio generation is complete")),
                        List.of("prompt"),
                        null, null, null),
                null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(generateTool)
                .callHandler((exchange, request) -> {
                    try {
                        logger.info("=== Suno Generate Music Tool Called ===");

                        Map<String, Object> arguments = (Map<String, Object>) request.arguments();

                        // 解析参数
                        GenerateRequest genRequest = new GenerateRequest();
                        genRequest.setPrompt((String) arguments.get("prompt"));
                        genRequest.setMakeInstrumental((Boolean) arguments.getOrDefault("make_instrumental", false));
                        genRequest.setModel((String) arguments.getOrDefault("model", "chirp-v3.5"));
                        genRequest.setWaitAudio((Boolean) arguments.getOrDefault("wait_audio", false));

                        // 调用服务
                        List<AudioClipResponse> clips = sunoApiService.generate(genRequest);

                        // 构建响应
                        String result = objectMapper.writeValueAsString(clips);

                        return CallToolResult.builder()
                                .content(List.of(new TextContent(result)))
                                .isError(false)
                                .build();

                    } catch (Exception e) {
                        logger.error("Suno generate music tool failed", e);
                        return CallToolResult.builder()
                                .content(List.of(new TextContent("{\"error\":\"" + e.getMessage() + "\"}")))
                                .isError(true)
                                .build();
                    }
                })
                .build();
    }
}
