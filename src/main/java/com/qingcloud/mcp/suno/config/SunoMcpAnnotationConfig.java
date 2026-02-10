package com.qingcloud.mcp.suno.config;

import com.qingcloud.mcp.suno.service.SunoApiService;
import com.qingcloud.mcp.suno.tools.SunoToolService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Suno MCP 注解方式配置
 * 使用 Spring AI @Tool 注解和 MethodToolCallbackProvider
 * 仅在 SunoApiService 可用且模式为 annotation 时启用
 */
@Configuration
@ConditionalOnProperty(name = "mcp.tool.mode", havingValue = "annotation")
@ConditionalOnBean(SunoApiService.class)
public class SunoMcpAnnotationConfig {

    /**
     * 注册 Suno Tools 通过 MethodToolCallbackProvider
     * Spring AI 会自动将这些 tools 转换为 MCP tools
     */
    @Bean
    public ToolCallbackProvider sunoTools(SunoToolService sunoToolService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(sunoToolService)
                .build();
    }
}
