package com.qingcloud.mcp.xhs.cookie;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.options.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cookie 管理器
 * 负责 Cookie 的持久化存储和加载
 */
@Component
public class CookieManager {

    private static final Logger logger = LoggerFactory.getLogger(CookieManager.class);
    private static final String COOKIE_FILE = "cookies.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从文件加载 cookies
     * 支持两种格式:
     * 1. Playwright Cookie 格式
     * 2. 浏览器导出格式(包含 expirationDate, hostOnly 等字段)
     */
    public List<Cookie> loadCookies() {
        File file = new File(COOKIE_FILE);
        logger.info("=== Loading cookies from: {} ===", file.getAbsolutePath());
        logger.info("File exists: {}", file.exists());

        if (!file.exists()) {
            logger.warn("Cookie file not found: {}", COOKIE_FILE);
            return new ArrayList<>();
        }

        try {
            // 先读取为通用 Map 列表
            List<Map<String, Object>> rawCookies = objectMapper.readValue(
                    file,
                    new TypeReference<List<Map<String, Object>>>() {
                    });

            List<Cookie> cookies = new ArrayList<>();
            for (Map<String, Object> rawCookie : rawCookies) {
                try {
                    Cookie cookie = convertToCookie(rawCookie);
                    if (cookie != null) {
                        cookies.add(cookie);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to convert cookie: {}", rawCookie.get("name"), e);
                }
            }

            logger.info("Loaded {} cookies from {}", cookies.size(), COOKIE_FILE);
            return cookies;
        } catch (IOException e) {
            logger.warn("Failed to load cookies from {}: {}", COOKIE_FILE, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 将浏览器导出格式转换为 Playwright Cookie 格式
     */
    private Cookie convertToCookie(Map<String, Object> rawCookie) {
        String name = (String) rawCookie.get("name");
        String value = (String) rawCookie.get("value");
        String domain = (String) rawCookie.get("domain");
        String path = (String) rawCookie.get("path");

        if (name == null || value == null) {
            return null;
        }

        // 创建 Cookie 对象
        Cookie cookie = new Cookie(name, value);

        // 设置 domain
        if (domain != null) {
            cookie.setDomain(domain);
        }

        // 设置 path
        if (path != null) {
            cookie.setPath(path);
        }

        // 设置 expires (从 expirationDate 转换)
        Object expirationDate = rawCookie.get("expirationDate");
        if (expirationDate != null) {
            double expiration = ((Number) expirationDate).doubleValue();
            cookie.setExpires(expiration);
        } else {
            cookie.setExpires(-1); // 会话 cookie
        }

        // 设置 httpOnly
        Object httpOnly = rawCookie.get("httpOnly");
        if (httpOnly instanceof Boolean) {
            cookie.setHttpOnly((Boolean) httpOnly);
        }

        // 设置 secure
        Object secure = rawCookie.get("secure");
        if (secure instanceof Boolean) {
            cookie.setSecure((Boolean) secure);
        }

        // Note: sameSite is set automatically by Playwright based on the cookie
        // properties
        // We don't need to set it explicitly

        return cookie;
    }

    /**
     * 保存 cookies 到文件
     */
    public void saveCookies(List<Cookie> cookies) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(COOKIE_FILE), cookies);
            logger.info("Saved {} cookies to {}", cookies.size(), COOKIE_FILE);
        } catch (IOException e) {
            logger.error("Failed to save cookies to {}: {}", COOKIE_FILE, e.getMessage(), e);
        }
    }

    /**
     * 删除 cookie 文件
     */
    public void deleteCookies() {
        File file = new File(COOKIE_FILE);
        if (file.exists()) {
            if (file.delete()) {
                logger.info("Deleted cookie file: {}", COOKIE_FILE);
            } else {
                logger.warn("Failed to delete cookie file: {}", COOKIE_FILE);
            }
        } else {
            logger.debug("Cookie file not found, nothing to delete");
        }
    }

    /**
     * 检查 cookie 文件是否存在
     */
    public boolean cookiesExist() {
        return new File(COOKIE_FILE).exists();
    }
}
