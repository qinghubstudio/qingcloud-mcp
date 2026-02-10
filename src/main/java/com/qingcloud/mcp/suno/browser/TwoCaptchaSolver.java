package com.qingcloud.mcp.suno.browser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingcloud.mcp.suno.config.SunoProperties;
import com.qingcloud.mcp.suno.dto.CaptchaSolution;
import com.qingcloud.mcp.suno.dto.Coordinate;
import com.qingcloud.mcp.suno.exception.SunoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 2Captcha CAPTCHA 解决器实现
 * 使用 2Captcha API 解决坐标类型的 CAPTCHA
 * 
 * @author qingcloud-mcp
 */
@Service
@ConditionalOnProperty(name = "suno.captcha.solver", havingValue = "2captcha", matchIfMissing = true)
public class TwoCaptchaSolver implements ICaptchaSolver {

    private static final Logger logger = LoggerFactory.getLogger(TwoCaptchaSolver.class);
    private static final String API_BASE_URL = "https://2captcha.com";

    private final WebClient webClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public TwoCaptchaSolver(SunoProperties sunoProperties, ObjectMapper objectMapper) {
        this.apiKey = sunoProperties.getCaptcha().getKey();
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(API_BASE_URL)
                .build();

        logger.info("Initialized 2Captcha solver");
    }

    @Override
    public CaptchaSolution solve(byte[] screenshot, String instructions) {
        try {
            logger.info("Sending CAPTCHA to 2Captcha...");

            // 1. 提交任务
            String taskId = submitTask(screenshot, instructions);
            logger.info("Task submitted, ID: {}", taskId);

            // 2. 轮询结果
            CaptchaSolution solution = pollResult(taskId);
            logger.info("CAPTCHA solved successfully, {} coordinates", solution.getData().size());

            return solution;

        } catch (Exception e) {
            logger.error("Failed to solve CAPTCHA", e);
            throw new SunoException("2Captcha solving failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void reportBad(String taskId) {
        try {
            webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/res.php")
                            .queryParam("key", apiKey)
                            .queryParam("action", "reportbad")
                            .queryParam("id", taskId)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            logger.info("Reported bad CAPTCHA solution: {}", taskId);

        } catch (Exception e) {
            logger.warn("Failed to report bad CAPTCHA", e);
        }
    }

    @Override
    public String getName() {
        return "2Captcha";
    }

    /**
     * 提交 CAPTCHA 任务到 2Captcha
     */
    private String submitTask(byte[] screenshot, String instructions) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(screenshot);

            // 构建请求参数
            Map<String, Object> params = Map.of(
                    "key", apiKey,
                    "method", "post",
                    "coordinatescaptcha", "1",
                    "body", base64Image,
                    "json", "1");

            // 如果有指令,添加到参数中
            Map<String, Object> finalParams = instructions != null ? new java.util.HashMap<>(params) : params;
            if (instructions != null) {
                finalParams.put("textinstructions", instructions);
            }

            // 发送请求
            String response = webClient.post()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/in.php");
                        finalParams.forEach((key, value) -> uriBuilder.queryParam(key, value));
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 解析响应
            JsonNode jsonNode = objectMapper.readTree(response);
            if (jsonNode.get("status").asInt() != 1) {
                throw new SunoException("2Captcha task submission failed: " +
                        jsonNode.get("request").asText());
            }

            return jsonNode.get("request").asText();

        } catch (Exception e) {
            throw new SunoException("Failed to submit CAPTCHA task", e);
        }
    }

    /**
     * 轮询 CAPTCHA 结果
     */
    private CaptchaSolution pollResult(String taskId) {
        int maxAttempts = 60; // 最多等待 60 秒
        int attempt = 0;

        while (attempt < maxAttempts) {
            try {
                Thread.sleep(2000); // 每 2 秒检查一次

                String response = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/res.php")
                                .queryParam("key", apiKey)
                                .queryParam("action", "get")
                                .queryParam("id", taskId)
                                .queryParam("json", "1")
                                .build())
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                JsonNode jsonNode = objectMapper.readTree(response);
                int status = jsonNode.get("status").asInt();

                if (status == 1) {
                    // 解析坐标
                    String requestText = jsonNode.get("request").asText();
                    List<Coordinate> coordinates = parseCoordinates(requestText);
                    return new CaptchaSolution(taskId, coordinates);
                }

                // status == 0 表示还在处理中
                attempt++;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SunoException("CAPTCHA polling interrupted", e);
            } catch (Exception e) {
                throw new SunoException("Failed to poll CAPTCHA result", e);
            }
        }

        throw new SunoException("CAPTCHA solving timeout after " + maxAttempts + " attempts");
    }

    /**
     * 解析坐标字符串
     * 格式: "coordinates:x=123,y=456;x=789,y=012"
     */
    private List<Coordinate> parseCoordinates(String coordinatesText) {
        List<Coordinate> coordinates = new ArrayList<>();

        try {
            // 移除 "coordinates:" 前缀
            String coordsOnly = coordinatesText.replace("coordinates:", "");

            // 分割每个坐标点
            String[] points = coordsOnly.split(";");
            for (String point : points) {
                String[] parts = point.split(",");
                if (parts.length == 2) {
                    int x = Integer.parseInt(parts[0].split("=")[1]);
                    int y = Integer.parseInt(parts[1].split("=")[1]);
                    coordinates.add(new Coordinate(x, y));
                }
            }

        } catch (Exception e) {
            logger.error("Failed to parse coordinates: {}", coordinatesText, e);
            throw new SunoException("Invalid coordinate format", e);
        }

        return coordinates;
    }
}
