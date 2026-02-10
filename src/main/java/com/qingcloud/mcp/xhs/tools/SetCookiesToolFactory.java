package com.qingcloud.mcp.xhs.tools;

import com.qingcloud.mcp.xhs.cookie.CookieManager;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cookie 设置工具工厂
 * 创建用于设置 cookies 的 MCP 工具
 */
public class SetCookiesToolFactory {

        private static final Logger logger = LoggerFactory.getLogger(SetCookiesToolFactory.class);

        /**
         * 创建 Cookie 设置工具
         */
        public static McpServerFeatures.SyncToolSpecification create(CookieManager cookieManager) {
                // 定义参数 schema
                Map<String, Object> cookieStringProperty = Map.of(
                                "type", "string",
                                "description", "Cookie string in format: 'name1=value1; name2=value2; ...'");

                Map<String, Object> properties = new LinkedHashMap<>();
                properties.put("cookieString", cookieStringProperty);

                JsonSchema inputSchema = new JsonSchema(
                                "object",
                                properties,
                                List.of("cookieString"),
                                null, null, null);

                Tool setCookiesTool = new Tool(
                                "setCookies",
                                "Set cookies for Xiaohongshu from a cookie string",
                                null,
                                inputSchema,
                                null, null, null);

                return McpServerFeatures.SyncToolSpecification.builder()
                                .tool(setCookiesTool)
                                .callHandler((exchange, request) -> {
                                        try {
                                                logger.info("=== Set Cookies Tool Called ===");

                                                // 获取 cookie 字符串
                                                Map<String, Object> params = (Map<String, Object>) request.arguments();
                                                String cookieString = (String) params.get("cookieString");

                                                if (cookieString == null || cookieString.trim().isEmpty()) {
                                                        return CallToolResult.builder()
                                                                        .content(List.of(new TextContent(
                                                                                        "{\"success\":false,\"message\":\"Cookie string is empty\"}")))
                                                                        .isError(true)
                                                                        .build();
                                                }

                                                logger.info("Cookie string length: {}", cookieString.length());

                                                // 这里暂时只记录,实际的 cookie 设置会在浏览器初始化时从文件加载
                                                // 用户需要手动将 cookie 字符串保存到 cookies.json 文件
                                                logger.warn("Note: Cookies should be saved to cookies.json file manually");
                                                logger.info("Cookie string: {}",
                                                                cookieString.substring(0,
                                                                                Math.min(100, cookieString.length()))
                                                                                + "...");

                                                return CallToolResult.builder()
                                                                .content(List.of(new TextContent(
                                                                                "{\"success\":true,\"message\":\"Cookie string received. Please save cookies to cookies.json file for persistence.\"}")))
                                                                .isError(false)
                                                                .build();

                                        } catch (Exception e) {
                                                logger.error("Set cookies failed", e);

                                                return CallToolResult.builder()
                                                                .content(List.of(new TextContent(
                                                                                "{\"success\":false,\"error\":\""
                                                                                                + e.getMessage()
                                                                                                + "\"}")))
                                                                .isError(true)
                                                                .build();
                                        }
                                })
                                .build();
        }

}
