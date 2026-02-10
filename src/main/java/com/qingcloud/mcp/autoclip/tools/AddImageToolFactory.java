package com.qingcloud.mcp.autoclip.tools;

import com.qingcloud.mcp.autoclip.service.DraftService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 添加图片工具
 */
public class AddImageToolFactory {

    private static final Logger logger = LoggerFactory.getLogger(AddImageToolFactory.class);

    public static McpServerFeatures.SyncToolSpecification create(DraftService draftService) {
        Tool tool = new Tool(
                "autoclip_addImage",
                "添加图片到草稿",
                null,
                new JsonSchema("object",
                        Map.of(
                                "imageUrl", Map.of("type", "string", "description", "图片URL或本地路径"),
                                "draftId", Map.of("type", "string", "description", "草稿ID"),
                                "start", Map.of("type", "number", "default", 0, "description", "开始时间（秒）"),
                                "end", Map.of("type", "number", "default", 3.0, "description", "结束时间（秒）"),
                                "trackName", Map.of("type", "string", "default", "main", "description", "轨道名称")),
                        List.of("imageUrl"), null, null, null),
                null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> {
                    try {
                        Map<String, Object> args = request.arguments();
                        String imageUrl = getString(args, "imageUrl", null);

                        if (imageUrl == null || imageUrl.isEmpty()) {
                            return errorResult("Missing required parameter: imageUrl");
                        }

                        String draftId = getString(args, "draftId", null);
                        double start = getDouble(args, "start", 0);
                        double end = getDouble(args, "end", 3.0);
                        int width = getInt(args, "width", 1080);
                        int height = getInt(args, "height", 1920);
                        String trackName = getString(args, "trackName", "main");

                        Map<String, Object> result = draftService.addImage(
                                draftId, imageUrl, start, end, width, height, trackName);

                        String json = String.format(
                                "{\"success\":true,\"draftId\":\"%s\",\"segmentId\":\"%s\"}",
                                result.get("draftId"), result.get("segmentId"));

                        logger.info("Added image to draft: {}", result.get("draftId"));

                        return CallToolResult.builder()
                                .content(List.of(new TextContent(json)))
                                .isError(false)
                                .build();

                    } catch (Exception e) {
                        logger.error("Failed to add image", e);
                        return errorResult(e.getMessage());
                    }
                })
                .build();
    }

    private static String getString(Map<String, Object> args, String key, String def) {
        if (args == null)
            return def;
        Object val = args.get(key);
        return val != null ? val.toString() : def;
    }

    private static int getInt(Map<String, Object> args, String key, int def) {
        if (args == null)
            return def;
        Object val = args.get(key);
        return val instanceof Number ? ((Number) val).intValue() : def;
    }

    private static double getDouble(Map<String, Object> args, String key, double def) {
        if (args == null)
            return def;
        Object val = args.get(key);
        return val instanceof Number ? ((Number) val).doubleValue() : def;
    }

    private static CallToolResult errorResult(String error) {
        return CallToolResult.builder()
                .content(List.of(new TextContent("{\"success\":false,\"error\":\"" + error + "\"}")))
                .isError(true)
                .build();
    }
}
