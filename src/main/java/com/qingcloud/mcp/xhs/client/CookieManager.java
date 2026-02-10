package com.qingcloud.mcp.xhs.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cookie manager for Xiaohongshu API client.
 * Handles cookie storage, persistence, and session state management.
 */
public class CookieManager {

    private static final Logger logger = LoggerFactory.getLogger(CookieManager.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Path cookieFilePath;
    private final Map<String, String> cookies = new ConcurrentHashMap<>();

    /**
     * Create a CookieManager with specified storage path.
     * 
     * @param storagePath directory to store cookie file
     */
    public CookieManager(String storagePath) {
        this.cookieFilePath = Paths.get(storagePath, "xhs_cookies.json");
        loadCookies();
    }

    /**
     * Create a CookieManager with default storage path (~/.qingcloud-mcp/).
     */
    public CookieManager() {
        this(System.getProperty("user.home") + "/.qingcloud-mcp");
    }

    /**
     * Get a specific cookie value by name.
     */
    public String getCookie(String name) {
        return cookies.get(name);
    }

    /**
     * Get all cookies as a map.
     */
    public Map<String, String> getAllCookies() {
        return new ConcurrentHashMap<>(cookies);
    }

    /**
     * Set a cookie value.
     */
    public void setCookie(String name, String value) {
        cookies.put(name, value);
        saveCookies();
    }

    /**
     * Set multiple cookies at once.
     */
    public void setCookies(Map<String, String> newCookies) {
        cookies.putAll(newCookies);
        saveCookies();
    }

    /**
     * Parse and set cookies from Set-Cookie header values.
     */
    public void parseCookieHeaders(java.util.List<String> setCookieHeaders) {
        if (setCookieHeaders == null)
            return;

        for (String header : setCookieHeaders) {
            // Parse "name=value; Path=/; ..." format
            String[] parts = header.split(";")[0].trim().split("=", 2);
            if (parts.length == 2) {
                cookies.put(parts[0].trim(), parts[1].trim());
            }
        }
        saveCookies();
    }

    /**
     * Build Cookie header string for HTTP requests.
     */
    public String buildCookieHeader() {
        if (cookies.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        cookies.forEach((name, value) -> {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(name).append("=").append(value);
        });
        return sb.toString();
    }

    /**
     * Check if user is logged in (has essential cookies).
     */
    public boolean isLoggedIn() {
        // Xiaohongshu uses these cookies to identify logged-in users
        return cookies.containsKey("web_session") ||
                cookies.containsKey("xsec_token") ||
                (cookies.containsKey("a1") && cookies.containsKey("webId"));
    }

    /**
     * Get essential cookies for API requests.
     */
    public String getA1() {
        return cookies.getOrDefault("a1", "");
    }

    public String getWebSession() {
        return cookies.getOrDefault("web_session", "");
    }

    public String getWebId() {
        return cookies.getOrDefault("webId", "");
    }

    /**
     * Clear all cookies and remove the stored file.
     */
    public void clearCookies() {
        cookies.clear();
        try {
            Files.deleteIfExists(cookieFilePath);
            logger.info("Cookies cleared");
        } catch (IOException e) {
            logger.warn("Failed to delete cookie file: {}", e.getMessage());
        }
    }

    /**
     * Load cookies from persistent storage.
     */
    private void loadCookies() {
        try {
            if (Files.exists(cookieFilePath)) {
                String json = Files.readString(cookieFilePath);
                Map<String, String> loaded = objectMapper.readValue(json,
                        new TypeReference<Map<String, String>>() {
                        });
                cookies.putAll(loaded);
                logger.info("Loaded {} cookies from {}", cookies.size(), cookieFilePath);
            }
        } catch (IOException e) {
            logger.warn("Failed to load cookies: {}", e.getMessage());
        }
    }

    /**
     * Save cookies to persistent storage.
     */
    private void saveCookies() {
        try {
            // Ensure parent directory exists
            Files.createDirectories(cookieFilePath.getParent());
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(cookies);
            Files.writeString(cookieFilePath, json);
            logger.debug("Saved {} cookies to {}", cookies.size(), cookieFilePath);
        } catch (IOException e) {
            logger.warn("Failed to save cookies: {}", e.getMessage());
        }
    }
}
