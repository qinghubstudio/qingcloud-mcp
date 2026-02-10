package com.qingcloud.mcp.autoclip.tools;

import com.qingcloud.mcp.autoclip.model.TransitionType;
import com.qingcloud.mcp.autoclip.service.DraftService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 设置转场效果工具
 */
public class SetTransitionToolFactory {

    private static final Logger logger = LoggerFactory.getLogger(SetTransitionToolFactory.class);

    public static McpServerFeatures.SyncToolSpecification create(DraftService draftService) {
        String transitionTypes = Arrays.stream(TransitionType.values())
                .map(t -> t.name().toLowerCase())
                .collect(Collectors.joining(","));

        Tool tool = new Tool(
                "autoclip_setTransition",
                "为视频片段设置转场效果。可用类型: " + transitionTypes,
                null,
                new JsonSchema("object",
                        Map.of(
                                "draftId", Map.of("type", "string", "description", "草稿ID"),
                                "segmentId", Map.of("type", "string", "description", "片段ID"),
                                "transitionType", Map.of("type", "string", "description", "转场类型"),
                                "duration", Map.of("type", "number", "default", 0.5, "description", "转场时长（秒）")),
                        List.of("draftId", "segmentId", "transitionType"), null, null, null),
                null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> {
                    try {
                        Map<String, Object> args = request.arguments();
                        String draftId = getString(args, "draftId", null);
                        String segmentId = getString(args, "segmentId", null);
                        String typeStr = getString(args, "transitionType", null);

                        if (draftId == null || segmentId == null || typeStr == null) {
                            return errorResult("Missing required parameters");
                        }

                        double duration = getDouble(args, "duration", 0.5);

                        // 解析转场类型
                        TransitionType type;
                        try {
                            type = TransitionType.valueOf(typeStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return errorResult("Invalid transition type: " + typeStr);
                        }

                        // 设置转场
                        Map<String, Object> result = draftService.setTransition(
                                draftId, segmentId, type, duration);

                        if (result == null) {
                            return errorResult("Draft or segment not found");
                        }

                        String json = String.format(
                                "{\"success\":true,\"draftId\":\"%s\",\"segmentId\":\"%s\",\"transition\":\"%s\"}",
                                draftId, segmentId, type.name());

                        logger.info("Set transition {} on segment: {}", type, segmentId);

                        return CallToolResult.builder()
                                .content(List.of(new TextContent(json)))
                                .isError(false)
                                .build();

                    } catch (Exception e) {
                        logger.error("Failed to set transition", e);
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

    private static CallToolResult errorResult(String error) {
        return CallToolResult.builder()
                .content(List.of(new TextContent("{\"success\":false,\"error\":\"" + error + "\"}")))
                .isError(true)
                .build();
    }
}
