package com.qingcloud.mcp.autoclip.tools;

import com.qingcloud.mcp.autoclip.service.DraftService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 设置速度工具
 */
public class SetSpeedToolFactory {

    private static final Logger logger = LoggerFactory.getLogger(SetSpeedToolFactory.class);

    public static McpServerFeatures.SyncToolSpecification create(DraftService draftService) {
        Tool tool = new Tool(
                "autoclip_setSpeed",
                "调整视频/音频片段的播放速度（0.1-100倍速）",
                null,
                new JsonSchema("object",
                        Map.of(
                                "draftId", Map.of("type", "string", "description", "草稿ID"),
                                "segmentId", Map.of("type", "string", "description", "片段ID"),
                                "speed", Map.of("type", "number", "description", "播放速度(0.1-100)"),
                                "keepPitch", Map.of("type", "boolean", "default", true, "description", "是否保持音调")),
                        List.of("draftId", "segmentId", "speed"), null, null, null),
                null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> {
                    try {
                        Map<String, Object> args = request.arguments();
                        String draftId = getString(args, "draftId", null);
                        String segmentId = getString(args, "segmentId", null);

                        if (draftId == null || segmentId == null) {
                            return errorResult("Missing required parameters");
                        }

                        double speed = getDouble(args, "speed", 1.0);
                        boolean keepPitch = getBoolean(args, "keepPitch", true);

                        // 验证速度范围
                        if (speed < 0.1 || speed > 100) {
                            return errorResult("Speed must be between 0.1 and 100");
                        }

                        Map<String, Object> result = draftService.setSpeed(
                                draftId, segmentId, speed, keepPitch);

                        if (result == null) {
                            return errorResult("Draft or segment not found");
                        }

                        String json = String.format(
                                "{\"success\":true,\"draftId\":\"%s\",\"segmentId\":\"%s\",\"speed\":%.2f}",
                                draftId, segmentId, speed);

                        logger.info("Set speed {}x on segment: {}", speed, segmentId);

                        return CallToolResult.builder()
                                .content(List.of(new TextContent(json)))
                                .isError(false)
                                .build();

                    } catch (Exception e) {
                        logger.error("Failed to set speed", e);
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

    private static double getDouble(Map<String, Object> args, String key, double def) {
        if (args == null)
            return def;
        Object val = args.get(key);
        return val instanceof Number ? ((Number) val).doubleValue() : def;
    }

    private static boolean getBoolean(Map<String, Object> args, String key, boolean def) {
        if (args == null)
            return def;
        Object val = args.get(key);
        return val instanceof Boolean ? (Boolean) val : def;
    }

    private static CallToolResult errorResult(String error) {
        return CallToolResult.builder()
                .content(List.of(new TextContent("{\"success\":false,\"error\":\"" + error + "\"}")))
                .isError(true)
                .build();
    }
}
