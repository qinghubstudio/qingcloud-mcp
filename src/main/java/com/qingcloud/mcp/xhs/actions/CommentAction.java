package com.qingcloud.mcp.xhs.actions;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 评论操作
 * 发表评论到小红书帖子
 */
public class CommentAction {

    private static final Logger logger = LoggerFactory.getLogger(CommentAction.class);
    private static final String POST_URL_TEMPLATE = "https://www.xiaohongshu.com/explore/%s?xsec_token=%s&xsec_source=pc_feed";

    private final Page page;

    public CommentAction(Page page) {
        this.page = page;
    }

    /**
     * 发表评论到帖子
     * 
     * @param noteId    帖子 ID
     * @param xsecToken 访问令牌
     * @param content   评论内容
     */
    public void postComment(String noteId, String xsecToken, String content) {
        try {
            logger.info("=== Post Comment Action: {} ===", noteId);

            // 构建完整 URL
            String postUrl = String.format(POST_URL_TEMPLATE, noteId, xsecToken);
            logger.info("Navigating to post page: {}", postUrl);

            // 导航到帖子页面
            page.navigate(postUrl, new Page.NavigateOptions().setTimeout(60000));

            // 等待页面加载
            try {
                page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(30000));
            } catch (Exception e) {
                logger.warn("Page load state timeout, continuing anyway: {}", e.getMessage());
            }

            logger.info("Page loaded, finding comment input box...");

            // 等待并点击评论输入框
            page.waitForSelector("div.input-box div.content-edit span",
                    new Page.WaitForSelectorOptions().setTimeout(10000));
            page.click("div.input-box div.content-edit span");

            logger.info("Clicked comment input box, entering content...");

            // 调试日志: 检查 content 字符
            if (content != null) {
                StringBuilder sb = new StringBuilder();
                for (char c : content.toCharArray()) {
                    sb.append(String.format("\\u%04x", (int) c));
                }
                logger.info("Comment content (hex): {}", sb.toString());
            }

            // 等待输入区域出现
            page.waitForSelector("div.input-box div.content-edit p.content-input",
                    new Page.WaitForSelectorOptions().setTimeout(5000));

            // 使用 evaluate 输入中文并触发事件，这是最稳妥的方法
            page.evaluate("(args) => { " +
                    "  const el = document.querySelector('div.input-box div.content-edit p.content-input');" +
                    "  if (el) {" +
                    "    el.textContent = args.text;" +
                    "    // 触发 React/Vue 所需的事件\n" +
                    "    el.dispatchEvent(new Event('input', { bubbles: true }));" +
                    "    el.dispatchEvent(new Event('change', { bubbles: true }));" +
                    "    el.dispatchEvent(new Event('blur', { bubbles: true }));" +
                    "  }" +
                    "}", new java.util.HashMap<String, Object>() {
                        {
                            put("text", content);
                        }
                    });

            logger.info("Content entered via evaluate, waiting before submit...");
            page.waitForTimeout(1000);

            // 等待提交按钮可点击（未禁用）
            page.waitForSelector("div.bottom button.submit:not([disabled])",
                    new Page.WaitForSelectorOptions().setTimeout(15000));

            // 点击提交按钮
            page.click("div.bottom button.submit");

            logger.info("Submit button clicked, waiting for completion...");
            page.waitForTimeout(2000);

            logger.info("✓ Comment posted successfully to note: {}", noteId);

        } catch (Exception e) {
            logger.error("Failed to post comment to note: {}", noteId, e);
            throw new RuntimeException("Failed to post comment: " + e.getMessage(), e);
        }
    }
}
