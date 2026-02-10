package com.qingcloud.mcp.xhs.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingcloud.mcp.xhs.actions.PostDetailAction;
import com.qingcloud.mcp.xhs.browser.PlaywrightBrowserManager;
import com.microsoft.playwright.Page;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 帖子详情工具工厂
 * 创建用于获取帖子详情的 MCP 工具
 */
public class PostDetailToolFactory {

        private static final Logger logger = LoggerFactory.getLogger(PostDetailToolFactory.class);
        private static final ObjectMapper objectMapper = new ObjectMapper();

        /**
         * 创建帖子详情工具
         */
        public static McpServerFeatures.SyncToolSpecification create(PlaywrightBrowserManager browserManager) {
                // 定义参数 schema
                Map<String, Object> properties = new LinkedHashMap<>();

                Map<String, Object> noteIdProperty = new LinkedHashMap<>();
                noteIdProperty.put("type", "string");
                noteIdProperty.put("description", "Note ID to get details for");
                properties.put("noteId", noteIdProperty);

                Map<String, Object> xsecTokenProperty = new LinkedHashMap<>();
                xsecTokenProperty.put("type", "string");
                xsecTokenProperty.put("description", "Access token from Feed list xsecToken field");
                properties.put("xsecToken", xsecTokenProperty);

                JsonSchema inputSchema = new JsonSchema(
                                "object",
                                properties,
                                List.of("noteId", "xsecToken"),
                                null, null, null);

                Tool postDetailTool = new Tool(
                                "getPostDetail",
                                "Get detailed information about a specific Xiaohongshu post/note",
                                null,
                                inputSchema,
                                null, null, null);

                return McpServerFeatures.SyncToolSpecification.builder()
                                .tool(postDetailTool)
                                .callHandler((exchange, request) -> {
                                        Page page = null;
                                        try {
                                                logger.info("=== Get Post Detail Tool Called ===");

                                                // 获取参数
                                                String noteId = (String) request.arguments().get("noteId");
                                                String xsecToken = (String) request.arguments().get("xsecToken");

                                                if (noteId == null || noteId.trim().isEmpty()) {
                                                        return CallToolResult.builder()
                                                                        .content(List.of(new TextContent(
                                                                                        "{\"code\":-1,\"success\":false,\"message\":\"noteId is required\"}")))
                                                                        .isError(true)
                                                                        .build();
                                                }

                                                if (xsecToken == null || xsecToken.trim().isEmpty()) {
                                                        return CallToolResult.builder()
                                                                        .content(List.of(new TextContent(
                                                                                        "{\"code\":-1,\"success\":false,\"message\":\"xsecToken is required\"}")))
                                                                        .isError(true)
                                                                        .build();
                                                }

                                                // 创建新页面
                                                page = browserManager.newPage();
                                                PostDetailAction postDetailAction = new PostDetailAction(page);

                                                // 获取帖子详情
                                                Map<String, Object> result = postDetailAction.getPostDetail(noteId,
                                                                xsecToken);

                                                page.close();

                                                // 构建响应
                                                Map<String, Object> response = new LinkedHashMap<>();
                                                response.put("code", 0);
                                                response.put("success", true);
                                                response.put("data", result);

                                                String responseJson = objectMapper.writeValueAsString(response);

                                                logger.info("✓ Post detail completed for: {}", noteId);

                                                return CallToolResult.builder()
                                                                .content(List.of(new TextContent(responseJson)))
                                                                .isError(false)
                                                                .build();

                                        } catch (Exception e) {
                                                logger.error("Post detail tool failed", e);
                                                if (page != null) {
                                                        try {
                                                                page.close();
                                                        } catch (Exception ignored) {
                                                        }
                                                }

                                                return CallToolResult.builder()
                                                                .content(List.of(new TextContent(
                                                                                "{\"code\":-1,\"success\":false,\"message\":\""
                                                                                                + e.getMessage()
                                                                                                + "\"}")))
                                                                .isError(true)
                                                                .build();
                                        }
                                })
                                .build();
        }
}
