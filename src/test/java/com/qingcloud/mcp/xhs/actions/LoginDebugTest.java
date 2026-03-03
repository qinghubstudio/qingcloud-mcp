package com.qingcloud.mcp.xhs.actions;

import com.microsoft.playwright.Page;
import com.qingcloud.mcp.xhs.browser.PlaywrightBrowserManager;
import com.qingcloud.mcp.xhs.cookie.CookieManager;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class LoginDebugTest {

    @Test
    public void testLogin() {
        System.out.println("Starting Login Debug Test...");

        File cookieFile = new File("cookies.json");
        if (!cookieFile.exists()) {
            System.out.println("cookies.json not found, starting fresh.");
        } else {
            System.out.println("cookies.json exists, size: " + cookieFile.length());
        }

        CookieManager cookieManager = new CookieManager();
        PlaywrightBrowserManager browserManager = new PlaywrightBrowserManager(cookieManager);

        try {
            browserManager.init();
            Page page = browserManager.newPage();

            LoginAction loginAction = new LoginAction(page);

            // Check status first
            if (loginAction.checkLoginStatus()) {
                System.out.println("Already logged in!");
                browserManager.saveCookies();
                return;
            }

            System.out.println("Not logged in. Fetching QR code...");
            String qrSrc = loginAction.fetchQrcodeImage();

            if (qrSrc != null) {
                System.out.println("QR Code Found.");

                // Handle Base64 QR Code
                if (qrSrc.startsWith("data:image")) {
                    String base64Image = qrSrc.split(",")[1];
                    byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                    Files.write(Paths.get("qrcode.png"), imageBytes);
                    System.out.println("QR Code saved to: " + new File("qrcode.png").getAbsolutePath());
                } else {
                    System.out.println("QR Code URL: " + qrSrc);
                }

                // Stealth: Move mouse a bit
                page.mouse().move(100, 100);
                page.mouse().move(200, 300);

                System.out.println("WAITING FOR USER TO SCAN QR CODE (180s timeout)...");
                boolean success = loginAction.waitForLogin(180);

                if (success) {
                    System.out.println("Login Successful! Saving cookies...");
                    browserManager.saveCookies();
                    System.out.println("Cookies saved to cookies.json");
                } else {
                    System.err.println("Login timed out. Taking screenshot...");
                    page.screenshot(
                            new Page.ScreenshotOptions().setPath(Paths.get("login_error.png")).setFullPage(true));
                    System.err.println("Screenshot saved to login_error.png");
                }
            } else {
                System.err.println("Failed to get QR code. Taking screenshot...");
                page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("login_error.png")).setFullPage(true));
                System.err.println("Screenshot saved to login_error.png");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            browserManager.close();
        }
    }
}
