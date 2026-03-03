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
                            if (feedsData && Array.isArray(feedsData)) {
                                // 提取并增强 noteId (防止为 "")
                                return JSON.stringify(feedsData.map(item => {
                                    // 尝试多个可能的 ID 字段
                                    const nid = item.noteId || item.id || item.id_ || item.nid || "";
                                    return {
                                        ...item,
                                        noteId: nid,
                                        // 确保 userId 也不为空
                                        userId: item.userId || (item.user ? item.user.userId : "")
                                    };
                                }));
                            }
                        }
                        return "";
                    }
                    """);

            if (result == null || result.toString().isEmpty() || result.toString().equals("null")) {
                logger.warn("No search results found in __INITIAL_STATE__");

                // Detailed debug of structure
                Object debugInfo = page.evaluate("""
                            () => {
                                const info = {};
                                if (!window.__INITIAL_STATE__) {
                                    info.hasState = false;
                                } else {
                                    info.hasState = true;
                                    info.keys = Object.keys(window.__INITIAL_STATE__);
                                    if (window.__INITIAL_STATE__.search) {
                                        info.hasSearch = true;
                                        info.searchKeys = Object.keys(window.__INITIAL_STATE__.search);
                                        if (window.__INITIAL_STATE__.search.feeds) {
                                            info.hasFeeds = true;
                                            info.feedsType = typeof window.__INITIAL_STATE__.search.feeds;
                                            info.feedsKeys = Object.keys(window.__INITIAL_STATE__.search.feeds);
                                        }
                                    }
                                    // Also check for 'note' or other potential keys
                                    if (window.__INITIAL_STATE__.note) {
                                        info.hasNote = true;
                                    }
                                }
                                return JSON.stringify(info);
                            }
                        """);
                logger.warn("Debug Info: {}", debugInfo);

                // Dump HTML for analysis (truncated)
                String html = page.content();
                logger.warn("Page Content Preview: {}", html.substring(0, Math.min(1000, html.length())));

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

            logger.info("✓ Successfully extracted {} search results from __INITIAL_STATE__", feeds.size());

            // 检查是否有空的 noteId
            boolean hasEmptyIds = feeds.stream().anyMatch(f -> {
                Object nid = f.get("noteId");
                return nid == null || nid.toString().isEmpty();
            });

            if (hasEmptyIds) {
                logger.warn("Extracted feeds have empty noteIds. Will attempt DOM scraping as fallback.");
            }

            // If feeds is empty or has empty IDs, try DOM scraping
            if (feeds.isEmpty() || hasEmptyIds) {
                logger.info("Feeds empty, attempting DOM scraping...");

                // Wait for elements to appear
                try {
                    page.waitForSelector("section.note-item", new Page.WaitForSelectorOptions().setTimeout(5000));
                } catch (Exception e) {
                    logger.warn("Timeout waiting for .note-item selectors");
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> domResults = (List<Map<String, Object>>) page.evaluate(
                        """
                                    () => {
                                        const items = [];
                                        document.querySelectorAll('section.note-item').forEach(el => {
                                            try {
                                                const titleEl = el.querySelector('.title span');
                                                const authorEl = el.querySelector('.author .name');
                                                const likeEl = el.querySelector('.like-wrapper .count');
                                                const imgEl = el.querySelector('img');
                                                const aEl = el.querySelector('a.cover');
                                                // Try to find user link (usually the author wrapper or link inside)
                                                // .author might be a div with text or an 'a' tag.
                                                // In recent XHS, likely .author is a link or contains one.
                                                // Let's assume .author is the container.
                                                const authorLink = el.querySelector('a.author') || el.querySelector('.author');

                                                if (titleEl) {
                                                    const title = titleEl.innerText;
                                                    const nickname = authorEl ? authorEl.innerText : 'Unknown';
                                                    const likes = likeEl ? likeEl.innerText : '0';
                                                    const cover = imgEl ? { url: imgEl.src } : {};

                                                    let noteId = '';
                                                    let xsecToken = '';
                                                    if (aEl && aEl.href) {
                                                        // Extract noteId from path
                                                        const matchId = aEl.href.match(new RegExp("/explore/([a-zA-Z0-9]+)"));
                                                        if (matchId) noteId = matchId[1];

                                                        // Extract xsec_token from query
                                                        try {
                                                            const urlObj = new URL(aEl.href);
                                                            xsecToken = urlObj.searchParams.get('xsec_token') || '';
                                                        } catch (e) {}
                                                    }

                                                    let userId = '';
                                                    if (authorLink && authorLink.href) {
                                                        const matchUid = authorLink.href.match(new RegExp("/user/profile/([a-zA-Z0-9]+)"));
                                                        if (matchUid) userId = matchUid[1];
                                                    }

                                                    items.push({
                                                        noteId: noteId,
                                                        title: title,
                                                        xsecToken: xsecToken,
                                                        userId: userId,
                                                        user: {
                                                            nickname: nickname,
                                                            userId: userId
                                                        },
                                                        likes: likes,
                                                        cover: cover,
                                                        type: 'video'
                                                    });
                                                }
                                            } catch (err) {
                                                // Ignore error for single item
                                            }
                                        });
                                        return items;
                                    }
                                """);

                if (domResults != null && !domResults.isEmpty()) {
                    logger.info("✓ DOM scraping found {} results", domResults.size());
                    return domResults;
                }
            }

            return feeds;

        } catch (Exception e) {
            logger.error("Search failed for keyword: {}", keyword, e);
            return List.of();
        }
    }
}
