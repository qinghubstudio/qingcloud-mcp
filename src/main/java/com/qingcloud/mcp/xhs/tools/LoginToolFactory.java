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
* 登录工具工厂
* 创建用于获取二维码并等待用户扫码登录的 MCP 工具
*/
public class LoginToolFactory {

private static final Logger logger =
LoggerFactory.getLogger(LoginToolFactory.class);

/**
* 创建登录工具
*/
public static McpServerFeatures.SyncToolSpecification
// create(PlaywrightBrowserManager browserManager) {
Tool loginTool = new Tool(
// "login",
// "Get QR code for Xiaohongshu login and wait for user to scan. Returns QR code
// image URL or login status.",
null,
new JsonSchema("object", Map.of(), List.of(), null, null, null),
null, null, null);

return McpServerFeatures.SyncToolSpecification.builder()
.tool(loginTool)
.callHandler((exchange, request) -> {
Page page = null;
try {
logger.info("=== Login Tool Called ===");

// 创建新页面
// page = browserManager.newPage();
LoginAction loginAction = new LoginAction(page);

// 获取二维码
String qrcodeUrl = loginAction.fetchQrcodeImage();

if (qrcodeUrl == null) {
// 已登录
logger.info("User is already logged in");
// page.close();

// 保存 cookies
// browserManager.saveCookies();

return CallToolResult.builder()
.content(List.of(new TextContent(
// "{\"status\":\"already_logged_in\",\"message\":\"User is already logged
// in\"}")))
.isError(false)
.build();
}

logger.info("QR code URL: {}", qrcodeUrl);
logger.info("Waiting for user to scan QR code (120s timeout)...");

// 等待登录(120秒超时)
boolean success = loginAction.waitForLogin(120);

if (success) {
logger.info("✓ Login successful");

// 保存 cookies
// browserManager.saveCookies();

// page.close();

return CallToolResult.builder()
.content(List.of(new TextContent(
// "{\"status\":\"success\",\"qrcodeUrl\":\""
+ qrcodeUrl
+ "\",\"message\":\"Login successful, cookies saved\"}")))
.isError(false)
.build();
} else {
logger.warn("✗ Login timeout");
// page.close();

return CallToolResult.builder()
.content(List.of(new TextContent(
// "{\"status\":\"timeout\",\"qrcodeUrl\":\""
+ qrcodeUrl
+ "\",\"message\":\"Login timeout after 120 seconds. Please try again.\"}")))
.isError(true)
.build();
}

} catch (Exception e) {
logger.error("Login tool failed", e);
if (page != null) {
try {
// page.close();
} catch (Exception ignored) {
}
}

return CallToolResult.builder()
.content(List.of(new TextContent(
// "{\"status\":\"error\",\"message\":\""
+ e.getMessage()
+ "\"}")))
.isError(true)
.build();
}
})
.build();
}
}
