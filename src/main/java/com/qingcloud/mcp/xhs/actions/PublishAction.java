package com.qingcloud.mcp.xhs.actions;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.qingcloud.mcp.xhs.model.PublishImageContent;
import com.qingcloud.mcp.xhs.model.PublishVideoContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 发布操作
 * 负责处理小红书内容发布相关的浏览器操作
 */
public class PublishAction {

    private static final Logger logger = LoggerFactory.getLogger(PublishAction.class);
    private static final String PUBLISH_URL = "https://creator.xiaohongshu.com/publish/publish?source=official";

    private final Page page;

    public PublishAction(Page page) {
        this.page = page;
    }

    /**
     * 发布图文内容
     */
    public void publishImage(PublishImageContent content) throws Exception {
        if (content.getImagePaths() == null || content.getImagePaths().isEmpty()) {
            throw new IllegalArgumentException("图片不能为空");
        }

        logger.info("开始发布图文内容: title={}, images={}, tags={}",
                content.getTitle(), content.getImagePaths().size(), content.getTags());

        // 导航到发布页面
        navigateToPublishPage();

        // 点击"上传图文"标签
        clickPublishTab("上传图文");
        page.waitForTimeout(1000);

        // 上传图片
        uploadImages(content.getImagePaths());

        // 填写标题和内容
        fillTitleAndContent(content.getTitle(), content.getContent());

        // 输入标签
        if (content.getTags() != null && !content.getTags().isEmpty()) {
            inputTags(content.getTags());
        }

        // 提交发布
        submitPublish();

        logger.info("✓ 图文内容发布成功");
    }

    /**
     * 发布视频内容
     */
    public void publishVideo(PublishVideoContent content) throws Exception {
        if (content.getVideoPath() == null || content.getVideoPath().isEmpty()) {
            throw new IllegalArgumentException("视频不能为空");
        }

        logger.info("开始发布视频内容: title={}, video={}, tags={}",
                content.getTitle(), content.getVideoPath(), content.getTags());

        // 导航到发布页面
        navigateToPublishPage();

        // 点击"上传视频"标签
        clickPublishTab("上传视频");
        page.waitForTimeout(1000);

        // 上传视频
        uploadVideo(content.getVideoPath());

        // 填写标题和内容
        fillTitleAndContent(content.getTitle(), content.getContent());

        // 输入标签
        if (content.getTags() != null && !content.getTags().isEmpty()) {
            inputTags(content.getTags());
        }

        // 等待视频处理完成并提交
        waitForPublishButtonClickable();
        submitPublish();

        logger.info("✓ 视频内容发布成功");
    }

    /**
     * 导航到发布页面
     */
    private void navigateToPublishPage() {
        logger.info("导航到发布页面: {}", PUBLISH_URL);
        page.navigate(PUBLISH_URL, new Page.NavigateOptions().setTimeout(60000));
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.waitForTimeout(1000);
    }

    /**
     * 点击发布标签（上传图文/上传视频）
     */
    private void clickPublishTab(String tabName) throws Exception {
        logger.info("点击发布标签: {}", tabName);

        page.locator("div.upload-content").waitFor();

        long deadline = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < deadline) {
            Locator tabs = page.locator("div.creator-tab");
            int count = tabs.count();

            for (int i = 0; i < count; i++) {
                Locator tab = tabs.nth(i);
                if (!tab.isVisible()) {
                    continue;
                }

                String text = tab.textContent().trim();
                if (text.equals(tabName)) {
                    // 检查是否被遮挡
                    if (isElementBlocked(tab)) {
                        logger.info("标签被遮挡，尝试移除遮挡");
                        removePopCover();
                        Thread.sleep(200);
                        continue;
                    }

                    tab.click();
                    logger.info("✓ 成功点击标签: {}", tabName);
                    return;
                }
            }

            Thread.sleep(200);
        }

