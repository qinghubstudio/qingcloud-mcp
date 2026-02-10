package com.qingcloud.mcp.autoclip.tools;

import com.qingcloud.mcp.autoclip.model.ScriptFile;
import com.qingcloud.mcp.autoclip.service.DraftService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 创建草稿工具
 */
public class CreateDraftToolFactory {

    private static final Logger logger = LoggerFactory.getLogger(CreateDraftToolFactory.class);

    public static McpServerFeatures.SyncToolSpecification create(DraftService draftService) {
        Tool tool = new Tool(
                "autoclip_createDraft",
                "创建新的剪辑草稿",
                null,
                new JsonSchema("object",
                        Map.of(
                                "width", Map.of("type", "integer", "default", 1080, "description", "视频宽度"),
                                "height", Map.of("type", "integer", "default", 1920, "description", "视频高度")),
                        List.of(), null, null, null),
                null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> {
                    try {
                        Map<String, Object> args = request.arguments();
                        int width = getInt(args, "width", 1080);
                        int height = getInt(args, "height", 1920);

                        ScriptFile script = draftService.createDraft(width, height);

                        String result = String.format(
                                "{\"success\":true,\"draftId\":\"%s\",\"width\":%d,\"height\":%d}",
                                script.getDraftId(), script.getWidth(), script.getHeight());

                        logger.info("Created draft: {}", script.getDraftId());

                        return CallToolResult.builder()
                                .content(List.of(new TextContent(result)))
                                .isError(false)
                                .build();

                    } catch (Exception e) {
                        logger.error("Failed to create draft", e);
                        return CallToolResult.builder()
                                .content(List.of(new TextContent(
                                        "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}")))
                                .isError(true)
                                .build();
                    }
                })
                .build();
    }

    private static int getInt(Map<String, Object> args, String key, int defaultValue) {
        if (args == null)
            return defaultValue;
        Object val = args.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return defaultValue;
    }
}
