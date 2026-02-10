package com.qingcloud.mcp.xhs.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingcloud.mcp.xhs.actions.UserProfileAction;
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
 * 用户资料工具工厂
 * 创建用于获取用户资料的 MCP 工具
 */
public class UserProfileToolFactory {

        private static final Logger logger = LoggerFactory.getLogger(UserProfileToolFactory.class);
        private static final ObjectMapper objectMapper = new ObjectMapper();

        /**
         * 创建用户资料工具
         */
        public static McpServerFeatures.SyncToolSpecification create(PlaywrightBrowserManager browserManager) {
                // 定义参数 schema
                Map<String, Object> properties = new LinkedHashMap<>();

                Map<String, Object> userIdProperty = new LinkedHashMap<>();
                userIdProperty.put("type", "string");
                userIdProperty.put("description", "User ID to fetch profile for");
                properties.put("userId", userIdProperty);

                Map<String, Object> xsecTokenProperty = new LinkedHashMap<>();
                xsecTokenProperty.put("type", "string");
                xsecTokenProperty.put("description", "Access token from Feed list xsecToken field");
                properties.put("xsecToken", xsecTokenProperty);

                JsonSchema inputSchema = new JsonSchema(
                                "object",
                                properties,
                                List.of("userId", "xsecToken"),
                                null, null, null);

                Tool userProfileTool = new Tool(
                                "getUserProfile",
                                "Get user profile information from Xiaohongshu",
                                null,
                                inputSchema,
                                null, null, null);

                return McpServerFeatures.SyncToolSpecification.builder()
                                .tool(userProfileTool)
                                .callHandler((exchange, request) -> {
                                        Page page = null;
                                        try {
                                                logger.info("=== Get User Profile Tool Called ===");

                                                // 获取参数
                                                String userId = (String) request.arguments().get("userId");
                                                String xsecToken = (String) request.arguments().get("xsecToken");

                                                if (userId == null || userId.trim().isEmpty()) {
                                                        return CallToolResult.builder()
                                                                        .content(List.of(new TextContent(
                                                                                        "{\"code\":-1,\"success\":false,\"message\":\"userId is required\"}")))
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
                                                UserProfileAction userProfileAction = new UserProfileAction(page);

                                                // 获取用户资料
                                                Map<String, Object> result = userProfileAction.getUserProfile(userId,
                                                                xsecToken);

                                                page.close();

                                                // 构建响应
                                                Map<String, Object> response = new LinkedHashMap<>();
                                                response.put("code", 0);
                                                response.put("success", true);
                                                response.put("data", result);

                                                String responseJson = objectMapper.writeValueAsString(response);

                                                logger.info("✓ User profile completed for: {}", userId);

                                                return CallToolResult.builder()
                                                                .content(List.of(new TextContent(responseJson)))
                                                                .isError(false)
                                                                .build();

                                        } catch (Exception e) {
                                                logger.error("User profile tool failed", e);
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
