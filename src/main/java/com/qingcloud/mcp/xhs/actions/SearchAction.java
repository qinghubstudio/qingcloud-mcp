package com.qingcloud.mcp.xhs.actions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 搜索操作
 * 通过页面抓取方式获取搜索结果,避免 API 签名问题
 */
public class SearchAction {

    private static final Logger logger = LoggerFactory.getLogger(SearchAction.class);
    private static final String SEARCH_URL_TEMPLATE = "https://www.xiaohongshu.com/search_result?keyword=%s&source=web_search_result_notes";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Page page;

    public SearchAction(Page page) {
        this.page = page;
    }

    /**
     * 搜索笔记
     * 
     * @param keyword 搜索关键词
     * @return 搜索结果列表
     */
    public List<Map<String, Object>> search(String keyword) {
        try {
            logger.info("=== Search Action: {} ===", keyword);

            // 构建搜索 URL
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String searchUrl = String.format(SEARCH_URL_TEMPLATE, encodedKeyword);

            logger.info("Navigating to search page: {}", searchUrl);

            // 导航到搜索页面,增加超时时间
            page.navigate(searchUrl, new Page.NavigateOptions().setTimeout(60000));

            // 等待页面加载,使用更宽松的条件
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

            logger.info("__INITIAL_STATE__ available, extracting search results...");

            // 先检查 __INITIAL_STATE__ 的结构
            Object stateCheck = page.evaluate("""
                    () => {
                        if (!window.__INITIAL_STATE__) return "NO_INITIAL_STATE";
                        if (!window.__INITIAL_STATE__.search) return "NO_SEARCH";
                        if (!window.__INITIAL_STATE__.search.feeds) return "NO_FEEDS";
                        const feeds = window.__INITIAL_STATE__.search.feeds;
                        const feedsData = feeds.value !== undefined ? feeds.value : feeds._value;
                        if (!feedsData) return "NO_FEEDS_DATA";
                        return "OK:" + feedsData.length;
                    }
                    """);

            logger.info("State check result: {}", stateCheck);

            // 从 window.__INITIAL_STATE__ 提取搜索结果
            Object result = page.evaluate("""
                    () => {
                        if (window.__INITIAL_STATE__ &&
                            window.__INITIAL_STATE__.search &&
                            window.__INITIAL_STATE__.search.feeds) {
                            const feeds = window.__INITIAL_STATE__.search.feeds;
                            const feedsData = feeds.value !== undefined ? feeds.value : feeds._value;
                            if (feedsData) {
                                return JSON.stringify(feedsData);
                            }
                        }
                        return "";
                    }
                    """);

            if (result == null || result.toString().isEmpty()) {
                logger.warn("No search results found in __INITIAL_STATE__");
                logger.warn("Dumping __INITIAL_STATE__ structure...");
                Object stateDump = page.evaluate("() => JSON.stringify(Object.keys(window.__INITIAL_STATE__ || {}))");
                logger.warn("__INITIAL_STATE__ keys: {}", stateDump);
                return List.of();
            }

            logger.info("Raw result length: {}", result.toString().length());
            logger.debug("Raw result preview: {}",
                    result.toString().substring(0, Math.min(200, result.toString().length())));

            // 解析 JSON
            List<Map<String, Object>> feeds = objectMapper.readValue(
                    result.toString(),
                    new TypeReference<List<Map<String, Object>>>() {
                    });

            if (feeds.isEmpty()) {
                logger.warn("Initial state feeds is empty, trying DOM scraping...");

                // DOM Scraping Fallback
                try {
                    // Wait for feed items to appear
                    try {
                        page.waitForSelector(".feeds-container .note-item",
                                new Page.WaitForSelectorOptions().setTimeout(5000));
                    } catch (Exception e) {
                        logger.warn("Timeout waiting for .note-item selector");
                    }

                    List<Map<String, Object>> domFeeds = (List<Map<String, Object>>) page.evaluate(
                            """
                                    () => {
                                        const items = document.querySelectorAll('.feeds-container .note-item');
                                        const results = [];
                                        items.forEach(item => {
                                            const titleEl = item.querySelector('.title, .note-title');
                                            const userEl = item.querySelector('.user .name, .author .name, .nickname');
                                            const linkEl = item.querySelector('a.cover, a.info, a');
                                            const imgEl = item.querySelector('img');

                                            if (titleEl && linkEl) {
                                                const href = linkEl.href;
                                                // Extract noteId from https://www.xiaohongshu.com/search_result/68b0f334000000001d036cb1...
                                                // or https://www.xiaohongshu.com/explore/68b0f334000000001d036cb1...
                                                let noteId = "";
                                                const noteIdMatch = href.match(/\\/(?:search_result|explore)\\/([a-zA-Z0-9]+)/);
                                                if (noteIdMatch) noteId = noteIdMatch[1];

                                                // Extract xsec_token
                                                let xsecToken = "";
                                                const xsecMatch = href.match(/xsec_token=([^&]+)/);
                                                if (xsecMatch) xsecToken = xsecMatch[1];

                                                results.push({
                                                    title: titleEl.innerText.trim(),
                                                    user: userEl ? userEl.innerText.trim() : '',
                                                    link: href,
                                                    noteId: noteId,
                                                    xsecToken: xsecToken,
                                                    cover: imgEl ? imgEl.src : ''
                                                });
                                            }
                                        });
                                        return results;
                                    }
                                    """);

                    if (!domFeeds.isEmpty()) {
                        logger.info("✓ Successfully scraped {} items from DOM", domFeeds.size());
                        return domFeeds;
                    } else {
                        logger.warn("DOM scraping also returned 0 items.");
                    }
                } catch (Exception e) {
                    logger.error("DOM scraping failed", e);
                }
            }

            logger.info("✓ Successfully extracted {} search results from state", feeds.size());
            return feeds;

        } catch (Exception e) {
            logger.error("Search failed for keyword: {}", keyword, e);
            return List.of();
        }
    }
}
