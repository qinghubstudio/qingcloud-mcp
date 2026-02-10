package com.qingcloud.mcp.autoclip.tools;

import com.qingcloud.mcp.autoclip.service.DraftService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 获取草稿信息工具
 */
public class GetDraftInfoToolFactory {

    private static final Logger logger = LoggerFactory.getLogger(GetDraftInfoToolFactory.class);

    public static McpServerFeatures.SyncToolSpecification create(DraftService draftService) {
        Tool tool = new Tool(
                "autoclip_getDraftInfo",
                "获取草稿详细信息",
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

                        Map<String, Object> info = draftService.getDraftInfo(draftId);

                        if (info == null) {
                            return errorResult("Draft not found: " + draftId);
                        }

                        StringBuilder json = new StringBuilder("{\"success\":true");
                        for (Map.Entry<String, Object> e : info.entrySet()) {
                            json.append(",\"").append(e.getKey()).append("\":");
                            Object v = e.getValue();
                            if (v instanceof String) {
                                json.append("\"").append(v).append("\"");
                            } else {
                                json.append(v);
                            }
                        }
                        json.append("}");

                        logger.info("Got draft info: {}", draftId);

                        return CallToolResult.builder()
                                .content(List.of(new TextContent(json.toString())))
                                .isError(false)
                                .build();

                    } catch (Exception e) {
                        logger.error("Failed to get draft info", e);
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
