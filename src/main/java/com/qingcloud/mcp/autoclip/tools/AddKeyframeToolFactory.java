package com.qingcloud.mcp.autoclip.tools;

import com.qingcloud.mcp.autoclip.model.*;
import com.qingcloud.mcp.autoclip.service.DraftService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 添加关键帧工具
 */
public class AddKeyframeToolFactory {

    private static final Logger logger = LoggerFactory.getLogger(AddKeyframeToolFactory.class);

    public static McpServerFeatures.SyncToolSpecification create(DraftService draftService) {
        Tool tool = new Tool(
                "autoclip_addKeyframe",
                "为视频片段添加关键帧动画",
                null,
                new JsonSchema("object",
                        Map.of(
                                "draftId", Map.of("type", "string", "description", "草稿ID"),
                                "segmentId", Map.of("type", "string", "description", "片段ID"),
                                "property",
                                Map.of("type", "string", "description",
                                        "属性(position_x,position_y,scale_x,scale_y,rotation,alpha)"),
                                "time", Map.of("type", "number", "description", "时间点（秒，相对于片段开始）"),
                                "value", Map.of("type", "number", "description", "属性值"),
                                "easing", Map.of("type", "string", "default", "linear", "description", "缓动函数")),
                        List.of("draftId", "segmentId", "property", "time", "value"), null, null, null),
                null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> {
                    try {
                        Map<String, Object> args = request.arguments();
                        String draftId = getString(args, "draftId", null);
                        String segmentId = getString(args, "segmentId", null);
                        String propertyStr = getString(args, "property", null);

                        if (draftId == null || segmentId == null || propertyStr == null) {
                            return errorResult("Missing required parameters");
                        }

                        double time = getDouble(args, "time", 0);
                        double value = getDouble(args, "value", 0);
                        String easing = getString(args, "easing", "linear");

                        // 解析属性类型
                        KeyframeProperty property;
                        try {
                            property = KeyframeProperty.valueOf(propertyStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return errorResult("Invalid property: " + propertyStr);
                        }

                        // 添加关键帧
                        Map<String, Object> result = draftService.addKeyframe(
                                draftId, segmentId, property, time, value, easing);

                        if (result == null) {
                            return errorResult("Draft or segment not found");
                        }

                        String json = String.format(
                                "{\"success\":true,\"draftId\":\"%s\",\"keyframeId\":\"%s\"}",
                                result.get("draftId"), result.get("keyframeId"));

                        logger.info("Added keyframe to segment: {}", segmentId);

                        return CallToolResult.builder()
                                .content(List.of(new TextContent(json)))
                                .isError(false)
                                .build();

                    } catch (Exception e) {
                        logger.error("Failed to add keyframe", e);
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
