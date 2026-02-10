package com.qingcloud.mcp.suno.browser;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.qingcloud.mcp.suno.dto.CaptchaSolution;
import com.qingcloud.mcp.suno.dto.Coordinate;
import com.qingcloud.mcp.suno.exception.SunoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * CAPTCHA Token 提取器
 * 负责完整的 CAPTCHA 处理流程并提取 Token
 * 
 * @author qingcloud-mcp
 */
@Service
public class CaptchaTokenExtractor {

    private static final Logger logger = LoggerFactory.getLogger(CaptchaTokenExtractor.class);
    private static final String CREATE_PAGE_URL = "https://suno.com/create";
    private static final int TIMEOUT_PAGE_LOAD = 30000;
    private static final int TIMEOUT_TEXTAREA_WAIT = 5000;
    private static final int TIMEOUT_BUTTON_WAIT = 5000;
    private static final int CAPTCHA_IMAGE_LOAD_DELAY = 2000;
    private static final int CAPTCHA_PIECE_UNLOCK_DELAY = 300;

    private final SunoBrowserManager browserManager;
    private final ICaptchaSolver captchaSolver;

    public CaptchaTokenExtractor(SunoBrowserManager browserManager, ICaptchaSolver captchaSolver) {
        this.browserManager = browserManager;
        this.captchaSolver = captchaSolver;
        logger.info("Initialized CaptchaTokenExtractor with {} solver", captchaSolver.getName());
    }

    /**
     * 提取 CAPTCHA Token
     * 
     * @param prompt 测试提示词
     * @return CAPTCHA Token
     */
    public String extractToken(String prompt) {
        BrowserContext context = null;
        Page page = null;

        try {
            logger.info("Starting CAPTCHA token extraction...");

            // 创建 Context
            context = browserManager.newContext();
            page = context.newPage();

            // 设置 Token 拦截器
            AtomicReference<String> tokenRef = new AtomicReference<>();
            CompletableFuture<String> tokenFuture = new CompletableFuture<>();

            page.route("**/api/generate/v2/**", route -> {
                try {
                    String requestBody = route.request().postData();
                    if (requestBody != null && requestBody.contains("token")) {
                        // 提取 token (简化版,实际需要 JSON 解析)
                        int tokenIndex = requestBody.indexOf("\"token\":");
                        if (tokenIndex != -1) {
                            int start = requestBody.indexOf("\"", tokenIndex + 8) + 1;
                            int end = requestBody.indexOf("\"", start);
                            if (start > 0 && end > start) {
                                String token = requestBody.substring(start, end);
                                if (token != null && !token.equals("null")) {
                                    logger.info("Token extracted: {}...",
                                            token.substring(0, Math.min(20, token.length())));
                                    tokenRef.set(token);
                                    tokenFuture.complete(token);
                                }
                            }
                        }
                    }
                    route.abort();
                } catch (Exception e) {
                    logger.error("Error in route handler", e);
                    route.abort();
                }
            });

            // 导航到页面
            logger.info("Navigating to Suno create page...");
            page.navigate(CREATE_PAGE_URL, new Page.NavigateOptions().setTimeout(TIMEOUT_PAGE_LOAD));
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // 查找并填写 textarea
            logger.info("Looking for textarea...");
            Locator textarea = findTextarea(page);
            textarea.fill(prompt != null ? prompt : "Test prompt");
            logger.info("Textarea filled");

            // 查找并点击 Create 按钮
            logger.info("Looking for Create button...");
            Locator button = page.locator("button[aria-label=\"Create song\"]");
            button.waitFor(new Locator.WaitForOptions().setTimeout(TIMEOUT_BUTTON_WAIT));
            button.click();
            logger.info("Create button clicked");

            // 处理 CAPTCHA
            solveCaptchaLoop(page, button);

            // 等待 Token
            String token = tokenFuture.get(60, TimeUnit.SECONDS);
            logger.info("Token extraction completed successfully");

            return token;

        } catch (Exception e) {
            logger.error("Failed to extract CAPTCHA token", e);
            throw new SunoException("CAPTCHA token extraction failed: " + e.getMessage(), e);
        } finally {
            if (page != null) {
                try {
                    page.close();
                } catch (Exception e) {
                    logger.warn("Failed to close page", e);
                }
            }
            if (context != null) {
                try {
                    context.close();
                } catch (Exception e) {
                    logger.warn("Failed to close context", e);
                }
            }
        }
    }

