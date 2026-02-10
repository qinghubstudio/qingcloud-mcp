package com.qingcloud.mcp.autoclip.tools;

import com.qingcloud.mcp.autoclip.service.DraftExportService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 保存草稿工具
 */
public class SaveDraftToolFactory {

    private static final Logger logger = LoggerFactory.getLogger(SaveDraftToolFactory.class);

    public static McpServerFeatures.SyncToolSpecification create(DraftExportService exportService) {
        Tool tool = new Tool(
                "autoclip_saveDraft",
                "保存草稿为剪映/CapCut格式",
                null,
                new JsonSchema("object",
                        Map.of(
                                "draftId", Map.of("type", "string", "description", "草稿ID"),
                                "outputFolder", Map.of("type", "string", "description", "输出文件夹路径（可选）")),
                        List.of("draftId"), null, null, null),
                null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> {
                    try {
                        Map<String, Object> args = request.arguments();
                        String draftId = getString(args, "draftId", null);

                        if (draftId == null || draftId.isEmpty()) {
                            return errorResult("Missing required parameter: draftId");
                        }

                        String outputFolder = getString(args, "outputFolder", null);

                        Map<String, Object> result = exportService.saveDraft(draftId, outputFolder);

                        String json = String.format(
                                "{\"success\":true,\"draftId\":\"%s\",\"draftPath\":\"%s\"}",
                                result.get("draftId"),
                                result.get("draftPath").toString().replace("\\", "\\\\"));

                        logger.info("Saved draft: {} to {}", draftId, result.get("draftPath"));

                        return CallToolResult.builder()
                                .content(List.of(new TextContent(json)))
                                .isError(false)
                                .build();

                    } catch (Exception e) {
                        logger.error("Failed to save draft", e);
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

    private static CallToolResult errorResult(String error) {
        return CallToolResult.builder()
                .content(List.of(new TextContent("{\"success\":false,\"error\":\"" + error + "\"}")))
                .isError(true)
                .build();
    }
}
