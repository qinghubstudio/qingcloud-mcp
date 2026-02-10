package com.qingcloud.mcp.suno.browser;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.qingcloud.mcp.suno.config.SunoProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Suno 浏览器管理器
 * 负责 Playwright 浏览器生命周期管理
 * 
 * @author qingcloud-mcp
 */
@Service
public class SunoBrowserManager {

    private static final Logger logger = LoggerFactory.getLogger(SunoBrowserManager.class);

    private Playwright playwright;
    private Browser browser;
    private final SunoProperties sunoProperties;
    private boolean initialized = false;

    public SunoBrowserManager(SunoProperties sunoProperties) {
        this.sunoProperties = sunoProperties;
    }

    /**
     * 初始化浏览器
     */
    public synchronized void init() {
        if (initialized) {
            logger.debug("Browser already initialized");
            return;
        }

        try {
            logger.info("Initializing Suno Playwright browser...");

            // 创建 Playwright 实例
            playwright = Playwright.create();

            // 启动浏览器
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(sunoProperties.getBrowser().isHeadless())
                    .setArgs(List.of(
                            "--disable-blink-features=AutomationControlled",
                            "--disable-dev-shm-usage",
                            "--no-sandbox"));

            browser = playwright.chromium().launch(launchOptions);

            initialized = true;
            logger.info("Suno Playwright browser initialized successfully (headless: {})",
                    sunoProperties.getBrowser().isHeadless());

        } catch (Exception e) {
            logger.error("Failed to initialize Suno Playwright browser", e);
            throw new RuntimeException("Failed to initialize Suno browser", e);
        }
    }

    /**
     * 创建新的 BrowserContext (带 Cookie)
     */
    public BrowserContext newContext() {
        if (!initialized) {
            init();
        }

        try {
            // 解析 Cookie
            List<Cookie> cookies = parseSunoCookies();

            // 创建 Context
            Browser.NewContextOptions options = new Browser.NewContextOptions()
                    .setUserAgent(
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .setViewportSize(1920, 1080)
                    .setLocale(sunoProperties.getBrowser().getLocale());

            BrowserContext context = browser.newContext(options);

            // 添加 Cookies
            if (!cookies.isEmpty()) {
                context.addCookies(cookies);
                logger.debug("Added {} cookies to context", cookies.size());
            }

            return context;

        } catch (Exception e) {
            logger.error("Failed to create browser context", e);
            throw new RuntimeException("Failed to create browser context", e);
        }
    }

    /**
     * 解析 Suno Cookie 字符串为 Playwright Cookie 列表
     */
    private List<Cookie> parseSunoCookies() {
        String cookieString = sunoProperties.getCookie();
        if (cookieString == null || cookieString.isEmpty()) {
            logger.warn("No Suno cookie configured");
            return List.of();
        }

        try {
            String[] pairs = cookieString.split(";");
            return java.util.Arrays.stream(pairs)
                    .map(String::trim)
                    .filter(pair -> !pair.isEmpty())
                    .map(pair -> {
                        String[] keyValue = pair.split("=", 2);
                        if (keyValue.length == 2) {
                            Cookie cookie = new Cookie(keyValue[0].trim(), keyValue[1].trim());
                            cookie.domain = ".suno.com";
                            cookie.path = "/";
                            return cookie;
                        }
                        return null;
                    })
                    .filter(cookie -> cookie != null)
                    .toList();
        } catch (Exception e) {
            logger.error("Failed to parse Suno cookies", e);
            return List.of();
        }
    }

    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 关闭浏览器
     */
    @PreDestroy
    public synchronized void close() {
        if (!initialized) {
            return;
        }

        try {
            logger.info("Closing Suno Playwright browser...");

            if (browser != null) {
                browser.close();
            }
            if (playwright != null) {
                playwright.close();
            }

            initialized = false;
            logger.info("Suno Playwright browser closed successfully");

        } catch (Exception e) {
            logger.error("Error closing Suno browser", e);
        }
    }
}