    /**
     * 查找 textarea
     */
    private Locator findTextarea(Page page) {
        try {
            // 尝试原始选择器
            Locator textarea = page.locator("textarea[placeholder*=\"Hip-hop\"]");
            textarea.waitFor(new Locator.WaitForOptions()
                    .setTimeout(TIMEOUT_TEXTAREA_WAIT));
            return textarea;
        } catch (Exception e) {
            logger.info("Hip-hop placeholder not found, trying alternative...");
            // 尝试查找任何可见的 textarea
            Locator textareas = page.locator("textarea");
            int count = textareas.count();
            for (int i = 0; i < count; i++) {
                Locator ta = textareas.nth(i);
                if (ta.isVisible()) {
                    logger.info("Using textarea at index {}", i);
                    return ta;
                }
            }
            throw new SunoException("Could not find any visible textarea");
        }
    }

    /**
     * CAPTCHA 解决循环
     */
    private void solveCaptchaLoop(Page page, Locator button) {
        try {
            FrameLocator frame = page.frameLocator("iframe[title*=\"hCaptcha\"]");
            Locator challenge = frame.locator(".challenge-container");

            boolean shouldWaitForImages = true;
            int maxAttempts = 10;
            int attempt = 0;

            while (attempt < maxAttempts) {
                attempt++;
                logger.info("CAPTCHA solving attempt {}/{}", attempt, maxAttempts);

                // 等待图片加载
                if (shouldWaitForImages) {
                    Thread.sleep(CAPTCHA_IMAGE_LOAD_DELAY);
                }

                // 判断 CAPTCHA 类型
                String promptText = challenge.locator(".prompt-text").first().innerText();
                boolean isDragType = promptText.toLowerCase().contains("drag");
                logger.info("CAPTCHA type: {}", isDragType ? "DRAG" : "CLICK");

                // 截图并求解
                byte[] screenshot = challenge.screenshot();
                String instructions = isDragType
                        ? "CLICK on the shapes at their edge or center as shown above—please be precise!"
                        : null;

                CaptchaSolution solution = captchaSolver.solve(screenshot, instructions);

                // 验证拖动类型的解决方案
                if (isDragType && solution.getData().size() % 2 != 0) {
                    logger.warn("Drag solution has odd number of points, requesting new solution");
                    captchaSolver.reportBad(solution.getId());
                    shouldWaitForImages = false;
                    continue;
                }

                // 执行操作
                if (isDragType) {
                    performDrags(page, challenge, solution.getData());
                } else {
                    performClicks(challenge, solution.getData());
                }

                // 提交
                submitSolution(frame, button);
                shouldWaitForImages = true;

                // 等待一下看是否需要继续
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            // CAPTCHA 窗口关闭或其他预期的终止条件
            if (e.getMessage() != null &&
                    (e.getMessage().contains("closed") || e.getMessage().contains("detached"))) {
                logger.info("CAPTCHA window closed, assuming solved");
                return;
            }
            throw new SunoException("CAPTCHA solving failed", e);
        }
    }

    /**
     * 执行点击操作
     */
    private void performClicks(Locator challenge, List<Coordinate> coordinates) {
        logger.info("Performing {} clicks", coordinates.size());
        for (Coordinate coord : coordinates) {
            logger.debug("Clicking at {}", coord);
            challenge.click(new Locator.ClickOptions()
                    .setPosition(coord.getX(), coord.getY()));
        }
    }

    /**
     * 执行拖动操作
     */
    private void performDrags(Page page, Locator challenge, List<Coordinate> coordinates) {
        try {
            var box = challenge.boundingBox();
            if (box == null) {
                throw new SunoException("Challenge bounding box is null");
            }

            logger.info("Performing {} drag operations", coordinates.size() / 2);

            for (int i = 0; i < coordinates.size(); i += 2) {
                Coordinate start = coordinates.get(i);
                Coordinate end = coordinates.get(i + 1);

                logger.debug("Dragging from {} to {}", start, end);

                // 移动到起点
                page.mouse().move(box.x + start.getX(), box.y + start.getY());
                page.mouse().down();

                // 等待解锁
                Thread.sleep(CAPTCHA_PIECE_UNLOCK_DELAY);

                // 拖动到终点 (30 步平滑移动)
                page.mouse().move(box.x + end.getX(), box.y + end.getY(),
                        new Mouse.MoveOptions().setSteps(30));
                page.mouse().up();
            }

        } catch (Exception e) {
            throw new SunoException("Failed to perform drag operations", e);
        }
    }

    /**
     * 提交 CAPTCHA 解决方案
     */
    private void submitSolution(FrameLocator frame, Locator button) {
        try {
            frame.locator(".button-submit").click();
        } catch (Exception e) {
            // 如果 CAPTCHA 窗口因不活动而关闭,重新触发
            if (e.getMessage() != null && e.getMessage().contains("viewport")) {
                logger.info("CAPTCHA window closed, retriggering...");
                button.click();
            } else {
                throw e;
            }
        }
    }
}
