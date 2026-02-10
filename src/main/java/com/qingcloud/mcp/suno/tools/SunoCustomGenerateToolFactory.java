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
 * Suno 自定义音乐生成工具工厂
 * 
 * @author qingcloud-mcp
 */
public class SunoCustomGenerateToolFactory {

    private static final Logger logger = LoggerFactory.getLogger(SunoCustomGenerateToolFactory.class);

    /**
     * 创建自定义音乐生成工具
     */
    public static McpServerFeatures.SyncToolSpecification create(SunoApiService sunoApiService,
            ObjectMapper objectMapper) {
        Tool customGenerateTool = new Tool(
                "suno_custom_generate",
                "Generate custom music with specific lyrics, style tags, and title",
                null,
                new JsonSchema("object",
                        Map.of(
                                "prompt", Map.of(
                                        "type", "string",
                                        "description", "Custom lyrics or vocal content for the song"),
                                "tags", Map.of(
                                        "type", "string",
                                        "description", "Musical style tags (e.g., 'jazz, piano, slow tempo')"),
                                "title", Map.of(
                                        "type", "string",
                                        "description", "Title for the generated song"),
                                "make_instrumental", Map.of(
                                        "type", "boolean",
                                        "description", "Whether to generate instrumental version"),
                                "negative_tags", Map.of(
                                        "type", "string",
                                        "description", "Style elements to avoid (optional)"),
                                "wait_audio", Map.of(
                                        "type", "boolean",
                                        "description", "Whether to wait until generation completes")),
                        List.of("prompt", "tags", "title"),
                        null, null, null),
                null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(customGenerateTool)
                .callHandler((exchange, request) -> {
                    try {
                        logger.info("=== Suno Custom Generate Tool Called ===");

                        Map<String, Object> arguments = (Map<String, Object>) request.arguments();

                        // 解析参数
                        GenerateRequest genRequest = new GenerateRequest();
                        genRequest.setPrompt((String) arguments.get("prompt"));
                        genRequest.setTags((String) arguments.get("tags"));
                        genRequest.setTitle((String) arguments.get("title"));
                        genRequest.setMakeInstrumental((Boolean) arguments.getOrDefault("make_instrumental", false));
                        genRequest.setNegativeTags((String) arguments.get("negative_tags"));
                        genRequest.setWaitAudio((Boolean) arguments.getOrDefault("wait_audio", false));

                        // 调用服务
                        List<AudioClipResponse> clips = sunoApiService.customGenerate(genRequest);

                        // 构建响应
                        String result = objectMapper.writeValueAsString(clips);

                        return CallToolResult.builder()
                                .content(List.of(new TextContent(result)))
                                .isError(false)
                                .build();

                    } catch (Exception e) {
                        logger.error("Suno custom generate tool failed", e);
                        return CallToolResult.builder()
                                .content(List.of(new TextContent("{\"error\":\"" + e.getMessage() + "\"}")))
                                .isError(true)
                                .build();
                    }
                })
                .build();
    }
}
