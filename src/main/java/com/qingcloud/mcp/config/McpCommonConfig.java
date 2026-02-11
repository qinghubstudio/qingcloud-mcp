package com.qingcloud.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingcloud.mcp.xhs.util.ImageDownloader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Common configuration for shared beans.
 * Extracts beans to avoid circular dependencies between HttpMcpConfig and
 * Service beans.
 */
@Configuration
public class McpCommonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public ImageDownloader imageDownloader() {
        return new ImageDownloader();
    }
}
