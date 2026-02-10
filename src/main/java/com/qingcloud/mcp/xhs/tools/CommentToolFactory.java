package com.qingcloud.mcp.xhs.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingcloud.mcp.xhs.actions.CommentAction;
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
 * 评论工具工厂
 * 创建用于发表评论的 MCP 工具
 */
public class CommentToolFactory {

        private static final Logger logger = LoggerFactory.getLogger(CommentToolFactory.class);
        private static final ObjectMapper objectMapper = new ObjectMapper();

        /**
         * 创建评论工具
         */
        public static McpServerFeatures.SyncToolSpecification create(PlaywrightBrowserManager browserManager) {
                // 定义参数 schema
                Map<String, Object> properties = new LinkedHashMap<>();

                Map<String, Object> noteIdProperty = new LinkedHashMap<>();
                noteIdProperty.put("type", "string");
                noteIdProperty.put("description", "Note ID to comment on");
                properties.put("noteId", noteIdProperty);

                Map<String, Object> xsecTokenProperty = new LinkedHashMap<>();
                xsecTokenProperty.put("type", "string");
                xsecTokenProperty.put("description", "Access token from Feed list xsecToken field");
                properties.put("xsecToken", xsecTokenProperty);

                Map<String, Object> contentProperty = new LinkedHashMap<>();
                contentProperty.put("type", "string");
                contentProperty.put("description", "Comment content text");
                properties.put("content", contentProperty);

                JsonSchema inputSchema = new JsonSchema(
                                "object",
                                properties,
                                List.of("noteId", "xsecToken", "content"),
                                null, null, null);

                Tool commentTool = new Tool(
                                "postComment",
                                "Post a comment on a Xiaohongshu note",
                                null,
                                inputSchema,
                                null, null, null);

                return McpServerFeatures.SyncToolSpecification.builder()
                                .tool(commentTool)
                                .callHandler((exchange, request) -> {
                                        Page page = null;
                                        try {
                                                logger.info("=== Post Comment Tool Called ===");

                                                // 获取参数
                                                String noteId = (String) request.arguments().get("noteId");
                                                String xsecToken = (String) request.arguments().get("xsecToken");
                                                String content = (String) request.arguments().get("content");

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

                                                if (content == null || content.trim().isEmpty()) {
                                                        return CallToolResult.builder()
                                                                        .content(List.of(new TextContent(
                                                                                        "{\"code\":-1,\"success\":false,\"message\":\"content is required\"}")))
                                                                        .isError(true)
                                                                        .build();
                                                }

                                                // 创建新页面
                                                page = browserManager.newPage();
                                                CommentAction commentAction = new CommentAction(page);

                                                // 发表评论
                                                commentAction.postComment(noteId, xsecToken, content);

                                                page.close();

                                                // 构建响应
                                                Map<String, Object> response = new LinkedHashMap<>();
                                                response.put("code", 0);
                                                response.put("success", true);
                                                response.put("message", "Comment posted successfully");

                                                String responseJson = objectMapper.writeValueAsString(response);

                                                logger.info("✓ Comment posted successfully to note: {}", noteId);

                                                return CallToolResult.builder()
                                                                .content(List.of(new TextContent(responseJson)))
                                                                .isError(false)
                                                                .build();

                                        } catch (Exception e) {
                                                logger.error("Post comment tool failed", e);
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
