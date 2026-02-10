package com.qingcloud.mcp.xhs.config;

import com.qingcloud.mcp.xhs.tools.XhsToolService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XHS MCP 注解方式配置
 * 使用 Spring AI @Tool 注解和 MethodToolCallbackProvider
 */
@Configuration
@ConditionalOnProperty(name = "mcp.tool.mode", havingValue = "annotation")
public class XhsMcpAnnotationConfig {

    /**
     * 注册 XHS Tools 通过 MethodToolCallbackProvider
     * Spring AI 会自动将这些 tools 转换为 MCP tools
     */
    @Bean
    public ToolCallbackProvider xhsTools(XhsToolService xhsToolService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(xhsToolService)
                .build();
    }
}
