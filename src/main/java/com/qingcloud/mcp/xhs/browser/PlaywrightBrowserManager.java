package com.qingcloud.mcp.xhs.browser;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.qingcloud.mcp.xhs.cookie.CookieManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.List;

/**
 * Playwright 浏览器管理器
 * 负责浏览器生命周期管理、Context 创建和 Cookie 加载
 */
@Service
public class PlaywrightBrowserManager {

    private static final Logger logger = LoggerFactory.getLogger(PlaywrightBrowserManager.class);

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private final CookieManager cookieManager;
    private boolean initialized = false;

    public PlaywrightBrowserManager(CookieManager cookieManager) {
        this.cookieManager = cookieManager;
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
            logger.info("Initializing Playwright browser...");

            // 创建 Playwright 实例，跳过浏览器下载
            Playwright.CreateOptions options = new Playwright.CreateOptions();
            options.setEnv(java.util.Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1"));
            playwright = Playwright.create(options);

            // 启动浏览器
            browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(true)
                            .setArgs(List.of(
                                    "--disable-blink-features=AutomationControlled",
                                    "--disable-dev-shm-usage",
                                    "--no-sandbox")));

            // 创建 Browser Context
            context = browser.newContext(
                    new Browser.NewContextOptions()
                            .setUserAgent(
                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            .setViewportSize(1920, 1080));

            // 加载 cookies
            List<Cookie> cookies = cookieManager.loadCookies();
            if (!cookies.isEmpty()) {
                context.addCookies(cookies);
                logger.info("Loaded {} cookies from file", cookies.size());
            } else {
                logger.info("No cookies found, starting fresh");
            }

            initialized = true;
            logger.info("Playwright browser initialized successfully");

        } catch (Exception e) {
            logger.error("Failed to initialize Playwright browser", e);
            throw new RuntimeException("Failed to initialize browser", e);
        }
    }

    /**
     * 创建新页面
     */
    public Page newPage() {
        if (!initialized) {
            init();
        }

        Page page = context.newPage();
        logger.debug("Created new page");
        return page;
    }

    /**
     * 保存当前 cookies
     */
    public void saveCookies() {
        if (!initialized) {
            logger.warn("Browser not initialized, cannot save cookies");
            return;
        }

        try {
            List<Cookie> cookies = context.cookies();
            cookieManager.saveCookies(cookies);
            logger.info("Saved {} cookies to file", cookies.size());
        } catch (Exception e) {
            logger.error("Failed to save cookies", e);
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
            logger.info("Closing Playwright browser...");

            if (context != null) {
                context.close();
            }
            if (browser != null) {
                browser.close();
            }
            if (playwright != null) {
                playwright.close();
            }

            initialized = false;
            logger.info("Playwright browser closed successfully");

        } catch (Exception e) {
            logger.error("Error closing browser", e);
        }
    }
}
