package com.qingcloud.mcp.xhs.actions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 用户资料操作
 * 获取小红书用户的个人资料和帖子
 */
public class UserProfileAction {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileAction.class);
    private static final String USER_PROFILE_URL_TEMPLATE = "https://www.xiaohongshu.com/user/profile/%s?xsec_token=%s&xsec_source=pc_note";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Page page;

    public UserProfileAction(Page page) {
        this.page = page;
    }

    /**
     * 获取用户资料
     * 
     * @param userId    用户 ID
     * @param xsecToken 访问令牌
     * @return 用户资料数据
     */
    public Map<String, Object> getUserProfile(String userId, String xsecToken) {
        try {
            logger.info("=== User Profile Action: {} ===", userId);

            // 构建完整 URL
            String profileUrl = String.format(USER_PROFILE_URL_TEMPLATE, userId, xsecToken);
            logger.info("Navigating to user profile page: {}", profileUrl);

            // 导航到用户资料页面
            page.navigate(profileUrl, new Page.NavigateOptions().setTimeout(60000));

            // 等待页面加载
            try {
                page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(30000));
            } catch (Exception e) {
                logger.warn("Page load state timeout, continuing anyway: {}", e.getMessage());
            }

            logger.info("Page loaded, waiting for __INITIAL_STATE__...");

            // 等待 __INITIAL_STATE__ 可用
            page.waitForFunction("() => window.__INITIAL_STATE__ !== undefined", null,
                    new Page.WaitForFunctionOptions().setTimeout(15000));

            logger.info("__INITIAL_STATE__ available, extracting user profile...");

            // 提取用户资料和帖子数据 - 增强版带调试信息
            Object result = page.evaluate("""
                    () => {
                        const state = window.__INITIAL_STATE__;

                        // 调试：记录 __INITIAL_STATE__ 的结构
                        const debugInfo = {
                            hasState: !!state,
                            hasUser: !!(state && state.user),
                            stateKeys: state ? Object.keys(state) : [],
                            userKeys: (state && state.user) ? Object.keys(state.user) : []
                        };

                        if (!state) {
                            return JSON.stringify({
                                debug: debugInfo,
                                error: "__INITIAL_STATE__ is undefined",
                                profile: {},
                                notes: []
                            });
                        }

                        if (!state.user) {
                            return JSON.stringify({
                                debug: debugInfo,
                                error: "state.user is undefined",
                                profile: {},
                                notes: []
                            });
                        }

                        // 1. 提取用户基本信息 - 多种可能的数据结构
                        let userData = {};
                        if (state.user.userPageData) {
                            // 尝试多种可能的数据结构
                            let data = state.user.userPageData.value ||
                                      state.user.userPageData._value ||
                                      state.user.userPageData;

                            if (data && typeof data === 'object') {
                                userData = {
                                    basicInfo: data.basicInfo || {},
                                    interactions: data.interactions || {}
                                };
                            }
                        }

                        // 2. 提取用户帖子 - 多种可能的数据结构
                        let notes = [];
                        if (state.user.notes) {
                            let data = state.user.notes.value ||
                                      state.user.notes._value ||
                                      state.user.notes;

                            if (Array.isArray(data)) {
                                // 展平双重数组（如果是嵌套数组）
                                notes = data.flat();
                            } else if (data && typeof data === 'object') {
                                // 如果是对象，尝试提取数组
                                notes = Object.values(data).filter(item =>
                                    item && typeof item === 'object'
                                );
                            }
                        }

                        return JSON.stringify({
                            debug: debugInfo,
                            profile: userData,
                            notes: notes
                        });
                    }
                    """);

            if (result == null || result.toString().isEmpty()) {
                logger.warn("No user profile data found in __INITIAL_STATE__");
                return Map.of();
            }

            logger.info("Raw result length: {}", result.toString().length());

            // 解析 JSON
            @SuppressWarnings("unchecked")
            Map<String, Object> profileData = objectMapper.readValue(result.toString(), Map.class);

            // 输出调试信息
            if (profileData.containsKey("debug")) {
                logger.info("Debug Info: {}", profileData.get("debug"));
            }
            if (profileData.containsKey("error")) {
                logger.warn("Page evaluation error: {}", profileData.get("error"));
            }

            // 检查数据是否为空
            Object profile = profileData.get("profile");
            Object notes = profileData.get("notes");

            if (profile instanceof Map && ((Map<?, ?>) profile).isEmpty()) {
                logger.warn("WARNING: Profile data is empty!");
            } else {
                logger.info("Profile data extracted: {} fields",
                        profile instanceof Map ? ((Map<?, ?>) profile).size() : 0);
            }

            if (notes instanceof java.util.List && ((java.util.List<?>) notes).isEmpty()) {
                logger.warn("WARNING: Notes array is empty!");
            } else {
                logger.info("Notes extracted: {} items",
                        notes instanceof java.util.List ? ((java.util.List<?>) notes).size() : 0);
            }

            logger.info("✓ Successfully extracted user profile for: {}", userId);

            return profileData;

        } catch (Exception e) {
            logger.error("Failed to get user profile for: {}", userId, e);
            throw new RuntimeException("Failed to get user profile: " + e.getMessage(), e);
        }
    }
}
