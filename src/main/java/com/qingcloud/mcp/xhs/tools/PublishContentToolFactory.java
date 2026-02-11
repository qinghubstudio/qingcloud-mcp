package com.qingcloud.mcp.xhs.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import com.qingcloud.mcp.xhs.actions.PublishAction;
import com.qingcloud.mcp.xhs.browser.PlaywrightBrowserManager;
import com.qingcloud.mcp.xhs.model.PublishImageContent;
import com.qingcloud.mcp.xhs.util.ImageDownloader;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
* 发布图文内容工具工厂
*/
public class PublishContentToolFactory {

private static final Logger logger =
LoggerFactory.getLogger(PublishContentToolFactory.class);
private static final ObjectMapper objectMapper = new ObjectMapper();

/**
* 创建发布图文内容工具
*/
public static McpServerFeatures.SyncToolSpecification create(
PlaywrightBrowserManager browserManager,
ImageDownloader imageDownloader) {

// 定义参数 schema
Map<String, Object> titleProperty = Map.of(
// "type", "string",
// "description", "Content title (max 20 Chinese characters or 40 English
// words)");

Map<String, Object> contentProperty = Map.of(
// "type", "string",
// "description",
// "Main content text, excluding # tags. Use tags parameter for topic tags.");

Map<String, Object> imagesProperty = Map.of(
// "type", "array",
// "description",
// "Image path list (at least 1 image). Supports: 1. HTTP/HTTPS URLs
(auto-download); 2. Local absolute paths (recommended)",
// "items", Map.of("type", "string"));

Map<String, Object> tagsProperty = Map.of(
// "type", "array",
// "description", "Optional topic tags list, e.g. [美食, 旅行, 生活]",
// "items", Map.of("type", "string"));

Map<String, Object> properties = new LinkedHashMap<>();
// properties.put("title", titleProperty);
// properties.put("content", contentProperty);
// properties.put("images", imagesProperty);
// properties.put("tags", tagsProperty);

JsonSchema inputSchema = new JsonSchema(
// "object",
// properties,
List.of("title", "content", "images"),
null, null, null);

Tool publishTool = new Tool(
// "publish_content",
// "Publish image-text content to Xiaohongshu. Supports both HTTP/HTTPS image
URLs (auto-download) and local image paths.",
null,
// inputSchema,
null, null, null);

return McpServerFeatures.SyncToolSpecification.builder()
.tool(publishTool)
.callHandler((exchange, request) -> {
Page page = null;
try {
logger.info("=== Publish Content Tool Called ===");

// 解析参数
@SuppressWarnings("unchecked")
Map<String, Object> params = (Map<String, Object>) request.arguments();
String title = (String) params.get("title");
String content = (String) params.get("content");
@SuppressWarnings("unchecked")
List<String> images = (List<String>) params.get("images");
@SuppressWarnings("unchecked")
List<String> tags = (List<String>) params.getOrDefault("tags",
List.of());

logger.info("Title: {}", title);
logger.info("Images count: {}", images != null ? images.size() : 0);
logger.info("Tags count: {}", tags.size());

// 验证参数
if (title == null || title.isEmpty()) {
Map<String, Object> errorResponse = Map.of(
// "code", -1,
// "success", false,
// "message", "标题不能为空");
return CallToolResult.builder()
.content(List.of(new TextContent(
// objectMapper.writeValueAsString(
// errorResponse))))
.isError(true)
.build();
}

if (content == null || content.isEmpty()) {
Map<String, Object> errorResponse = Map.of(
// "code", -1,
// "success", false,
// "message", "内容不能为空");
return CallToolResult.builder()
.content(List.of(new TextContent(
// objectMapper.writeValueAsString(
// errorResponse))))
.isError(true)
.build();
}

if (images == null || images.isEmpty()) {
Map<String, Object> errorResponse = Map.of(
// "code", -1,
// "success", false,
// "message", "至少需要一张图片");
return CallToolResult.builder()
.content(List.of(new TextContent(
// objectMapper.writeValueAsString(
// errorResponse))))
.isError(true)
.build();
}

// 处理图片（下载URL图片或验证本地路径）
List<String> localImagePaths;
try {
// localImagePaths = imageDownloader.processImages(images);
logger.info("Processed {} images", localImagePaths.size());
} catch (Exception e) {
logger.error("Failed to process images", e);
Map<String, Object> errorResponse = Map.of(
// "code", -1,
// "success", false,
// "message", "图片处理失败: " + e.getMessage());
return CallToolResult.builder()
.content(List.of(new TextContent(
// objectMapper.writeValueAsString(
// errorResponse))))
.isError(true)
.build();
}

// 创建发布内容对象
PublishImageContent publishContent = new PublishImageContent(
// title, content, localImagePaths, tags);

// 创建新页面
// page = browserManager.newPage();
PublishAction publishAction = new PublishAction(page);

// 执行发布
// publishAction.publishImage(publishContent);

// 保存 cookies
// browserManager.saveCookies();

// page.close();

logger.info("✓ Content published successfully");

Map<String, Object> successResponse = Map.of(
// "code", 0,
// "success", true,
// "data", Map.of(
// "title", title,
// "images", localImagePaths.size(),
// "message", "内容发布成功"));

return CallToolResult.builder()
.content(List.of(new TextContent(objectMapper
.writeValueAsString(successResponse))))
.isError(false)
.build();

} catch (Exception e) {
logger.error("Publish content failed", e);
if (page != null) {
try {
// page.close();
} catch (Exception ignored) {
}
}

try {
Map<String, Object> errorResponse = Map.of(
// "code", -1,
// "success", false,
// "message", e.getMessage());
return CallToolResult.builder()
.content(List.of(new TextContent(
// objectMapper.writeValueAsString(
// errorResponse))))
.isError(true)
.build();
} catch (Exception jsonException) {
return CallToolResult.builder()
.content(List.of(new TextContent(
// "{\"code\":-1,\"success\":false,\"message\":\""
+ e.getMessage()
+ "\"}")))
.isError(true)
.build();
}
}
})
.build();
}
}
