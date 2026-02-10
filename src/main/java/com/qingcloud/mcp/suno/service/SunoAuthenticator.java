package com.qingcloud.mcp.suno.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.qingcloud.mcp.suno.common.SunoConstants;
import com.qingcloud.mcp.suno.config.SunoProperties;
import com.qingcloud.mcp.suno.exception.SunoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Suno 认证服务 - 负责管理 Clerk 会话和 JWT Token
 * 仅在配置了 suno.cookie 时启用
 * 
 * @author qingcloud-mcp
 */
@Service
@ConditionalOnProperty(name = "suno.cookie")
public class SunoAuthenticator {

    private static final Logger logger = LoggerFactory.getLogger(SunoAuthenticator.class);

    private final WebClient clerkClient;
    private final SunoProperties sunoProperties;
    private final Map<String, String> cookieMap;

    private String sessionId;
    private String jwtToken;
    private String deviceId;

    public SunoAuthenticator(SunoProperties sunoProperties) {
        this.sunoProperties = sunoProperties;
        this.cookieMap = parseCookies(sunoProperties.getCookie());
        this.deviceId = cookieMap.getOrDefault("ajs_anonymous_id", java.util.UUID.randomUUID().toString());

        this.clerkClient = WebClient.builder()
                .baseUrl(SunoConstants.CLERK_BASE_URL)
                .build();
    }

    /**
     * 解析 Cookie 字符串为 Map
     */
    private Map<String, String> parseCookies(String cookieString) {
        Map<String, String> cookies = new HashMap<>();
        if (cookieString == null || cookieString.isEmpty()) {
            return cookies;
        }

        String[] pairs = cookieString.split(";");
        for (String pair : pairs) {
            String[] keyValue = pair.trim().split("=", 2);
            if (keyValue.length == 2) {
                cookies.put(keyValue[0], keyValue[1]);
            }
        }
        return cookies;
    }

    /**
     * 初始化认证 - 获取 Session ID
     */
    public void initialize() {
        logger.info("Initializing Suno authentication...");

        String clientCookie = cookieMap.get("__client");
        if (clientCookie == null) {
            throw new SunoException("Missing __client cookie", 401);
        }

        // 检测是否为模拟数据
        if (clientCookie.startsWith("mock_")) {
            logger.warn("Using mock Suno cookie - skipping real API initialization");
            this.sessionId = "mock_session_id";
            this.jwtToken = "mock_jwt_token";
            logger.info("Mock authentication initialized");
            return;
        }

        String url = String.format("/v1/client?_is_native=true&_clerk_js_version=%s",
                SunoConstants.CLERK_VERSION);

        try {
            JsonNode response = clerkClient.get()
                    .uri(url)
                    .header("Authorization", clientCookie)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response != null && response.has("response")) {
                JsonNode responseNode = response.get("response");
                if (responseNode.has("last_active_session_id")) {
                    this.sessionId = responseNode.get("last_active_session_id").asText();
                    logger.info("Session ID obtained successfully");

                    // 立即刷新 Token
                    refreshJwt();
                } else {
                    throw new SunoException("Failed to get session ID from response", 401);
                }
            } else {
                throw new SunoException("Invalid response from Clerk API", 500);
            }
        } catch (Exception e) {
            logger.error("Failed to initialize authentication", e);
            throw new SunoException("Authentication initialization failed: " + e.getMessage(), e, 500);
        }
    }

    /**
     * 刷新 JWT Token (KeepAlive)
     */
    public void refreshJwt() {
        if (sessionId == null) {
            throw new SunoException("Session ID not initialized", 401);
        }

        // 如果是模拟模式，跳过真实 API 调用
        if (sessionId.equals("mock_session_id")) {
            logger.debug("Mock mode - skipping JWT refresh");
            return;
        }

        String clientCookie = cookieMap.get("__client");
        String url = String.format("/v1/client/sessions/%s/tokens?_is_native=true&_clerk_js_version=%s",
                sessionId, SunoConstants.CLERK_VERSION);

        try {
            logger.debug("Refreshing JWT token...");

            JsonNode response = clerkClient.post()
                    .uri(url)
                    .header("Authorization", clientCookie)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response != null && response.has("jwt")) {
                this.jwtToken = response.get("jwt").asText();
                logger.debug("JWT token refreshed successfully");
            } else {
                throw new SunoException("Failed to refresh JWT token", 401);
            }
        } catch (Exception e) {
            logger.error("Failed to refresh JWT", e);
            throw new SunoException("JWT refresh failed: " + e.getMessage(), e, 500);
        }
    }

    /**
     * 获取当前有效的 JWT Token
     */
    public String getJwtToken() {
        if (jwtToken == null) {
            refreshJwt();
        }
        return jwtToken;
    }

    /**
     * 获取 Cookie Map
     */
    public Map<String, String> getCookieMap() {
        return cookieMap;
    }

    /**
     * 获取 Device ID
     */
    public String getDeviceId() {
        return deviceId;
    }
}
