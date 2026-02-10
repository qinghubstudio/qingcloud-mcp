package com.qingcloud.mcp.xhs.actions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Feeds 操作
 * 获取首页推荐内容
 */
public class FeedsAction {

    private static final Logger logger = LoggerFactory.getLogger(FeedsAction.class);
    private static final String EXPLORE_URL = "https://www.xiaohongshu.com/explore";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Page page;

    public FeedsAction(Page page) {
        this.page = page;
    }

    /**
     * 获取首页推荐 feeds
     * 
     * @return Feeds 列表
     */
    public List<Map<String, Object>> getFeeds() {
        try {
            logger.info("=== Feeds Action ===");

            logger.info("Navigating to explore page: {}", EXPLORE_URL);

            // 导航到探索页面
            page.navigate(EXPLORE_URL, new Page.NavigateOptions().setTimeout(60000));

            // 等待页面加载
            try {
                page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(30000));
            } catch (Exception e) {
                logger.warn("Page load state timeout, continuing anyway: {}", e.getMessage());
            }

            logger.info("Page loaded, current URL: {}", page.url());
            logger.info("Page title: {}", page.title());
            logger.info("Waiting for __INITIAL_STATE__...");

            // 等待 __INITIAL_STATE__ 可用
            page.waitForFunction("() => window.__INITIAL_STATE__ !== undefined");

            logger.info("__INITIAL_STATE__ available, extracting feeds...");

            // 从 window.__INITIAL_STATE__ 提取 feeds
            Object result = page.evaluate("""
                    () => {
                        if (window.__INITIAL_STATE__ &&
                            window.__INITIAL_STATE__.feed &&
                            window.__INITIAL_STATE__.feed.feeds) {
                            const feeds = window.__INITIAL_STATE__.feed.feeds;
                            const feedsData = feeds.value !== undefined ? feeds.value : feeds._value;
                            if (feedsData) {
                                return JSON.stringify(feedsData);
                            }
                        }
                        return "";
                    }
                    """);

            if (result == null || result.toString().isEmpty()) {
                logger.warn("No feeds found in __INITIAL_STATE__");
                logger.warn("Dumping __INITIAL_STATE__ structure...");
                Object stateDump = page.evaluate("() => JSON.stringify(Object.keys(window.__INITIAL_STATE__ || {}))");
                logger.warn("__INITIAL_STATE__ keys: {}", stateDump);
                return List.of();
            }

            logger.info("Raw result length: {}", result.toString().length());

            // 解析 JSON
            List<Map<String, Object>> feeds = objectMapper.readValue(
                    result.toString(),
                    new TypeReference<List<Map<String, Object>>>() {
                    });

            logger.info("✓ Successfully extracted {} feeds", feeds.size());
            return feeds;

        } catch (Exception e) {
            logger.error("Failed to get feeds", e);
            return List.of();
        }
    }
}
