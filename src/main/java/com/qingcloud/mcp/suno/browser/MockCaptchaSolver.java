package com.qingcloud.mcp.suno.browser;

import com.qingcloud.mcp.suno.dto.CaptchaSolution;
import com.qingcloud.mcp.suno.dto.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mock CAPTCHA 解决器实现
 * 返回随机坐标用于测试
 * 仅用于开发环境,不应在生产环境使用
 * 
 * @author qingcloud-mcp
 */
@Service
@ConditionalOnProperty(name = "suno.captcha.solver", havingValue = "mock")
public class MockCaptchaSolver implements ICaptchaSolver {

    private static final Logger logger = LoggerFactory.getLogger(MockCaptchaSolver.class);
    private final Random random = new Random();

    public MockCaptchaSolver() {
        logger.warn("Initialized Mock CAPTCHA solver - FOR TESTING ONLY!");
        logger.warn("Mock solver returns random coordinates and will likely fail real CAPTCHAs");
    }

    @Override
    public CaptchaSolution solve(byte[] screenshot, String instructions) {
        logger.info("Mock solving CAPTCHA (screenshot: {} bytes)", screenshot.length);

        // 生成 3-5 个随机坐标点
        int numPoints = 3 + random.nextInt(3);
        List<Coordinate> coordinates = new ArrayList<>();

        for (int i = 0; i < numPoints; i++) {
            int x = 50 + random.nextInt(350); // 假设 CAPTCHA 宽度 ~400px
            int y = 50 + random.nextInt(250); // 假设 CAPTCHA 高度 ~300px
            coordinates.add(new Coordinate(x, y));
        }

        logger.info("Generated {} random coordinates", numPoints);

        return new CaptchaSolution("mock-" + System.currentTimeMillis(), coordinates);
    }

    @Override
    public void reportBad(String taskId) {
        logger.info("Mock solver ignoring reportBad (taskId: {})", taskId);
    }

    @Override
    public String getName() {
        return "Mock";
    }
}
