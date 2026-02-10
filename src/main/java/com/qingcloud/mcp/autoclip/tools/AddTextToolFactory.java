package com.qingcloud.mcp.autoclip.tools;

import com.qingcloud.mcp.autoclip.service.DraftService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 添加文字工具
 */
public class AddTextToolFactory {

    private static final Logger logger = LoggerFactory.getLogger(AddTextToolFactory.class);

    public static McpServerFeatures.SyncToolSpecification create(DraftService draftService) {
        Tool tool = new Tool(
                "autoclip_addText",
                "添加文字到草稿",
                null,
                new JsonSchema("object",
                        Map.of(
                                "text", Map.of("type", "string", "description", "文字内容"),
                                "draftId", Map.of("type", "string", "description", "草稿ID"),
                                "startTime", Map.of("type", "number", "description", "开始时间（秒）"),
                                "endTime", Map.of("type", "number", "description", "结束时间（秒）"),
                                "fontColor", Map.of("type", "string", "default", "#ffffff", "description", "字体颜色"),
                                "fontSize", Map.of("type", "number", "default", 8.0, "description", "字体大小"),
                                "transformX", Map.of("type", "number", "default", 0, "description", "X位置(-1到1)"),
                                "transformY", Map.of("type", "number", "default", -0.8, "description", "Y位置(-1到1)")),
                        List.of("text", "startTime", "endTime"), null, null, null),
                null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> {
                    try {
                        Map<String, Object> args = request.arguments();
                        String text = getString(args, "text", null);

                        if (text == null || text.isEmpty()) {
                            return errorResult("Missing required parameter: text");
                        }

                        String draftId = getString(args, "draftId", null);
                        double startTime = getDouble(args, "startTime", 0);
                        double endTime = getDouble(args, "endTime", 3.0);
                        String fontColor = getString(args, "fontColor", "#ffffff");
                        double fontSize = getDouble(args, "fontSize", 8.0);
                        double transformX = getDouble(args, "transformX", 0);
                        double transformY = getDouble(args, "transformY", -0.8);
                        int width = getInt(args, "width", 1080);
                        int height = getInt(args, "height", 1920);
                        String trackName = getString(args, "trackName", "text_main");

                        Map<String, Object> result = draftService.addText(
                                draftId, text, startTime, endTime,
                                fontColor, fontSize, transformX, transformY,
                                width, height, trackName);

                        String json = String.format(
                                "{\"success\":true,\"draftId\":\"%s\",\"segmentId\":\"%s\"}",
                                result.get("draftId"), result.get("segmentId"));

                        logger.info("Added text to draft: {}", result.get("draftId"));

                        return CallToolResult.builder()
                                .content(List.of(new TextContent(json)))
                                .isError(false)
                                .build();

                    } catch (Exception e) {
                        logger.error("Failed to add text", e);
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
