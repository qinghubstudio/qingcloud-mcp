package com.qingcloud.mcp.suno.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingcloud.mcp.suno.service.SunoApiService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Suno 获取配额工具工厂
 * 
 * @author qingcloud-mcp
 */
public class SunoGetQuotaToolFactory {

    private static final Logger logger = LoggerFactory.getLogger(SunoGetQuotaToolFactory.class);

    /**
     * 创建获取配额工具
     */
    public static McpServerFeatures.SyncToolSpecification create(SunoApiService sunoApiService,
            ObjectMapper objectMapper) {
        Tool getQuotaTool = new Tool(
                "suno_get_quota",
                "Get Suno account credit balance and usage information",
                null,
                new JsonSchema("object", Map.of(), List.of(), null, null, null),
                null, null, null);

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(getQuotaTool)
                .callHandler((exchange, request) -> {
                    try {
                        logger.info("=== Suno Get Quota Tool Called ===");

                        // 调用服务
                        Map<String, Object> credits = sunoApiService.getCredits();

                        // 构建响应
                        String result = objectMapper.writeValueAsString(credits);

                        return CallToolResult.builder()
                                .content(List.of(new TextContent(result)))
                                .isError(false)
                                .build();

                    } catch (Exception e) {
                        logger.error("Suno get quota tool failed", e);
                        return CallToolResult.builder()
                                .content(List.of(new TextContent("{\"error\":\"" + e.getMessage() + "\"}")))
                                .isError(true)
                                .build();
                    }
                })
                .build();
    }
}
