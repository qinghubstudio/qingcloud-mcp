package com.qingcloud.mcp.suno.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingcloud.mcp.suno.dto.AudioClipResponse;
import com.qingcloud.mcp.suno.service.SunoApiService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Suno 获取音乐工具工厂
 * 
 * @author qingcloud-mcp
 */
public class SunoGetMusicToolFactory {

    private static final Logger logger = LoggerFactory.getLogger(SunoGetMusicToolFactory.class);

    /**
     * 创建获取音乐工具
     */
    public static McpServerFeatures.SyncToolSpecification create(SunoApiService sunoApiService,
            ObjectMapper objectMapper) {
        Tool getMusicTool = new Tool(
                "suno_get_music",
                "Get music information by IDs or browse user's music feed",
                null,
                new JsonSchema("object",
                        Map.of(
                                "ids", Map.of(
                                        "type", "string",
                                        "description", "Comma-separated music IDs to retrieve (optional)"),
                                "page", Map.of(
                                        "type", "integer",
                                        "description", "Page number for pagination (default: 1)")),
                        List.of(),
                        null, null, null),
                null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(getMusicTool)
                .callHandler((exchange, request) -> {
                    try {
                        logger.info("=== Suno Get Music Tool Called ===");

                        Map<String, Object> arguments = (Map<String, Object>) request.arguments();

                        String ids = (String) arguments.get("ids");
                        Integer page = arguments.containsKey("page") ? ((Number) arguments.get("page")).intValue()
                                : null;

                        // 调用服务
                        List<AudioClipResponse> clips = sunoApiService.getClips(ids, page);

                        // 构建响应
                        String result = objectMapper.writeValueAsString(clips);

                        return CallToolResult.builder()
                                .content(List.of(new TextContent(result)))
                                .isError(false)
                                .build();

                    } catch (Exception e) {
                        logger.error("Suno get music tool failed", e);
                        return CallToolResult.builder()
                                .content(List.of(new TextContent("{\"error\":\"" + e.getMessage() + "\"}")))
                                .isError(true)
                                .build();
                    }
                })
                .build();
    }
}
