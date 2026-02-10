package com.qingcloud.mcp.autoclip.tools;

import com.qingcloud.mcp.autoclip.service.DraftService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 删除草稿工具
 */
public class DeleteDraftToolFactory {

    private static final Logger logger = LoggerFactory.getLogger(DeleteDraftToolFactory.class);

    public static McpServerFeatures.SyncToolSpecification create(DraftService draftService) {
        Tool tool = new Tool(
                "autoclip_deleteDraft",
                "删除草稿（从内存缓存中移除）",
                null,
                new JsonSchema("object",
                        Map.of(
                                "draftId", Map.of("type", "string", "description", "草稿ID")),
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

                        boolean deleted = draftService.deleteDraft(draftId);

                        String json = String.format(
                                "{\"success\":%s,\"draftId\":\"%s\",\"message\":\"%s\"}",
                                deleted, draftId,
                                deleted ? "Draft deleted" : "Draft not found");

                        logger.info("Delete draft {}: {}", draftId, deleted);

                        return CallToolResult.builder()
                                .content(List.of(new TextContent(json)))
                                .isError(!deleted)
                                .build();

                    } catch (Exception e) {
                        logger.error("Failed to delete draft", e);
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
