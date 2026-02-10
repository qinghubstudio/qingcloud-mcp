package com.qingcloud.mcp.suno.browser;

import com.qingcloud.mcp.suno.dto.CaptchaSolution;
import com.qingcloud.mcp.suno.dto.Coordinate;
import com.qingcloud.mcp.suno.exception.SunoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 手动 CAPTCHA 解决器实现
 * 在控制台提示用户手动输入坐标
 * 适合开发和测试环境
 * 
 * @author qingcloud-mcp
 */
@Service
@ConditionalOnProperty(name = "suno.captcha.solver", havingValue = "manual")
public class ManualCaptchaSolver implements ICaptchaSolver {

    private static final Logger logger = LoggerFactory.getLogger(ManualCaptchaSolver.class);

    public ManualCaptchaSolver() {
        logger.info("Initialized Manual CAPTCHA solver");
        logger.warn("Manual solver requires human interaction - not suitable for production!");
    }

    @Override
    public CaptchaSolution solve(byte[] screenshot, String instructions) {
        logger.info("=== MANUAL CAPTCHA SOLVING REQUIRED ===");
        logger.info("Screenshot size: {} bytes", screenshot.length);
        if (instructions != null) {
            logger.info("Instructions: {}", instructions);
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("CAPTCHA DETECTED - Manual Input Required");
        System.out.println("=".repeat(60));
        System.out.println("Please solve the CAPTCHA in the browser window.");
        if (instructions != null) {
            System.out.println("Instructions: " + instructions);
        }
        System.out.println("\nEnter coordinates in format: x1,y1;x2,y2;x3,y3");
        System.out.println("For drag type: x1,y1;x2,y2 (pairs of start and end points)");
        System.out.println("Or press ENTER to skip and let browser handle it");
        System.out.println("=".repeat(60));
        System.out.print("Coordinates: ");

        try {
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                logger.info("User chose to skip manual input");
                // 返回空坐标列表,让浏览器自然处理
                return new CaptchaSolution("manual-skip", new ArrayList<>());
            }

            List<Coordinate> coordinates = parseInput(input);
            logger.info("User provided {} coordinates", coordinates.size());

            return new CaptchaSolution("manual-" + System.currentTimeMillis(), coordinates);

        } catch (Exception e) {
            logger.error("Failed to read manual input", e);
            throw new SunoException("Manual CAPTCHA solving failed", e);
        }
    }

    @Override
    public void reportBad(String taskId) {
        logger.info("Manual solver does not support reportBad (taskId: {})", taskId);
    }

    @Override
    public String getName() {
        return "Manual";
    }

    /**
     * 解析用户输入的坐标
     * 格式: x1,y1;x2,y2;x3,y3
     */
    private List<Coordinate> parseInput(String input) {
        List<Coordinate> coordinates = new ArrayList<>();

        try {
            String[] points = input.split(";");
            for (String point : points) {
                String[] parts = point.trim().split(",");
                if (parts.length == 2) {
                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());
                    coordinates.add(new Coordinate(x, y));
                }
            }
        } catch (Exception e) {
            throw new SunoException("Invalid coordinate format: " + input, e);
        }

        return coordinates;
    }
}
