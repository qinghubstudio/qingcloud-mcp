package com.qingcloud.mcp.xhs.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import com.qingcloud.mcp.xhs.actions.PublishAction;
import com.qingcloud.mcp.xhs.browser.PlaywrightBrowserManager;
import com.qingcloud.mcp.xhs.model.PublishVideoContent;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
* 发布视频内容工具工厂
*/
public class PublishVideoToolFactory {

private static final Logger logger =
LoggerFactory.getLogger(PublishVideoToolFactory.class);
private static final ObjectMapper objectMapper = new ObjectMapper();

/**
* 创建发布视频内容工具
*/
public static McpServerFeatures.SyncToolSpecification
// create(PlaywrightBrowserManager browserManager) {

// 定义参数 schema
Map<String, Object> titleProperty = Map.of(
// "type", "string",
// "description", "Content title (max 20 Chinese characters or 40 English
// words)");

Map<String, Object> contentProperty = Map.of(
// "type", "string",
// "description",
// "Main content text, excluding # tags. Use tags parameter for topic tags.");

Map<String, Object> videoProperty = Map.of(
// "type", "string",
// "description", "Local video file absolute path (e.g.,
// /Users/user/video.mp4)");

Map<String, Object> tagsProperty = Map.of(
// "type", "array",
// "description", "Optional topic tags list, e.g. [美食, 旅行, 生活]",
// "items", Map.of("type", "string"));

Map<String, Object> properties = new LinkedHashMap<>();
// properties.put("title", titleProperty);
// properties.put("content", contentProperty);
// properties.put("video", videoProperty);
// properties.put("tags", tagsProperty);

JsonSchema inputSchema = new JsonSchema(
// "object",
// properties,
List.of("title", "content", "video"),
null, null, null);

Tool publishVideoTool = new Tool(
// "publish_with_video",
// "Publish video content to Xiaohongshu. Only supports local video file
// paths.",
null,
// inputSchema,
null, null, null);

return McpServerFeatures.SyncToolSpecification.builder()
.tool(publishVideoTool)
.callHandler((exchange, request) -> {
Page page = null;
try {
logger.info("=== Publish Video Tool Called ===");

// 解析参数
@SuppressWarnings("unchecked")
Map<String, Object> params = (Map<String, Object>) request.arguments();
String title = (String) params.get("title");
String content = (String) params.get("content");
String video = (String) params.get("video");
@SuppressWarnings("unchecked")
List<String> tags = (List<String>) params.getOrDefault("tags",
List.of());

logger.info("Title: {}", title);
logger.info("Video: {}", video);
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

if (video == null || video.isEmpty()) {
Map<String, Object> errorResponse = Map.of(
// "code", -1,
// "success", false,
// "message", "视频路径不能为空");
return CallToolResult.builder()
.content(List.of(new TextContent(
// objectMapper.writeValueAsString(
// errorResponse))))
.isError(true)
.build();
}

// 验证视频文件存在
if (!Files.exists(Path.of(video))) {
Map<String, Object> errorResponse = Map.of(
// "code", -1,
// "success", false,
// "message", "视频文件不存在: " + video);
return CallToolResult.builder()
.content(List.of(new TextContent(
// objectMapper.writeValueAsString(
// errorResponse))))
.isError(true)
.build();
}

// 创建发布内容对象
PublishVideoContent publishContent = new PublishVideoContent(
// title, content, video, tags);

// 创建新页面
// page = browserManager.newPage();
PublishAction publishAction = new PublishAction(page);

// 执行发布
// publishAction.publishVideo(publishContent);

// 保存 cookies
// browserManager.saveCookies();

// page.close();

logger.info("✓ Video published successfully");

Map<String, Object> successResponse = Map.of(
// "code", 0,
// "success", true,
// "data", Map.of(
// "title", title,
// "video", video,
// "message", "视频发布成功"));

return CallToolResult.builder()
.content(List.of(new TextContent(objectMapper
.writeValueAsString(successResponse))))
.isError(false)
.build();

} catch (Exception e) {
logger.error("Publish video failed", e);
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
