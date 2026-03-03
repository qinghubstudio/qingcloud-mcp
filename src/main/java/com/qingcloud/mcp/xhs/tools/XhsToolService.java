package com.qingcloud.mcp.xhs.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import com.qingcloud.mcp.xhs.actions.*;
import com.qingcloud.mcp.xhs.browser.PlaywrightBrowserManager;

import com.qingcloud.mcp.xhs.model.PublishImageContent;
import com.qingcloud.mcp.xhs.model.PublishVideoContent;
import com.qingcloud.mcp.xhs.util.ImageDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * XHS（小红书）MCP Tools 服务 - Spring AI Tool 注解方式
 * 使用 @Tool 注解实现所有小红书功能
 */
@Service
public class XhsToolService {

    private static final Logger log = LoggerFactory.getLogger(XhsToolService.class);

    @Autowired
    private PlaywrightBrowserManager browserManager;

    @Autowired
    private ImageDownloader imageDownloader;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 二维码登录工具
     */
    @Tool(name = "login", description = "Get QR code for Xiaohongshu login. Set forceRefresh=true to clear existing session first. Returns QR code image URL.")
    public String loginWithQrCode(
            @ToolParam(description = "Whether to force logout before getting QR code", required = false) Boolean forceRefresh) {
        Page page = null;
        try {
            log.info("=== Login Tool Called (forceRefresh: {}) ===", forceRefresh);

            page = browserManager.newPage();
            LoginAction loginAction = new LoginAction(page);

            if (Boolean.TRUE.equals(forceRefresh)) {
                log.info("Force refresh requested. Clearing all session data...");
                loginAction.logout();
                // Thoroughly clear browser context
                browserManager.clearContext();
                // Re-get the page as the context might have been recreated
                page = browserManager.newPage();
                loginAction = new LoginAction(page);

                // Delete physical cookie file
                try {
                    java.nio.file.Files.deleteIfExists(java.nio.file.Path.of("cookies.json"));
                    log.info("Physical cookies.json deleted.");
                } catch (Exception e) {
                    log.warn("Failed to delete cookies.json: {}", e.getMessage());
                }
            }

            String qrcodeUrl = loginAction.fetchQrcodeImage();

            if (qrcodeUrl == null) {
                log.info("User is already logged in");
                page.close();
                browserManager.saveCookies();
                return "{\"status\":\"already_logged_in\",\"message\":\"User is already logged in\"}";
            }

            log.info("QR code URL: {}", qrcodeUrl);
            log.info("Waiting for user to scan QR code (120s timeout)...");

            boolean success = loginAction.waitForLogin(120);

            if (success) {
                log.info("✓ Login successful");
                browserManager.saveCookies();
                page.close();
                return "{\"status\":\"success\",\"qrcodeUrl\":\"" + qrcodeUrl
                        + "\",\"message\":\"Login successful, cookies saved\"}";
            } else {
                log.warn("✗ Login timeout");
                page.close();
                return "{\"status\":\"timeout\",\"qrcodeUrl\":\"" + qrcodeUrl
                        + "\",\"message\":\"Login timeout after 120 seconds. Please try again.\"}";
            }

        } catch (Exception e) {
            log.error("Login tool failed", e);
            if (page != null) {
                try {
                    page.close();
                } catch (Exception ignored) {
                }
            }
            return "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 检查登录状态
     */
    @Tool(name = "checkLoginStatus", description = "Check if the user is currently logged in to Xiaohongshu")
    public String checkLoginStatus() {
        Page page = null;
        try {
            log.info("=== Check Login Status Tool Called ===");

            page = browserManager.newPage();
            LoginAction loginAction = new LoginAction(page);
            boolean isLoggedIn = loginAction.checkLoginStatus();

            page.close();

            if (!isLoggedIn) {
                log.info("✗ User is not logged in");
                return "{\"isLoggedIn\":false,\"message\":\"User is not logged in\"}";
            } else {
                log.info("✓ User is logged in");
                return "{\"isLoggedIn\":true,\"message\":\"User is logged in\"}";
            }

        } catch (Exception e) {
            log.error("Check login status failed", e);
            if (page != null) {
                try {
                    page.close();
                } catch (Exception ignored) {
                }
            }
            return "{\"isLoggedIn\":false,\"message\":\"Error checking login status: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 退出登录工具
     */
    @Tool(name = "logout", description = "Logout from Xiaohongshu and clear local session")
    public String logout() {
        Page page = null;
        try {
            log.info("=== Logout Tool Called ===");

            page = browserManager.newPage();
            LoginAction loginAction = new LoginAction(page);
            loginAction.logout();

            // Clear local cookie file
            try {
                java.nio.file.Files.deleteIfExists(java.nio.file.Path.of("cookies.json"));
            } catch (Exception e) {
                log.warn("Failed to delete cookies.json: {}", e.getMessage());
            }

            page.close();
            log.info("✓ Logout successful and cookies.json cleared");
            return "{\"success\":true,\"message\":\"Logged out successfully and session cleared\"}";

        } catch (Exception e) {
            log.error("Logout tool failed", e);
            if (page != null) {
                try {
                    page.close();
                } catch (Exception ignored) {
                }
            }
            return "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 搜索笔记
     */
    @Tool(name = "searchNotes", description = "Search Xiaohongshu notes by keyword using browser page scraping")
    public String searchNotes(@ToolParam(description = "Search keyword") String keyword) {
        Page page = null;
        try {
            log.info("=== Search Notes Tool Called ===");
            log.info("Received keyword parameter: '{}'", keyword);

            if (keyword == null || keyword.trim().isEmpty()) {
                log.warn("Keyword is null or empty!");
                return "{\"code\":-1,\"success\":false,\"message\":\"Keyword is required\"}";
            }

            log.info("Keyword is valid: {}", keyword);

            page = browserManager.newPage();
            SearchAction searchAction = new SearchAction(page);

            List<Map<String, Object>> results = searchAction.search(keyword);

            page.close();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", 0);
            response.put("success", true);
            response.put("data", Map.of(
                    "items", results,
                    "total", results.size()));

            String responseJson = objectMapper.writeValueAsString(response);

            log.info("✓ Search completed, found {} results", results.size());

            return responseJson;

        } catch (Exception e) {
            log.error("Search tool failed", e);
            if (page != null) {
                try {
                    page.close();
                } catch (Exception ignored) {
                }
            }
            return "{\"code\":-1,\"success\":false,\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 获取信息流
     */
    @Tool(name = "getFeeds", description = "Get personalized feed of notes from Xiaohongshu homepage")
    public String getFeeds() {
        Page page = null;
        try {
            log.info("=== Get Feeds Tool Called ===");

            page = browserManager.newPage();
            FeedsAction feedsAction = new FeedsAction(page);

            List<Map<String, Object>> feeds = feedsAction.getFeeds();

            page.close();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", 0);
            response.put("success", true);
            response.put("data", Map.of(
                    "items", feeds,
                    "total", feeds.size()));

            String responseJson = objectMapper.writeValueAsString(response);

            log.info("✓ Get feeds completed, found {} items", feeds.size());

            return responseJson;

        } catch (Exception e) {
            log.error("Get feeds failed", e);
            if (page != null) {
                try {
                    page.close();
                } catch (Exception ignored) {
                }
            }
            return "{\"code\":-1,\"success\":false,\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 获取帖子详情
     */
    @Tool(name = "getPostDetail", description = "Get detailed information about a specific Xiaohongshu post/note")
    public String getPostDetail(String noteId, String xsecToken) {
        Page page = null;
        try {
            log.info("=== Get Post Detail Tool Called ===");

            if (noteId == null || noteId.trim().isEmpty()) {
                return "{\"code\":-1,\"success\":false,\"message\":\"noteId is required\"}";
            }
            if (xsecToken == null || xsecToken.trim().isEmpty()) {
                return "{\"code\":-1,\"success\":false,\"message\":\"xsecToken is required\"}";
            }

            page = browserManager.newPage();
            PostDetailAction postDetailAction = new PostDetailAction(page);

            Map<String, Object> result = postDetailAction.getPostDetail(noteId, xsecToken);

            page.close();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", 0);
            response.put("success", true);
            response.put("data", result);

            String responseJson = objectMapper.writeValueAsString(response);
            log.info("✓ Post detail completed for: {}", noteId);
            return responseJson;

        } catch (Exception e) {
            log.error("Post detail tool failed", e);
            if (page != null) {
                try {
                    page.close();
                } catch (Exception ignored) {
                }
            }
            return "{\"code\":-1,\"success\":false,\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 发表评论
     */
    @Tool(name = "postComment", description = "Post a comment on a Xiaohongshu note")
    public String postComment(String noteId, String xsecToken, String content) {
        Page page = null;
        try {
            log.info("=== Post Comment Tool Called ===");

            if (noteId == null || noteId.trim().isEmpty()) {
                return "{\"code\":-1,\"success\":false,\"message\":\"noteId is required\"}";
            }
            if (xsecToken == null || xsecToken.trim().isEmpty()) {
                return "{\"code\":-1,\"success\":false,\"message\":\"xsecToken is required\"}";
            }
            if (content == null || content.trim().isEmpty()) {
                return "{\"code\":-1,\"success\":false,\"message\":\"content is required\"}";
            }

            page = browserManager.newPage();
            CommentAction commentAction = new CommentAction(page);

            commentAction.postComment(noteId, xsecToken, content);

            page.close();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", 0);
            response.put("success", true);
            response.put("message", "Comment posted successfully");

            String responseJson = objectMapper.writeValueAsString(response);
            log.info("✓ Comment posted successfully to note: {}", noteId);
            return responseJson;

        } catch (Exception e) {
            log.error("Post comment tool failed", e);
            if (page != null) {
                try {
                    page.close();
                } catch (Exception ignored) {
                }
            }
            return "{\"code\":-1,\"success\":false,\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 获取用户资料
     */
    @Tool(name = "getUserProfile", description = "Get user profile information from Xiaohongshu")
    public String getUserProfile(String userId, String xsecToken) {
        Page page = null;
        try {
            log.info("=== Get User Profile Tool Called ===");

            if (userId == null || userId.trim().isEmpty()) {
                return "{\"code\":-1,\"success\":false,\"message\":\"userId is required\"}";
            }
            if (xsecToken == null || xsecToken.trim().isEmpty()) {
                return "{\"code\":-1,\"success\":false,\"message\":\"xsecToken is required\"}";
            }

            page = browserManager.newPage();
            UserProfileAction userProfileAction = new UserProfileAction(page);

            Map<String, Object> result = userProfileAction.getUserProfile(userId, xsecToken);

            page.close();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", 0);
            response.put("success", true);
            response.put("data", result);

            String responseJson = objectMapper.writeValueAsString(response);
            log.info("✓ User profile completed for: {}", userId);
            return responseJson;

        } catch (Exception e) {
            log.error("User profile tool failed", e);
            if (page != null) {
                try {
                    page.close();
                } catch (Exception ignored) {
                }
            }
            return "{\"code\":-1,\"success\":false,\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 发布图文内容
     */
    @Tool(name = "publish_content", description = "Publish image-text content to Xiaohongshu. Supports both HTTP/HTTPS image URLs (auto-download) and local image paths.")
    public String publishContent(String title, String content, List<String> images, List<String> tags) {
        Page page = null;
        try {
            log.info("=== Publish Content Tool Called ===");
            log.info("Title: {}", title);
            log.info("Images count: {}", images != null ? images.size() : 0);

            if (title == null || title.isEmpty()) {
                return "{\"code\":-1,\"success\":false,\"message\":\"标题不能为空\"}";
            }
            if (content == null || content.isEmpty()) {
                return "{\"code\":-1,\"success\":false,\"message\":\"内容不能为空\"}";
            }
            if (images == null || images.isEmpty()) {
                return "{\"code\":-1,\"success\":false,\"message\":\"至少需要一张图片\"}";
            }

            // 处理图片
            List<String> localImagePaths;
            try {
                localImagePaths = imageDownloader.processImages(images);
                log.info("Processed {} images", localImagePaths.size());
            } catch (Exception e) {
                log.error("Failed to process images", e);
                return "{\"code\":-1,\"success\":false,\"message\":\"图片处理失败: " + e.getMessage() + "\"}";
            }

            PublishImageContent publishContent = new PublishImageContent(
                    title, content, localImagePaths, tags != null ? tags : List.of());

            page = browserManager.newPage();
            PublishAction publishAction = new PublishAction(page);

            publishAction.publishImage(publishContent);
            browserManager.saveCookies();
            page.close();

            log.info("✓ Content published successfully");

            Map<String, Object> successResponse = Map.of(
                    "code", 0,
                    "success", true,
                    "data", Map.of(
                            "title", title,
                            "images", localImagePaths.size(),
                            "message", "内容发布成功"));
            return objectMapper.writeValueAsString(successResponse);

        } catch (Exception e) {
            log.error("Publish content failed", e);
            if (page != null) {
                try {
                    page.close();
                } catch (Exception ignored) {
                }
            }
            return "{\"code\":-1,\"success\":false,\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 发布视频内容
     */
    @Tool(name = "publish_with_video", description = "Publish video content to Xiaohongshu. Only supports local video file paths.")
    public String publishVideo(String title, String content, String video, List<String> tags) {
        Page page = null;
        try {
            log.info("=== Publish Video Tool Called ===");
            log.info("Title: {}", title);
            log.info("Video: {}", video);

            if (title == null || title.isEmpty()) {
                return "{\"code\":-1,\"success\":false,\"message\":\"标题不能为空\"}";
            }
            if (content == null || content.isEmpty()) {
                return "{\"code\":-1,\"success\":false,\"message\":\"内容不能为空\"}";
            }
            if (video == null || video.isEmpty()) {
                return "{\"code\":-1,\"success\":false,\"message\":\"视频路径不能为空\"}";
            }
            if (!Files.exists(Path.of(video))) {
                return "{\"code\":-1,\"success\":false,\"message\":\"视频文件不存在: " + video + "\"}";
            }

            PublishVideoContent publishContent = new PublishVideoContent(
                    title, content, video, tags != null ? tags : List.of());

            page = browserManager.newPage();
            PublishAction publishAction = new PublishAction(page);

            publishAction.publishVideo(publishContent);
            browserManager.saveCookies();
            page.close();

            log.info("✓ Video published successfully");

            Map<String, Object> successResponse = Map.of(
                    "code", 0,
                    "success", true,
                    "data", Map.of(
                            "title", title,
                            "video", video,
                            "message", "视频发布成功"));
            return objectMapper.writeValueAsString(successResponse);

        } catch (Exception e) {
            log.error("Publish video failed", e);
            if (page != null) {
                try {
                    page.close();
                } catch (Exception ignored) {
                }
            }
            return "{\"code\":-1,\"success\":false,\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 设置 Cookies
     */
    @Tool(name = "setCookies", description = "Set cookies for Xiaohongshu from a cookie string")
    public String setCookies(String cookieString) {
        try {
            log.info("=== Set Cookies Tool Called ===");

            if (cookieString == null || cookieString.trim().isEmpty()) {
                return "{\"success\":false,\"message\":\"Cookie string is empty\"}";
            }

            log.info("Cookie string length: {}", cookieString.length());
            log.warn(
                    "Note: Cookies should be saved to cookies.json file manually. This tool currently only logs them.");

            // Log truncated cookie string
            log.info("Cookie string: {}...", cookieString.substring(0, Math.min(100, cookieString.length())));

            return "{\"success\":true,\"message\":\"Cookie string received. Please save cookies to cookies.json file for persistence.\"}";

        } catch (Exception e) {
            log.error("Set cookies failed", e);
            return "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}
