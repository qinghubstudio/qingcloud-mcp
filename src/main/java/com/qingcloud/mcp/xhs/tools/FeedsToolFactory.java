package com.qingcloud.mcp.xhs.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingcloud.mcp.xhs.actions.FeedsAction;
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
 * Feeds 工具工厂
 * 创建用于获取首页推荐的 MCP 工具
 */
public class FeedsToolFactory {

        private static final Logger logger = LoggerFactory.getLogger(FeedsToolFactory.class);
        private static final ObjectMapper objectMapper = new ObjectMapper();

        /**
         * 创建 Feeds 工具
         */
        public static McpServerFeatures.SyncToolSpecification create(PlaywrightBrowserManager browserManager) {
                // 定义参数 schema (feeds 不需要参数)
                Map<String, Object> properties = new LinkedHashMap<>();

                JsonSchema inputSchema = new JsonSchema(
                                "object",
                                properties,
                                List.of(),
                                null, null, null);

                Tool feedsTool = new Tool(
                                "getFeeds",
                                "Get homepage recommended feeds from Xiaohongshu",
                                null,
                                inputSchema,
                                null, null, null);

                return McpServerFeatures.SyncToolSpecification.builder()
                                .tool(feedsTool)
                                .callHandler((exchange, request) -> {
                                        Page page = null;
                                        try {
                                                logger.info("=== Get Feeds Tool Called ===");

                                                // 创建新页面
                                                page = browserManager.newPage();
                                                FeedsAction feedsAction = new FeedsAction(page);

                                                // 获取 feeds
                                                List<Map<String, Object>> results = feedsAction.getFeeds();

                                                page.close();

                                                // 构建响应
                                                Map<String, Object> response = new LinkedHashMap<>();
                                                response.put("code", 0);
                                                response.put("success", true);
                                                response.put("data", Map.of(
                                                                "items", results,
                                                                "total", results.size()));

                                                String responseJson = objectMapper.writeValueAsString(response);

                                                logger.info("✓ Feeds completed, found {} items", results.size());

                                                return CallToolResult.builder()
                                                                .content(List.of(new TextContent(responseJson)))
                                                                .isError(false)
                                                                .build();

                                        } catch (Exception e) {
                                                logger.error("Feeds tool failed", e);
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
