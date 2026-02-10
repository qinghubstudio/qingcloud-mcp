package com.qingcloud.mcp.autoclip.tools;

import com.qingcloud.mcp.autoclip.service.DraftService;
import com.qingcloud.mcp.autoclip.util.SrtParser;
import com.qingcloud.mcp.autoclip.util.SrtParser.SubtitleEntry;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 添加字幕工具（支持SRT文件）
 */
public class AddSubtitleToolFactory {

    private static final Logger logger = LoggerFactory.getLogger(AddSubtitleToolFactory.class);

    public static McpServerFeatures.SyncToolSpecification create(DraftService draftService) {
        Tool tool = new Tool(
                "autoclip_addSubtitle",
                "从SRT文件添加字幕到草稿",
                null,
                new JsonSchema("object",
                        Map.of(
                                "srtPath", Map.of("type", "string", "description", "SRT文件路径或URL"),
                                "draftId", Map.of("type", "string", "description", "草稿ID"),
                                "timeOffset", Map.of("type", "number", "default", 0, "description", "时间偏移（秒）"),
                                "fontColor", Map.of("type", "string", "default", "#ffffff", "description", "字体颜色"),
                                "fontSize", Map.of("type", "number", "default", 8.0, "description", "字体大小"),
                                "transformY", Map.of("type", "number", "default", -0.8, "description", "Y位置")),
                        List.of("srtPath"), null, null, null),
                null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> {
                    try {
                        Map<String, Object> args = request.arguments();
                        String srtPath = getString(args, "srtPath", null);

                        if (srtPath == null || srtPath.isEmpty()) {
                            return errorResult("Missing required parameter: srtPath");
                        }

                        String draftId = getString(args, "draftId", null);
                        double timeOffset = getDouble(args, "timeOffset", 0);
                        String fontColor = getString(args, "fontColor", "#ffffff");
                        double fontSize = getDouble(args, "fontSize", 8.0);
                        double transformY = getDouble(args, "transformY", -0.8);
                        int width = getInt(args, "width", 1080);
                        int height = getInt(args, "height", 1920);
                        String trackName = getString(args, "trackName", "subtitle");

                        // 读取 SRT 内容
                        String srtContent = loadSrtContent(srtPath);

                        // 解析字幕
                        List<SubtitleEntry> entries = SrtParser.parse(srtContent);

                        if (entries.isEmpty()) {
                            return errorResult("No subtitles found in SRT file");
                        }

                        // 添加每条字幕
                        String resultDraftId = null;
                        int count = 0;
                        long offsetMicros = (long) (timeOffset * 1_000_000);

                        for (SubtitleEntry entry : entries) {
                            double startSec = (entry.startTime + offsetMicros) / 1_000_000.0;
                            double endSec = (entry.endTime + offsetMicros) / 1_000_000.0;

                            Map<String, Object> result = draftService.addText(
                                    draftId != null ? draftId : resultDraftId,
                                    entry.text, startSec, endSec,
                                    fontColor, fontSize, 0, transformY,
                                    width, height, trackName);

                            if (resultDraftId == null) {
                                resultDraftId = (String) result.get("draftId");
                                draftId = resultDraftId;
                            }
                            count++;
                        }

                        String json = String.format(
                                "{\"success\":true,\"draftId\":\"%s\",\"subtitleCount\":%d}",
                                resultDraftId, count);

                        logger.info("Added {} subtitles to draft: {}", count, resultDraftId);

                        return CallToolResult.builder()
                                .content(List.of(new TextContent(json)))
                                .isError(false)
                                .build();

                    } catch (Exception e) {
                        logger.error("Failed to add subtitle", e);
                        return errorResult(e.getMessage());
                    }
                })
                .build();
    }

    private static String loadSrtContent(String path) throws Exception {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(path))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } else {
            return Files.readString(Path.of(path));
        }
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
