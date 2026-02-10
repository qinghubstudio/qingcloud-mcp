package com.qingcloud.mcp.autoclip.tools;

import com.qingcloud.mcp.autoclip.service.DraftService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 添加视频工具
 */
public class AddVideoToolFactory {

    private static final Logger logger = LoggerFactory.getLogger(AddVideoToolFactory.class);

    public static McpServerFeatures.SyncToolSpecification create(DraftService draftService) {
        Tool tool = new Tool(
                "autoclip_addVideo",
                "添加视频到草稿",
                null,
                new JsonSchema("object",
                        Map.of(
                                "videoUrl", Map.of("type", "string", "description", "视频URL或本地路径"),
                                "draftId", Map.of("type", "string", "description", "草稿ID"),
                                "start", Map.of("type", "number", "default", 0, "description", "源视频开始时间（秒）"),
                                "end", Map.of("type", "number", "description", "源视频结束时间（秒）"),
                                "targetStart", Map.of("type", "number", "default", 0, "description", "目标时间轴开始时间"),
                                "trackName", Map.of("type", "string", "default", "main", "description", "轨道名称"),
                                "speed", Map.of("type", "number", "default", 1.0, "description", "播放速度"),
                                "volume", Map.of("type", "number", "default", 1.0, "description", "音量")),
                        List.of("videoUrl"), null, null, null),
                null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> {
                    try {
                        Map<String, Object> args = request.arguments();
                        String videoUrl = getString(args, "videoUrl", null);

                        if (videoUrl == null || videoUrl.isEmpty()) {
                            return errorResult("Missing required parameter: videoUrl");
                        }

                        String draftId = getString(args, "draftId", null);
                        double start = getDouble(args, "start", 0);
                        double end = getDouble(args, "end", 0);
                        double targetStart = getDouble(args, "targetStart", 0);
                        int width = getInt(args, "width", 1080);
                        int height = getInt(args, "height", 1920);
                        String trackName = getString(args, "trackName", "main");
                        double speed = getDouble(args, "speed", 1.0);
                        double volume = getDouble(args, "volume", 1.0);

                        Map<String, Object> result = draftService.addVideo(
                                draftId, videoUrl, start, end, targetStart,
                                width, height, trackName, speed, volume);

                        String json = String.format(
                                "{\"success\":true,\"draftId\":\"%s\",\"segmentId\":\"%s\"}",
                                result.get("draftId"), result.get("segmentId"));

                        logger.info("Added video to draft: {}", result.get("draftId"));

                        return CallToolResult.builder()
                                .content(List.of(new TextContent(json)))
                                .isError(false)
                                .build();

                    } catch (Exception e) {
                        logger.error("Failed to add video", e);
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
