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

    /**
     * 二维码登录工具
     */
    @Tool(name = "login", description = "Get QR code for Xiaohongshu login and wait for user to scan. Returns QR code image URL or login status.")
    public String loginWithQrCode() {
        Page page = null;
        try {
            log.info("=== Login Tool Called ===");

            page = browserManager.newPage();
            LoginAction loginAction = new LoginAction(page);

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
            page.navigate("https://www.xiaohongshu.com/explore");
            page.waitForTimeout(3000);

            boolean isLoggedOut = page.locator("text=登录").count() > 0;

            page.close();

            if (isLoggedOut) {
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
     * 搜索笔记
     */
    @Tool(name = "searchNotes", description = "Search Xiaohongshu notes by keyword using browser page scraping")
    public String searchNotes(String keyword) {
        Page page = null;
        try {
            log.info("=== Search Notes Tool Called ===");

            if (keyword == null || keyword.trim().isEmpty()) {
                return "{\"code\":-1,\"success\":false,\"message\":\"Keyword is required\"}";
            }

            log.info("Keyword: {}", keyword);

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
}
