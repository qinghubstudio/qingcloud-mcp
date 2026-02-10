package com.qingcloud.mcp.autoclip.tools;

import com.qingcloud.mcp.autoclip.model.FilterType;
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
 * 设置滤镜工具
 */
public class SetFilterToolFactory {

    private static final Logger logger = LoggerFactory.getLogger(SetFilterToolFactory.class);

    public static McpServerFeatures.SyncToolSpecification create(DraftService draftService) {
        String filterTypes = Arrays.stream(FilterType.values())
                .map(t -> t.name().toLowerCase())
                .collect(Collectors.joining(","));

        Tool tool = new Tool(
                "autoclip_setFilter",
                "为视频片段设置滤镜效果。可用类型: " + filterTypes,
                null,
                new JsonSchema("object",
                        Map.of(
                                "draftId", Map.of("type", "string", "description", "草稿ID"),
                                "segmentId", Map.of("type", "string", "description", "片段ID"),
                                "filterType", Map.of("type", "string", "description", "滤镜类型"),
                                "intensity", Map.of("type", "number", "default", 1.0, "description", "滤镜强度(0-1)")),
                        List.of("draftId", "segmentId", "filterType"), null, null, null),
                null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> {
                    try {
                        Map<String, Object> args = request.arguments();
                        String draftId = getString(args, "draftId", null);
                        String segmentId = getString(args, "segmentId", null);
                        String typeStr = getString(args, "filterType", null);
                        double intensity = getDouble(args, "intensity", 1.0);

                        if (draftId == null || segmentId == null || typeStr == null) {
                            return errorResult("Missing required parameters");
                        }

                        FilterType type;
                        try {
                            type = FilterType.valueOf(typeStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return errorResult("Invalid filter type: " + typeStr);
                        }

                        Map<String, Object> result = draftService.setFilter(
                                draftId, segmentId, type, intensity);

                        if (result == null) {
                            return errorResult("Draft or segment not found");
                        }

                        String json = String.format(
                                "{\"success\":true,\"draftId\":\"%s\",\"segmentId\":\"%s\",\"filter\":\"%s\"}",
                                draftId, segmentId, type.name());

                        logger.info("Set filter {} on segment: {}", type, segmentId);

                        return CallToolResult.builder()
                                .content(List.of(new TextContent(json)))
                                .isError(false)
                                .build();

                    } catch (Exception e) {
                        logger.error("Failed to set filter", e);
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
