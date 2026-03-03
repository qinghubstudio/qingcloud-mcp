package com.qingcloud.mcp.xhs.actions;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 登录操作
 * 负责处理小红书登录相关的浏览器操作
 */
public class LoginAction {

    private static final Logger logger = LoggerFactory.getLogger(LoginAction.class);
    private static final String XHS_EXPLORE_URL = "https://www.xiaohongshu.com/explore";
    private static final String LOGIN_STATUS_SELECTOR = ".main-container .user .link-wrapper .channel";
    private static final String QRCODE_SELECTOR = ".login-container .qrcode-img, .qrcode-img, img[src*='qrcode'], .captcha-qrcode, .qrcode-container img";

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

            // DEBUG: Log cookie count to verify they're loaded
            int cookieCount = page.context().cookies().size();
            logger.info("DEBUG: Current cookie count in context: {}", cookieCount);

            // PRIMARY CHECK: Validate critical cookies
            List<Cookie> cookies = page.context().cookies();
            boolean hasWebSession = cookies.stream()
                    .anyMatch(c -> "web_session".equals(c.name) && c.value != null && !c.value.isEmpty());
            boolean hasA1 = cookies.stream()
                    .anyMatch(c -> "a1".equals(c.name) && c.value != null && !c.value.isEmpty());

            logger.info("Cookie check - web_session: {}, a1: {}", hasWebSession, hasA1);

            // If critical cookies are present, consider logged in
            if (hasWebSession || hasA1) {
                logger.info("Login status: LOGGED_IN (via cookies)");
                return true;
            }

            // FALLBACK: Check DOM elements
            boolean isLoggedIn = false;

            // 1. Original selector
            if (page.locator(LOGIN_STATUS_SELECTOR).count() > 0) {
                logger.info("Login check passed: LOGIN_STATUS_SELECTOR found");
                isLoggedIn = true;
            }

            // 2. Avatar in side bar
            if (!isLoggedIn && page.locator(".side-bar .user-avatar").count() > 0) {
                logger.info("Login check passed: Avatar found");
                isLoggedIn = true;
            }

            // 3. User name element
            if (!isLoggedIn && page.locator(".side-bar .user-name").count() > 0) {
                logger.info("Login check passed: User name found");
                isLoggedIn = true;
            }

