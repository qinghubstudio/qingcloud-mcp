package com.qingcloud.mcp.xhs.actions;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 登录操作
 * 负责处理小红书登录相关的浏览器操作
 */
public class LoginAction {

    private static final Logger logger = LoggerFactory.getLogger(LoginAction.class);
    private static final String XHS_EXPLORE_URL = "https://www.xiaohongshu.com/explore";
    private static final String LOGIN_STATUS_SELECTOR = ".main-container .user .link-wrapper .channel";
    private static final String QRCODE_SELECTOR = ".login-container .qrcode-img";

    private final Page page;

    public LoginAction(Page page) {
        this.page = page;
    }

    /**
     * 检查登录状态
     * 
     * @return true 如果已登录, false 如果未登录
     */
    public boolean checkLoginStatus() {
        try {
            logger.info("Checking login status...");

            // 导航到探索页面,增加超时时间
            page.navigate(XHS_EXPLORE_URL, new Page.NavigateOptions().setTimeout(60000));

            // 等待页面加载,使用更宽松的条件
            try {
                page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(30000));
            } catch (Exception e) {
                logger.warn("Page load state timeout, continuing anyway: {}", e.getMessage());
            }

            // 等待一小段时间让页面完全加载
            page.waitForTimeout(2000);

            // 检查登录状态元素是否存在
            Locator loginElement = page.locator(LOGIN_STATUS_SELECTOR);
            boolean isLoggedIn = loginElement.count() > 0;

            logger.info("Login status: {}", isLoggedIn ? "LOGGED_IN" : "NOT_LOGGED_IN");
            return isLoggedIn;

        } catch (Exception e) {
            logger.error("Failed to check login status", e);
            return false;
        }
    }

    /**
     * 获取二维码图片 URL
     * 
     * @return 二维码图片 URL, 如果已登录则返回 null
     */
    public String fetchQrcodeImage() {
        try {
            logger.info("Fetching QR code image...");

            // 导航到探索页面
            page.navigate(XHS_EXPLORE_URL);

            // 使用更宽松的等待条件,避免已登录时超时
            try {
                page.waitForLoadState(LoadState.DOMCONTENTLOADED,
                        new Page.WaitForLoadStateOptions().setTimeout(10000));
            } catch (Exception e) {
                logger.warn("Page load state timeout, continuing anyway: {}", e.getMessage());
            }

            // 等待一小段时间让页面完全加载
            page.waitForTimeout(2000);

            // 检查是否已登录
            Locator loginElement = page.locator(LOGIN_STATUS_SELECTOR);
            if (loginElement.count() > 0) {
                logger.info("Already logged in, no QR code needed");
                return null;
            }

            // 获取二维码图片
            Locator qrcodeElement = page.locator(QRCODE_SELECTOR);
            if (qrcodeElement.count() == 0) {
                logger.warn("QR code element not found");
                return null;
            }

            String src = qrcodeElement.getAttribute("src");
            logger.info("QR code image URL: {}",
                    src != null ? src.substring(0, Math.min(50, src.length())) + "..." : "null");

            return src;

        } catch (Exception e) {
            logger.error("Failed to fetch QR code image", e);
            return null;
        }
    }

    /**
     * 等待用户扫码登录
     * 
     * @param timeoutSeconds 超时时间(秒)
     * @return true 如果登录成功, false 如果超时
     */
    public boolean waitForLogin(int timeoutSeconds) {
        try {
            logger.info("Waiting for login (timeout: {}s)...", timeoutSeconds);

            long endTime = System.currentTimeMillis() + timeoutSeconds * 1000L;

            while (System.currentTimeMillis() < endTime) {
                // 检查登录状态元素是否出现
                Locator loginElement = page.locator(LOGIN_STATUS_SELECTOR);
                if (loginElement.count() > 0) {
                    logger.info("✓ Login successful!");
                    return true;
                }

                // 等待 500ms 后再检查
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    logger.warn("Wait interrupted", e);
                    return false;
                }
            }

            logger.warn("✗ Login timeout after {}s", timeoutSeconds);
            return false;

        } catch (Exception e) {
            logger.error("Failed to wait for login", e);
            return false;
        }
    }
}