        throw new Exception("未找到发布标签: " + tabName);
    }

    /**
     * 检查元素是否被遮挡
     */
    private boolean isElementBlocked(Locator element) {
        try {
            Object result = element.evaluate("el => {" +
                    "const rect = el.getBoundingClientRect();" +
                    "if (rect.width === 0 || rect.height === 0) return true;" +
                    "const x = rect.left + rect.width / 2;" +
                    "const y = rect.top + rect.height / 2;" +
                    "const target = document.elementFromPoint(x, y);" +
                    "return !(target === el || el.contains(target));" +
                    "}");
            return (Boolean) result;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 移除弹窗遮挡
     */
    private void removePopCover() {
        try {
            Locator popover = page.locator("div.d-popover");
            if (popover.count() > 0) {
                popover.first().evaluate("el => el.remove()");
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 上传图片
     */
    private void uploadImages(List<String> imagePaths) throws Exception {
        logger.info("上传 {} 张图片", imagePaths.size());

        // 验证文件存在
        for (String path : imagePaths) {
            if (!Files.exists(Path.of(path))) {
                throw new Exception("图片文件不存在: " + path);
            }
        }

        // 查找上传输入框
        Locator uploadInput = page.locator(".upload-input");
        uploadInput.waitFor();

        // 上传文件
        Path[] pathArray = imagePaths.stream()
                .map(Path::of)
                .toArray(Path[]::new);
        uploadInput.setInputFiles(pathArray);
        // 等待上传完成
        waitForUploadComplete(imagePaths.size());
    }

    /**
     * 等待图片上传完成
     */
    private void waitForUploadComplete(int expectedCount) throws Exception {
        logger.info("等待 {} 张图片上传完成", expectedCount);

        long deadline = System.currentTimeMillis() + 60000;
        while (System.currentTimeMillis() < deadline) {
            Locator uploadedImages = page.locator(".img-preview-area .pr");
            int currentCount = uploadedImages.count();

            if (currentCount >= expectedCount) {
                logger.info("✓ 所有图片上传完成: {}", currentCount);
                return;
            }

            Thread.sleep(500);
        }

        throw new Exception("图片上传超时");
    }

    /**
     * 上传视频
     */
    private void uploadVideo(String videoPath) throws Exception {
        logger.info("上传视频: {}", videoPath);

        // 验证文件存在
        if (!Files.exists(Path.of(videoPath))) {
            throw new Exception("视频文件不存在: " + videoPath);
        }

        // 查找上传输入框
        Locator uploadInput = page.locator(".upload-input");
        if (uploadInput.count() == 0) {
            uploadInput = page.locator("input[type='file']");
        }
        uploadInput.waitFor();

        // 上传文件
        uploadInput.setInputFiles(new Path[] { Path.of(videoPath) });

        logger.info("视频文件已选择，等待处理...");
    }

    /**
     * 填写标题和内容
     */
    private void fillTitleAndContent(String title, String content) throws Exception {
        logger.info("填写标题和内容");

        // 填写标题
        Locator titleInput = page.locator("div.d-input input");
        titleInput.waitFor();
        titleInput.fill(title);

        page.waitForTimeout(500);

        // 检查标题长度
        checkTitleMaxLength();

        page.waitForTimeout(1000);

        // 填写内容
        Locator contentElem = getContentElement();
        if (contentElem == null) {
            throw new Exception("未找到内容输入框");
        }

        contentElem.fill(content);
        page.waitForTimeout(1000);

        // 检查内容长度
        checkContentMaxLength();
    }

    /**
     * 获取内容输入框
     */
    private Locator getContentElement() {
        // 尝试方法1: div.ql-editor
        Locator qlEditor = page.locator("div.ql-editor");
        if (qlEditor.count() > 0) {
            return qlEditor.first();
        }

        // 尝试方法2: 通过 placeholder 查找
        Locator paragraphs = page.locator("p[data-placeholder*='输入正文描述']");
        if (paragraphs.count() > 0) {
            // 向上查找 textbox 父元素
            Locator textbox = paragraphs.first().locator("xpath=ancestor::*[@role='textbox']");
            if (textbox.count() > 0) {
                return textbox.first();
            }
        }

        return null;
    }

    /**
     * 检查标题长度
     */
    private void checkTitleMaxLength() throws Exception {
        Locator maxSuffix = page.locator("div.title-container div.max_suffix");
        if (maxSuffix.count() > 0) {
            String lengthText = maxSuffix.textContent();
            throw new Exception("标题长度超过限制: " + lengthText);
        }
    }

    /**
     * 检查内容长度
     */
    private void checkContentMaxLength() throws Exception {
        Locator lengthError = page.locator("div.edit-container div.length-error");
        if (lengthError.count() > 0) {
            String lengthText = lengthError.textContent();
            throw new Exception("内容长度超过限制: " + lengthText);
        }
    }

    /**
     * 输入标签
     */
    private void inputTags(List<String> tags) throws Exception {
        if (tags.isEmpty()) {
            return;
        }

        // 限制标签数量
        List<String> limitedTags = tags.size() > 10 ? tags.subList(0, 10) : tags;
        logger.info("输入 {} 个标签", limitedTags.size());

        Locator contentElem = getContentElement();
        if (contentElem == null) {
            logger.warn("未找到内容输入框，跳过标签输入");
            return;
        }

        page.waitForTimeout(1000);

        // 移动到内容末尾
        for (int i = 0; i < 20; i++) {
            contentElem.press("ArrowDown");
            page.waitForTimeout(10);
        }

        contentElem.press("Enter");
        contentElem.press("Enter");
        page.waitForTimeout(1000);

        // 输入每个标签
        for (String tag : limitedTags) {
            inputSingleTag(contentElem, tag);
        }
    }

    /**
     * 输入单个标签
     */
    private void inputSingleTag(Locator contentElem, String tag) throws Exception {
        // 移除开头的 #
        tag = tag.replaceFirst("^#", "");

        contentElem.pressSequentially("#");
        page.waitForTimeout(200);

        // 逐字输入标签
        for (char c : tag.toCharArray()) {
            contentElem.pressSequentially(String.valueOf(c));
            page.waitForTimeout(50);
        }

        page.waitForTimeout(1000);

        // 尝试点击联想选项
        Locator topicContainer = page.locator("#creator-editor-topic-container");
        if (topicContainer.count() > 0) {
            Locator firstItem = topicContainer.locator(".item").first();
            if (firstItem.count() > 0) {
                firstItem.click();
                logger.info("✓ 成功选择标签联想: {}", tag);
                page.waitForTimeout(200);
                return;
            }
        }

        // 没有联想选项，输入空格结束
        contentElem.pressSequentially(" ");
        page.waitForTimeout(500);
    }

    /**
     * 等待发布按钮可点击（视频发布时使用）
     */
    private void waitForPublishButtonClickable() throws Exception {
        logger.info("等待发布按钮可点击（视频处理中）...");

        long deadline = System.currentTimeMillis() + 600000; // 10分钟
        while (System.currentTimeMillis() < deadline) {
            Locator btn = page.locator("button.publishBtn");
            if (btn.count() > 0 && btn.isVisible()) {
                String disabled = btn.getAttribute("disabled");
                if (disabled == null) {
                    logger.info("✓ 发布按钮已可点击");
                    return;
                }
            }
            Thread.sleep(1000);
        }

        throw new Exception("等待发布按钮可点击超时");
    }

    /**
     * 提交发布
     */
    private void submitPublish() throws Exception {
        logger.info("提交发布");

        // 使用 first() 来选择第一个按钮（发布按钮），避免匹配到"暂存离开"按钮
        Locator submitButton = page.locator("div.submit div.d-button-content").first();
        submitButton.waitFor();
        submitButton.click();

        page.waitForTimeout(3000);
        logger.info("✓ 发布提交完成");
    }
}
