package com.qingcloud.mcp.autoclip.tools;

import com.qingcloud.mcp.autoclip.model.MaskType;
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
 * 设置蒙版工具
 */
public class SetMaskToolFactory {

    private static final Logger logger = LoggerFactory.getLogger(SetMaskToolFactory.class);

    public static McpServerFeatures.SyncToolSpecification create(DraftService draftService) {
        String maskTypes = Arrays.stream(MaskType.values())
                .map(t -> t.name().toLowerCase())
                .collect(Collectors.joining(","));

        Tool tool = new Tool(
                "autoclip_setMask",
                "为视频片段设置蒙版效果。可用类型: " + maskTypes,
                null,
                new JsonSchema("object",
                        Map.of(
                                "draftId", Map.of("type", "string", "description", "草稿ID"),
                                "segmentId", Map.of("type", "string", "description", "片段ID"),
                                "maskType", Map.of("type", "string", "description", "蒙版类型"),
                                "centerX", Map.of("type", "number", "default", 0.5, "description", "中心X(0-1)"),
                                "centerY", Map.of("type", "number", "default", 0.5, "description", "中心Y(0-1)"),
                                "size", Map.of("type", "number", "default", 1.0, "description", "大小(0-1)"),
                                "feather", Map.of("type", "number", "default", 0, "description", "羽化(0-1)"),
                                "invert", Map.of("type", "boolean", "default", false, "description", "是否反转")),
                        List.of("draftId", "segmentId", "maskType"), null, null, null),
                null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> {
                    try {
                        Map<String, Object> args = request.arguments();
                        String draftId = getString(args, "draftId", null);
                        String segmentId = getString(args, "segmentId", null);
                        String typeStr = getString(args, "maskType", null);

                        if (draftId == null || segmentId == null || typeStr == null) {
                            return errorResult("Missing required parameters");
                        }

                        double centerX = getDouble(args, "centerX", 0.5);
                        double centerY = getDouble(args, "centerY", 0.5);
                        double size = getDouble(args, "size", 1.0);
                        double feather = getDouble(args, "feather", 0);
                        boolean invert = getBoolean(args, "invert", false);

                        // 解析蒙版类型
                        MaskType type;
                        try {
                            type = MaskType.valueOf(typeStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return errorResult("Invalid mask type: " + typeStr);
                        }

                        // 设置蒙版
                        Map<String, Object> result = draftService.setMask(
                                draftId, segmentId, type, centerX, centerY, size, feather, invert);

                        if (result == null) {
                            return errorResult("Draft or segment not found");
                        }

                        String json = String.format(
                                "{\"success\":true,\"draftId\":\"%s\",\"segmentId\":\"%s\",\"mask\":\"%s\"}",
                                draftId, segmentId, type.name());

                        logger.info("Set mask {} on segment: {}", type, segmentId);

                        return CallToolResult.builder()
                                .content(List.of(new TextContent(json)))
                                .isError(false)
                                .build();

                    } catch (Exception e) {
                        logger.error("Failed to set mask", e);
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
