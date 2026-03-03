package com.qingcloud.mcp.xhs.actions;

import com.microsoft.playwright.Page;
import com.qingcloud.mcp.xhs.browser.PlaywrightBrowserManager;
import com.qingcloud.mcp.xhs.cookie.CookieManager;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;
import java.util.Map;

public class SearchDebugTest {

    @Test
    public void testSearch() {
        System.out.println("Starting Search Debug Test...");

        // Ensure we are in the right directory or provide path to cookies.json
        File cookieFile = new File("cookies.json");
        System.out.println("Looking for cookies.json at: " + cookieFile.getAbsolutePath());
        if (!cookieFile.exists()) {
            System.err.println("cookies.json not found! Please run from workspace root.");
            // Try to find it in likely locations
            if (new File("/u01/jenkins/workspace/qingcloud-mcp/cookies.json").exists()) {
                System.out.println("Found it in absolute path, but CookieManager uses relative path.");
                // We might need to handle this if simple instantiation fails to find it.
                // But let's assume mvn test runs from project root.
            }
        }

        CookieManager cookieManager = new CookieManager();
        PlaywrightBrowserManager browserManager = new PlaywrightBrowserManager(cookieManager);

        try {
            browserManager.init();
            Page page = browserManager.newPage();

            SearchAction searchAction = new SearchAction(page);
            List<Map<String, Object>> results = searchAction.search("重庆小升初");

            System.out.println("Search Results Count: " + results.size());
            // Print first few results if any
            if (!results.isEmpty()) {
                System.out.println("First result: " + results.get(0));
            }

            // Dump HTML for debugging
            try {
                java.nio.file.Files.writeString(
                        java.nio.file.Path.of("debug_search_page.html"),
                        page.content());
                System.out.println("Dumped HTML to debug_search_page.html");

                // Also screenshot if possible (png)
                page.screenshot(new Page.ScreenshotOptions().setPath(java.nio.file.Path.of("debug_search_page.png")));
                System.out.println("Saved screenshot to debug_search_page.png");
            } catch (Exception e) {
                System.err.println("Failed to dump debug info: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            browserManager.close();
        }
    }
}
