package com.qingcloud.mcp.xhs.actions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 帖子详情操作
 * 获取帖子的完整信息
 */
public class PostDetailAction {

    private static final Logger logger = LoggerFactory.getLogger(PostDetailAction.class);
    private static final String POST_URL_TEMPLATE = "https://www.xiaohongshu.com/explore/%s";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Page page;

    public PostDetailAction(Page page) {
        this.page = page;
    }

    /**
     * 获取帖子详情
     * 
     * @param noteId    帖子 ID
     * @param xsecToken 访问令牌 (从 Feed 列表的 xsecToken 字段获取)
     * @return 帖子详情数据
     */
    public Map<String, Object> getPostDetail(String noteId, String xsecToken) {
        try {
            logger.info("=== Post Detail Action: {} ===", noteId);

            // 构建完整 URL (包含 xsec_token 和 xsec_source)
            String postUrl = String.format(POST_URL_TEMPLATE + "?xsec_token=%s&xsec_source=pc_feed",
                    noteId, xsecToken);
            logger.info("Navigating to post page: {}", postUrl);

            // 导航到帖子页面
            page.navigate(postUrl, new Page.NavigateOptions().setTimeout(60000));

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

            logger.info("__INITIAL_STATE__ available, extracting post detail...");

            // 检查可能的路径 (不序列化整个对象,避免循环引用)
            Object pathCheck = page.evaluate("""
                    () => {
                        const state = window.__INITIAL_STATE__;
                        const paths = {
                            hasNote: !!state.note,
                            hasNoteDetailMap: !!(state.note && state.note.noteDetailMap),
                            hasNoteDetail: !!(state.note && state.note.noteDetail),
                            noteKeys: state.note ? Object.keys(state.note) : [],
                            topLevelKeys: Object.keys(state)
                        };
                        return JSON.stringify(paths);
                    }
                    """);
            logger.info("Path check result: {}", pathCheck);

            // 从 window.__INITIAL_STATE__ 提取帖子详情 (只提取纯数据,避免循环引用)
            Object result = page.evaluate("""
                    () => {
                        const state = window.__INITIAL_STATE__;

                        // 尝试多个可能的路径
                        let noteData = null;

                        // 路径 1: note.noteDetailMap
                        if (state.note && state.note.noteDetailMap) {
                            const noteDetailMap = state.note.noteDetailMap;
                            const firstKey = Object.keys(noteDetailMap)[0];
                            if (firstKey && noteDetailMap[firstKey]) {
                                const noteDetail = noteDetailMap[firstKey];
                                noteData = noteDetail.note || noteDetail;
                            }
                        }

                        // 路径 2: note.noteDetail
                        if (!noteData && state.note && state.note.noteDetail) {
                            noteData = state.note.noteDetail;
                        }

                        // 路径 3: note.currentNote
                        if (!noteData && state.note && state.note.currentNote) {
                            noteData = state.note.currentNote;
                        }

                        if (!noteData) {
                            return JSON.stringify({debug: "noteData is null or undefined"});
                        }

                        // 尝试使用 toRaw 解包 Vue 响应式对象
                        let rawData = noteData;
                        if (window.Vue && window.Vue.toRaw) {
                            try {
                                rawData = window.Vue.toRaw(noteData);
                            } catch (e) {
                                // toRaw 失败,继续使用原始对象
                            }
                        }

                        // 获取所有键
                        const keys = Object.keys(rawData);
                        const debug = {
                            keysCount: keys.length,
                            keys: keys.slice(0, 20), // 只取前20个键
                            hasToRaw: !!(window.Vue && window.Vue.toRaw)
                        };

                        // 如果没有键,返回调试信息
                        if (keys.length === 0) {
                            return JSON.stringify({debug: debug});
                        }

                        // 提取所有可序列化的字段
                        const cleanData = {};
                        for (const key of keys) {
                            try {
                                const value = rawData[key];
                                // 跳过函数
                                if (typeof value === 'function') continue;
                                // 尝试序列化
                                JSON.stringify(value);
                                cleanData[key] = value;
                            } catch (e) {
                                // 跳过无法序列化的字段
                            }
                        }

                        return JSON.stringify(cleanData);
                    }
                    """);

            if (result == null || result.toString().isEmpty()) {
                logger.warn("No post detail found in __INITIAL_STATE__");
                logger.warn("Dumping __INITIAL_STATE__ structure...");
                Object stateDump = page.evaluate("() => JSON.stringify(Object.keys(window.__INITIAL_STATE__ || {}))");
                logger.warn("__INITIAL_STATE__ keys: {}", stateDump);
                return Map.of();
            }

            logger.info("Raw result length: {}", result.toString().length());

            // 解析 JSON
            @SuppressWarnings("unchecked")
            Map<String, Object> postDetail = objectMapper.readValue(
                    result.toString(),
                    Map.class);

            logger.info("✓ Successfully extracted post detail for: {}", noteId);
            return postDetail;

        } catch (Exception e) {
            logger.error("Failed to get post detail for: {}", noteId, e);
            return Map.of();
        }
    }
}