            logger.info("Login status: {}", isLoggedIn ? "LOGGED_IN" : "NOT_LOGGED_IN");
            return isLoggedIn;

        } catch (Exception e) {
            logger.error("Failed to check login status", e);
            return false;
        }
    }

    /**
     * 检查当前页面是否包含登录后的标识（不导航）
     */
    public boolean hasLoggedInIndicator() {
        try {
            // Priority 1: Check for verification modal (MUST be cleared)
            if (isVerificationModalVisible()) {
                logger.info("Verification modal (Safety Verification) is visible. Waiting...");
                return false;
            }

            // Priority 2: Check for critical cookies
            List<com.microsoft.playwright.options.Cookie> cookies = page.context().cookies();
            boolean hasWebSession = cookies.stream()
                    .anyMatch(c -> "web_session".equals(c.name) && c.value != null && !c.value.isEmpty());

            // Priority 3: Check for UI indicators
            int mainStatus = page.locator(LOGIN_STATUS_SELECTOR).count();
            int avatars = page.locator(".side-bar .user-avatar, .main-container .user .user-avatar, .user-avatar")
                    .count();
            int names = page.locator(".side-bar .user-name, .user-name").count();

            boolean hasUIStats = (mainStatus > 0 || avatars > 0 || names > 0);

            // Success condition: Must have session cookie AND UI confirmed
            // This prevents false positives from anonymous web_session cookies
            if (hasWebSession && hasUIStats) {
                logger.info("Login confirmed via web_session cookie AND UI elements.");
                return true;
            }

            // If we have web_session but NO UI yet, wait a bit more unless we are sure we
            // are logged in
            if (hasWebSession && page.url().contains("/explore") && mainStatus > 0) {
                logger.info("Login confirmed via web_session cookie and explorer sidebar.");
                return true;
            }

            // If only sidebar is found, it's not a definitive success
            int sideBar = page.locator(".side-bar").count();
            if (sideBar > 0) {
                logger.debug("Only SideBar found ({}), waiting for more indicators...", sideBar);
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查是否显示“扫码验证”弹窗
     */
    public boolean isVerificationModalVisible() {
        try {
            // Check for explicit captcha elements
            if (page.locator("text=扫码验证, .captcha-modal, .red-captcha-container, .verify-modal").count() > 0) {
                return true;
            }
            // Check for titles in modals
            Locator modalTitle = page.locator("h2, .title, .modal-title");
            for (int i = 0; i < modalTitle.count(); i++) {
                String text = modalTitle.nth(i).innerText();
                if (text != null && (text.contains("扫码验证") || text.contains("校验") || text.contains("行为验证"))) {
                    return true;
                }
            }
            // Check for iframe based captchas
            if (page.locator("iframe[src*='captcha']").count() > 0) {
                return true;
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
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
            String src = getCurrentQrcodeImage();
            if (src == null) {
                logger.warn("QR code element not found via multiple selectors");
                return null;
            }

            logger.info("QR code image URL: {}", src);
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
            String lastUrl = "";
            String lastQr = getCurrentQrcodeImage();

            while (System.currentTimeMillis() < endTime) {
                String currentUrl = page.url();
                if (!currentUrl.equals(lastUrl)) {
                    logger.info("Current URL: {}", currentUrl);
                    lastUrl = currentUrl;
                }

                // Check for QR code refresh (Multi-step scan handling)
                String currentQr = getCurrentQrcodeImage();
                if (currentQr != null && !currentQr.equals(lastQr)) {
                    logger.info("QR code refreshed/changed! New QR code image URL: {}", currentQr);
                    lastQr = currentQr;
                }

                // Check for verification modal
                if (isVerificationModalVisible()) {
                    logger.info("Verification modal is visible. Waiting for scan...");
                }

                // Check for login success
                if (hasLoggedInIndicator()) {
                    logger.info("✓ Login successful and all verification modals cleared.");
                    return true;
                }

                // If on captcha page and NO QR code is found, it might have just finished
                // scanning
                if (isCaptchaPage() && getCurrentQrcodeImage() == null) {
                    logger.info("Captcha QR disappeared, checking for redirect/success...");
                    // Wait a bit for potential redirect
                    page.waitForTimeout(2000);
                    if (hasLoggedInIndicator())
                        return true;
                }

                // Small delay
                try {
                    Thread.sleep(2000);
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

    /**
     * 检查当前是否处于“安全验证”页面
     * 
     * @return true 如果在验证页面, false 否则
     */
    public boolean isCaptchaPage() {
        String url = page.url();
        return url.contains("website-login/captcha") || url.contains("captcha");
    }

    /**
     * 获取当前页面上的二维码图片 URL (不进行额外导航)
     * 
     * @return 二维码图片 URL, 如果未找到返回 null
     */
    public String getCurrentQrcodeImage() {
        try {
            // 首先尝试通用的选择器
            Locator qrcodeElement = page.locator(QRCODE_SELECTOR).first();
            if (qrcodeElement.count() > 0) {
                return qrcodeElement.getAttribute("src");
            }

            // 如果没找到，尝试在整个页面寻找 src 包含 qrcode 的 img
            Locator allImgs = page.locator("img");
            for (int i = 0; i < allImgs.count(); i++) {
                String src = allImgs.nth(i).getAttribute("src");
                if (src != null && src.contains("qrcode")) {
                    return src;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to get current QR code: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 执行退出登录操作并清理所有缓存
     */
    public void logout() {
        try {
            logger.info("Attempting aggressive logout and cache clearing...");

            // 1. Try to click logout if button is visible
            try {
                if (page.url().contains("xiaohongshu.com")) {
                    // Hover avatar to show menu
                    Locator avatar = page.locator(".side-bar .user-avatar, .main-container .user .user-avatar");
                    if (avatar.count() > 0) {
                        avatar.first().hover();
                        page.waitForTimeout(500);
                        Locator logoutBtn = page.locator("text=退出登录, .logout-btn");
                        if (logoutBtn.count() > 0) {
                            logoutBtn.first().click();
                            logger.info("Logout button clicked.");
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Traditional logout failed: {}", e.getMessage());
            }

            // 2. Aggressive clearing via JS
            page.evaluate("() => { " +
                    "localStorage.clear(); " +
                    "sessionStorage.clear(); " +
                    "document.cookie.split(';').forEach(c => { " +
                    "  document.cookie = c.replace(/^ +/, '').replace(/=.*/, '=;expires=' + new Date().toUTCString() + ';path=/'); "
                    +
                    "}); " +
                    "}");
            logger.info("LocalStorage, SessionStorage and Cookies cleared via JS.");

            page.waitForTimeout(1000);
        } catch (Exception e) {
            logger.warn("Aggressive clearing failed: {}", e.getMessage());
        }
    }
}
