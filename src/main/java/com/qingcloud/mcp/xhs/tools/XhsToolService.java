package com.qingcloud.mcp.xhs.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import com.qingcloud.mcp.xhs.actions.*;
import com.qingcloud.mcp.xhs.browser.PlaywrightBrowserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * XHS（小红书）MCP Tools 服务 - Spring AI Tool 注解方式
 * 使用 @Tool 注解实现所有小红书功能
 */
@Service
public class XhsToolService {

    private static final Logger log = LoggerFactory.getLogger(XhsToolService.class);

    @Autowired
    private PlaywrightBrowserManager browserManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Page loginPage;
    private Instant loginStartTime;

    /**
     * 二维码登录工具
     */
    @Tool(name = "login", description = "Initiate login process. Returns QR code URL. You must scan the code and then call checkLoginStatus to verify login.")
    public String loginWithQrCode() {
        try {
// log.info("=== Login Tool Called ===");

            // Cleanup any previous session
            if (loginPage != null) {
                try {
// loginPage.close();
                } catch (Exception ignored) {
                }
// loginPage = null;
            }

// loginPage = browserManager.newPage();
// loginStartTime = Instant.now();
            LoginAction loginAction = new LoginAction(loginPage);

            String qrcodeUrl = loginAction.fetchQrcodeImage();

            if (qrcodeUrl == null) {
// log.info("User is already logged in");
// loginPage.close();
// browserManager.saveCookies();
// loginPage = null;
                return "{\"status\":\"already_logged_in\",\"message\":\"User is already logged in\"}";
            }

// log.info("QR code URL: {}", qrcodeUrl);
// log.info("Login initiated. Please scan QR code and then call checkLoginStatus.");

            return "{\"status\":\"pending\",\"qrcodeUrl\":\"" + qrcodeUrl
                    + "\",\"message\":\"Login initiated. Please scan QR code and then call checkLoginStatus.\"}";

        } catch (Exception e) {
// log.error("Login tool failed", e);
            if (loginPage != null) {
                try {
// loginPage.close();
                } catch (Exception ignored) {
                }
// loginPage = null;
            }
            return "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 检查登录状态
     */
    @Tool(name = "checkLoginStatus", description = "Check if the user is currently logged in to Xiaohongshu")
    public String checkLoginStatus() {
        try {
// log.info("=== Check Login Status Tool Called ===");

            // If we have an active login page
            if (loginPage != null) {
                // Check if expired (5 minutes)
                if (Instant.now().isAfter(loginStartTime.plus(5, ChronoUnit.MINUTES))) {
// log.info("Login session expired");
// loginPage.close();
// loginPage = null;
                    return "{\"isLoggedIn\":false,\"message\":\"Login session expired. Please call login tool again.\"}";
                }

                LoginAction loginAction = new LoginAction(loginPage);
                if (loginAction.isLoggedInOnPage()) {
// log.info("✓ Login successful via active session");
// browserManager.saveCookies();
// loginPage.close();
// loginPage = null;
                    return "{\"isLoggedIn\":true,\"message\":\"Login successful\"}";
                } else {
// log.info("Waiting for QR code scan...");
                    return "{\"isLoggedIn\":false,\"message\":\"Waiting for QR code scan...\"}";
                }
            }

            // Fallback to checking global state
            Page page = null;
            try {
// page = browserManager.newPage();
// page.navigate("https://www.xiaohongshu.com/explore");
// page.waitForTimeout(3000);

                boolean isLoggedOut = page.locator("text=登录").count() > 0;

                if (isLoggedOut) {
                    // Double check with another selector just in case
                    boolean hasUserIcon = page.locator(".user").count() > 0;
                    if (hasUserIcon) {
// isLoggedOut = false;
                    }
                }

// page.close();

                if (isLoggedOut) {
// log.info("✗ User is not logged in");
                    return "{\"isLoggedIn\":false,\"message\":\"User is not logged in\"}";
                } else {
// log.info("✓ User is logged in");
                    return "{\"isLoggedIn\":true,\"message\":\"User is logged in\"}";
                }
            } catch (Exception e) {
                if (page != null) {
                    try {
// page.close();
                    } catch (Exception ignored) {
                    }
                }
                throw e;
            }

        } catch (Exception e) {
// log.error("Check login status failed", e);
            return "{\"isLoggedIn\":false,\"message\":\"Error checking login status: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 搜索笔记
     */
    @Tool(name = "searchNotes", description = "Search Xiaohongshu notes by keyword using browser page scraping")
    public String searchNotes(String keyword) {
        Page page = null;
        try {
// log.info("=== Search Notes Tool Called ===");

            if (keyword == null || keyword.trim().isEmpty()) {
                return "{\"code\":-1,\"success\":false,\"message\":\"Keyword is required\"}";
            }

// log.info("Keyword: {}", keyword);

// page = browserManager.newPage();
            SearchAction searchAction = new SearchAction(page);

            List<Map<String, Object>> results = searchAction.search(keyword);

// page.close();

            Map<String, Object> response = new LinkedHashMap<>();
// response.put("code", 0);
// response.put("success", true);
// response.put("data", Map.of(
// "items", results,
// "total", results.size()));

            String responseJson = objectMapper.writeValueAsString(response);

// log.info("✓ Search completed, found {} results", results.size());

            return responseJson;

        } catch (Exception e) {
// log.error("Search tool failed", e);
            if (page != null) {
                try {
// page.close();
                } catch (Exception ignored) {
                }
            }
            return "{\"code\":-1,\"success\":false,\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 获取用户资料和笔记列表
     */
    @Tool(name = "getUserProfile", description = "Get user profile and their notes list. Requires userId and xsecToken (extracted from search result or post detail).")
    public String getUserProfile(String userId, String xsecToken) {
        Page page = null;
        try {
// log.info("=== Get User Profile Tool Called ===");
// log.info("UserId: {}, XsecToken: {}", userId, xsecToken);

// page = browserManager.newPage();
            UserProfileAction action = new UserProfileAction(page);
            Map<String, Object> profile = action.getUserProfile(userId, xsecToken);

// page.close();
            return objectMapper.writeValueAsString(profile);
        } catch (Exception e) {
// log.error("Get user profile failed", e);
            if (page != null) {
                try {
// page.close();
                } catch (Exception ignored) {
                }
            }
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 获取帖子详情
     */
    @Tool(name = "getPostDetail", description = "Get detailed information about a specific note. Requires noteId and xsecToken.")
    public String getPostDetail(String noteId, String xsecToken) {
        Page page = null;
        try {
// log.info("=== Get Post Detail Tool Called ===");
// log.info("NoteId: {}, XsecToken: {}", noteId, xsecToken);

// page = browserManager.newPage();
            PostDetailAction action = new PostDetailAction(page);
            Map<String, Object> detail = action.getPostDetail(noteId, xsecToken);

// page.close();
            return objectMapper.writeValueAsString(detail);
        } catch (Exception e) {
// log.error("Get post detail failed", e);
            if (page != null) {
                try {
// page.close();
                } catch (Exception ignored) {
                }
            }
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 获取信息流
     */
    @Tool(name = "getFeeds", description = "Get personalized feed of notes from Xiaohongshu homepage")
    public String getFeeds() {
        Page page = null;
        try {
// log.info("=== Get Feeds Tool Called ===");

// page = browserManager.newPage();
            FeedsAction feedsAction = new FeedsAction(page);

            List<Map<String, Object>> feeds = feedsAction.getFeeds();

// page.close();

            Map<String, Object> response = new LinkedHashMap<>();
// response.put("code", 0);
// response.put("success", true);
// response.put("data", Map.of(
// "items", feeds,
// "total", feeds.size()));

            String responseJson = objectMapper.writeValueAsString(response);

// log.info("✓ Get feeds completed, found {} items", feeds.size());

            return responseJson;

        } catch (Exception e) {
// log.error("Get feeds failed", e);
            if (page != null) {
                try {
// page.close();
                } catch (Exception ignored) {
                }
            }
            return "{\"code\":-1,\"success\":false,\"message\":\"" + e.getMessage() + "\"}";
        }
    }
}
