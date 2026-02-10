package com.qingcloud.mcp.xhs.tools;

import com.qingcloud.mcp.xhs.actions.LoginAction;
import com.qingcloud.mcp.xhs.browser.PlaywrightBrowserManager;
import com.microsoft.playwright.Page;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 登录状态检查工具工厂
 * 创建用于检查用户登录状态的 MCP 工具
 */
public class CheckLoginStatusToolFactory {

        private static final Logger logger = LoggerFactory.getLogger(CheckLoginStatusToolFactory.class);

        /**
         * 创建登录状态检查工具
         */
        public static McpServerFeatures.SyncToolSpecification create(PlaywrightBrowserManager browserManager) {
                Tool checkLoginTool = new Tool(
                                "checkLoginStatus",
                                "Check if user is logged in to Xiaohongshu",
                                null,
                                new JsonSchema("object", Map.of(), List.of(), null, null, null),
                                null, null, null);

                return McpServerFeatures.SyncToolSpecification.builder()
                                .tool(checkLoginTool)
                                .callHandler((exchange, request) -> {
                                        Page page = null;
                                        try {
                                                logger.info("=== Check Login Status Tool Called ===");

                                                // 创建新页面
                                                page = browserManager.newPage();
                                                LoginAction loginAction = new LoginAction(page);

                                                // 检查登录状态
                                                boolean isLoggedIn = loginAction.checkLoginStatus();

                                                page.close();

                                                String response = String.format(
                                                                "{\"isLoggedIn\":%b,\"message\":\"%s\"}",
                                                                isLoggedIn,
                                                                isLoggedIn ? "User is logged in"
                                                                                : "User is not logged in");

                                                logger.info("Login status: {}",
                                                                isLoggedIn ? "LOGGED_IN" : "NOT_LOGGED_IN");

                                                return CallToolResult.builder()
                                                                .content(List.of(new TextContent(response)))
                                                                .isError(false)
                                                                .build();

                                        } catch (Exception e) {
                                                logger.error("Check login status failed", e);
                                                if (page != null) {
                                                        try {
                                                                page.close();
                                                        } catch (Exception ignored) {
                                                        }
                                                }

                                                return CallToolResult.builder()
                                                                .content(List.of(new TextContent(
                                                                                "{\"isLoggedIn\":false,\"error\":\""
                                                                                                + e.getMessage()
                                                                                                + "\"}")))
                                                                .isError(true)
                                                                .build();
                                        }
                                })
                                .build();
        }
}
