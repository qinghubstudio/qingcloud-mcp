package com.qingcloud.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        org.springframework.ai.mcp.server.autoconfigure.McpServerSseWebFluxAutoConfiguration.class,
        org.springframework.ai.mcp.server.common.autoconfigure.McpServerAutoConfiguration.class
})
public class McpApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpApplication.class, args);
    }
}
