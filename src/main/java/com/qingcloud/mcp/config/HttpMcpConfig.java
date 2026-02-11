package com.qingcloud.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingcloud.mcp.suno.tools.*;
import com.qingcloud.mcp.xhs.browser.PlaywrightBrowserManager;
import com.qingcloud.mcp.xhs.cookie.CookieManager;
import com.qingcloud.mcp.xhs.tools.CheckLoginStatusToolFactory;
import com.qingcloud.mcp.xhs.tools.CommentToolFactory;
import com.qingcloud.mcp.xhs.tools.FeedsToolFactory;
import com.qingcloud.mcp.xhs.tools.LoginToolFactory;
import com.qingcloud.mcp.xhs.tools.PostDetailToolFactory;
import com.qingcloud.mcp.xhs.tools.PublishContentToolFactory;
import com.qingcloud.mcp.xhs.tools.PublishVideoToolFactory;
import com.qingcloud.mcp.xhs.tools.SearchToolFactory;
import com.qingcloud.mcp.xhs.tools.SetCookiesToolFactory;
import com.qingcloud.mcp.xhs.tools.UserProfileToolFactory;
import com.qingcloud.mcp.xhs.util.ImageDownloader;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for HTTP streaming transport mode.
 * Uses browser-based automation approach (no API calls, no signatures).
 */
@Configuration
@ConditionalOnProperty(name = "mcp.transport.mode", havingValue = "http", matchIfMissing = false)
public class HttpMcpConfig {

    private static final Logger logger = LoggerFactory.getLogger(HttpMcpConfig.class);

    @Value("${mcp.tool.mode:factory}")
    private String toolMode;

    @Autowired(required = false)
    private java.util.List<org.springframework.ai.tool.ToolCallbackProvider> toolCallbackProviders;

    @Value("${mcp.http.endpoint:/mcp}")
    private String mcpEndpoint;

    @Bean
    public HttpServletStreamableServerTransportProvider httpTransportProvider(ObjectMapper objectMapper) {
        logger.info("Creating HTTP Streamable transport provider with endpoint: {}", mcpEndpoint);
        return HttpServletStreamableServerTransportProvider.builder()
                .jsonMapper(new JacksonMcpJsonMapper(objectMapper))
                .mcpEndpoint(mcpEndpoint)
                .build();
    }

    @Bean
    public ServletRegistrationBean<HttpServletStreamableServerTransportProvider> mcpServlet(
            HttpServletStreamableServerTransportProvider transportProvider) {
        logger.info("Registering MCP servlet at: {}/*", mcpEndpoint);
        ServletRegistrationBean<HttpServletStreamableServerTransportProvider> registration = new ServletRegistrationBean<>(
                transportProvider, mcpEndpoint + "/*");
        registration.setName("mcpStreamableServlet");
        registration.setAsyncSupported(true);
        return registration;
    }

    @Bean
    public McpSyncServer mcpServer(
            HttpServletStreamableServerTransportProvider transportProvider,
            PlaywrightBrowserManager browserManager,
            CookieManager cookieManager,
            ImageDownloader imageDownloader,
            ObjectMapper objectMapper,
            @Autowired(required = false) SunoToolFactory sunoToolFactory) {

        logger.info("Initializing MCP HTTP Streaming Server...");
        logger.info("Tool mode: {}", toolMode);

        McpSyncServer server = McpServer.sync(transportProvider)
                .serverInfo("qingcloud-mcp-server", "1.0.0")
                .capabilities(ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .build();

        int toolCount = 0;

        if ("annotation".equalsIgnoreCase(toolMode) && toolCallbackProviders != null) {
            logger.info("Registering tools from Spring AI @Tool annotations...");
            ObjectMapper mapper = objectMapper;

            for (org.springframework.ai.tool.ToolCallbackProvider provider : toolCallbackProviders) {
                for (org.springframework.ai.tool.ToolCallback callback : provider.getToolCallbacks()) {
                    org.springframework.ai.tool.definition.ToolDefinition def = callback.getToolDefinition();
                    logger.info("Registering annotation tool: {}", def.name());

                    try {
                        // Parse input schema
                        Object schemaObj = mapper.readValue(def.inputSchema(), Object.class);
                        java.util.Map<String, Object> schemaMap = (java.util.Map<String, Object>) schemaObj;

                        // Create MCP Tool
                        io.modelcontextprotocol.spec.McpSchema.Tool mcpTool = new io.modelcontextprotocol.spec.McpSchema.Tool(
                                def.name(),
                                def.description(),
                                null,
                                new io.modelcontextprotocol.spec.McpSchema.JsonSchema(
                                        "object",
                                        (java.util.Map<String, Object>) schemaMap.get("properties"),
                                        (java.util.List<String>) schemaMap.get("required"),
                                        null, null, null),
                                null, null, null);

                        server.addTool(io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification.builder()
                                .tool(mcpTool)
                                .callHandler((exchange, request) -> {
                                    try {
                                        // Spring AI callback expects input as JSON string
                                        String argsJson = mapper.writeValueAsString(request.arguments());
                                        String result = callback.call(argsJson);

                                        return io.modelcontextprotocol.spec.McpSchema.CallToolResult.builder()
                                                .content(java.util.List.of(
                                                        new io.modelcontextprotocol.spec.McpSchema.TextContent(result)))
                                                .isError(false)
                                                .build();
                                    } catch (Exception e) {
                                        logger.error("Error executing tool " + def.name(), e);
                                        return io.modelcontextprotocol.spec.McpSchema.CallToolResult.builder()
                                                .content(java.util.List
                                                        .of(new io.modelcontextprotocol.spec.McpSchema.TextContent(
                                                                "Error: " + e.getMessage())))
                                                .isError(true)
                                                .build();
                                    }
                                })
                                .build());
                        toolCount++;
                    } catch (Exception e) {
                        logger.error("Failed to register tool: " + def.name(), e);
                    }
                }
            }
        } else {
            // Default Factory Mode
            logger.info("Registering tools using Factory mode...");

            // Register XHS browser-based tools
            server.addTool(LoginToolFactory.create(browserManager));
            server.addTool(CheckLoginStatusToolFactory.create(browserManager));
            server.addTool(SetCookiesToolFactory.create(cookieManager));
            server.addTool(PublishContentToolFactory.create(browserManager, imageDownloader));
            server.addTool(PublishVideoToolFactory.create(browserManager));
            server.addTool(SearchToolFactory.create(browserManager));
            server.addTool(FeedsToolFactory.create(browserManager));
            server.addTool(PostDetailToolFactory.create(browserManager));
            server.addTool(CommentToolFactory.create(browserManager));
            server.addTool(UserProfileToolFactory.create(browserManager));

            toolCount += 10;

            // Register Suno tools if available
            if (sunoToolFactory != null) {
                logger.info("Registering Suno AI music generation tools...");
                server.addTool(SunoGenerateToolFactory.create(sunoToolFactory.getSunoApiService(), objectMapper));
                server.addTool(
                        SunoCustomGenerateToolFactory.create(sunoToolFactory.getSunoApiService(), objectMapper));
                server.addTool(SunoGetMusicToolFactory.create(sunoToolFactory.getSunoApiService(), objectMapper));
                server.addTool(SunoGetQuotaToolFactory.create(sunoToolFactory.getSunoApiService(), objectMapper));
                toolCount += 4;
                logger.info("Suno tools registered successfully");
            }
        }

        logger.info("MCP HTTP Streaming Server initialized with {} tools", toolCount);

        return server;
    }
}
