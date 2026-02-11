package com.qingcloud.mcp.xhs.tools;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.qingcloud.mcp.xhs.actions.SearchAction;

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

* 搜索工具工厂

* 创建用于搜索小红书笔记的 MCP 工具

*/

public class SearchToolFactory {

private static final Logger logger =
LoggerFactory.getLogger(SearchToolFactory.class);

private static final ObjectMapper objectMapper = new ObjectMapper();

/**

* 创建搜索工具

*/

public static McpServerFeatures.SyncToolSpecification
// create(PlaywrightBrowserManager browserManager) {

// 定义参数 schema

Map<String, Object> keywordProperty = Map.of(

// "type", "string",

// "description", "Search keyword");

Map<String, Object> pageProperty = Map.of(

// "type", "integer",

// "description", "Page number (default: 1)",

// "default", 1);

Map<String, Object> pageSizeProperty = Map.of(

// "type", "integer",

// "description", "Number of results per page (default: 20)",

// "default", 20);

Map<String, Object> properties = new LinkedHashMap<>();

// properties.put("keyword", keywordProperty);

// properties.put("page", pageProperty);

// properties.put("page_size", pageSizeProperty);

JsonSchema inputSchema = new JsonSchema(

// "object",

// properties,

List.of("keyword"),

null, null, null);

Tool searchTool = new Tool(

// "searchNotes",

// "Search Xiaohongshu notes by keyword using browser page scraping",

null,

// inputSchema,

null, null, null);

return McpServerFeatures.SyncToolSpecification.builder()

.tool(searchTool)

.callHandler((exchange, request) -> {

Page page = null;

try {

logger.info("=== Search Notes Tool Called ===");

// 获取参数

Map<String, Object> params = (Map<String, Object>) request.arguments();

String keyword = (String) params.get("keyword");

if (keyword == null || keyword.trim().isEmpty()) {

return CallToolResult.builder()

.content(List.of(new TextContent(

// "{\"code\":-1,\"success\":false,\"message\":\"Keyword is required\"}")))

.isError(true)

.build();

}

logger.info("Keyword: {}", keyword);

// 创建新页面

// page = browserManager.newPage();

SearchAction searchAction = new SearchAction(page);

// 执行搜索

List<Map<String, Object>> results = searchAction.search(keyword);

// page.close();

// 构建响应

Map<String, Object> response = new LinkedHashMap<>();

// response.put("code", 0);

// response.put("success", true);

// response.put("data", Map.of(

// "items", results,

// "total", results.size()));

String responseJson = objectMapper.writeValueAsString(response);

logger.info("✓ Search completed, found {} results", results.size());

return CallToolResult.builder()

.content(List.of(new TextContent(responseJson)))

.isError(false)

.build();

} catch (Exception e) {

logger.error("Search tool failed", e);

if (page != null) {

try {

// page.close();

} catch (Exception ignored) {

}

}

return CallToolResult.builder()

.content(List.of(new TextContent(

// "{\"code\":-1,\"success\":false,\"message\":\""

+ e.getMessage()

+ "\"}")))

.isError(true)

.build();

}

})

.build();

}

}
