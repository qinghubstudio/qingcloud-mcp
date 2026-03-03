package com.qingcloud.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingcloud.mcp.suno.tools.*;
import com.qingcloud.mcp.xhs.browser.PlaywrightBrowserManager;
import com.qingcloud.mcp.xhs.cookie.CookieManager;
import com.qingcloud.mcp.xhs.util.ImageDownloader;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * Spring configuration for HTTP streaming transport mode.
 * Uses browser-based automation approach (no API calls, no signatures).
 */
@Configuration
@ConditionalOnProperty(name = "mcp.transport.mode", havingValue = "http", matchIfMissing = false)
public class HttpMcpConfig {

    private static final Logger logger = LoggerFactory.getLogger(HttpMcpConfig.class);

    @Value("${mcp.http.endpoint:/mcp}")
    private String mcpEndpoint;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public ImageDownloader imageDownloader() {
        return new ImageDownloader();
    }

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
    public org.springframework.boot.web.servlet.FilterRegistrationBean<jakarta.servlet.Filter> mcpCompatibilityFilter() {
        org.springframework.boot.web.servlet.FilterRegistrationBean<jakarta.servlet.Filter> registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>();
        registration.setFilter(new jakarta.servlet.Filter() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response,
                    jakarta.servlet.FilterChain chain)
                    throws java.io.IOException, jakarta.servlet.ServletException {
                if (request instanceof jakarta.servlet.http.HttpServletRequest) {
                    jakarta.servlet.http.HttpServletRequest req = (jakarta.servlet.http.HttpServletRequest) request;
                    String path = req.getRequestURI();

                    // Only apply to MCP endpoints
                    if (path.startsWith(mcpEndpoint)) {
                        final String method = req.getMethod();
                        final String originalAccept = req.getHeader("Accept");

                        logger.info("MCP Request: {} {} - SessionID: {} - Accept: {}",
                                method, path, req.getHeader("mcp-session-id"), originalAccept);

                        // Wrap request to inject Accept header if needed
                        jakarta.servlet.http.HttpServletRequestWrapper wrapped = new jakarta.servlet.http.HttpServletRequestWrapper(
                                req) {
                            @Override
                            public String getHeader(String name) {
                                if ("Accept".equalsIgnoreCase(name)) {
                                    if (originalAccept == null || originalAccept.contains("*/*")) {
                                        // Inject both types to satisfy strict server requirements
                                        String combinedAccept = "application/json, text/event-stream";
                                        logger.info("Injecting Accept: {}", combinedAccept);
                                        return combinedAccept;
                                    }
                                }
                                return super.getHeader(name);
                            }

                            @Override
                            public java.util.Enumeration<String> getHeaders(String name) {
                                if ("Accept".equalsIgnoreCase(name)) {
                                    String val = getHeader(name);
                                    if (val != null) {
                                        return java.util.Collections
                                                .enumeration(java.util.Collections.singletonList(val));
                                    }
                                }
                                return super.getHeaders(name);
                            }
                        };
                        chain.doFilter(wrapped, response);
                        return;
                    }
                }
                chain.doFilter(request, response);
            }
        });
        registration.addUrlPatterns(mcpEndpoint + "/*");
        registration.setName("mcpCompatibilityFilter");
        registration.setOrder(Integer.MIN_VALUE);
        return registration;
    }

    @Bean
    public McpSyncServer mcpServer(
            HttpServletStreamableServerTransportProvider transportProvider,
            PlaywrightBrowserManager browserManager,
            CookieManager cookieManager,
            ImageDownloader imageDownloader,
            ObjectMapper objectMapper,
            @Autowired(required = false) SunoToolFactory sunoToolFactory,
            @Autowired(required = false) List<ToolCallbackProvider> toolCallbackProviders) {

        logger.info("Initializing MCP HTTP Streaming Server...");

        McpSyncServer server = McpServer.sync(transportProvider)
                .serverInfo("qingcloud-mcp-server", "1.0.0")
                .capabilities(ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .build();

        int toolCount = 0;

        // Register Suno tools if available
        // Note: We now auto-discover all tools provided by Spring AI
        // ToolCallbackProviders
        // This includes XHS tools (from XhsMcpAnnotationConfig) and Suno tools (from
        // SunoMcpAnnotationConfig)

        if (toolCallbackProviders != null) {
            logger.info("Discovering tools from {} providers...", toolCallbackProviders.size());
            for (ToolCallbackProvider provider : toolCallbackProviders) {
                for (ToolCallback callback : provider.getToolCallbacks()) {
                    ToolDefinition def = callback.getToolDefinition();
                    logger.info("Registering tool: {}", def.name());

                    try {
                        // Convert Spring AI schema to MCP JsonSchema
                        String schemaJson = def.inputSchema();
                        Map<String, Object> schemaMap = objectMapper.readValue(schemaJson, Map.class);

                        // Extract standard JSON Schema fields
                        String type = (String) schemaMap.getOrDefault("type", "object");
                        Map<String, Object> properties = (Map<String, Object>) schemaMap.get("properties");
                        List<String> required = (List<String>) schemaMap.get("required");

                        JsonSchema inputSchema = new JsonSchema(
                                type,
                                properties,
                                required,
                                null, null, null);

                        Tool tool = new Tool(
                                def.name(),
                                def.description(),
                                null,
                                inputSchema,
                                null, null, null);

                        server.addTool(McpServerFeatures.SyncToolSpecification.builder()
                                .tool(tool)
                                .callHandler((exchange, request) -> {
                                    try {
                                        Map<String, Object> args = request.arguments();
                                        String argsJson = objectMapper.writeValueAsString(args);
                                        String result = callback.call(argsJson);
                                        return CallToolResult.builder()
                                                .content(List.of(new TextContent(result)))
                                                .isError(false)
                                                .build();
                                    } catch (Exception e) {
                                        logger.error("Error executing tool " + def.name(), e);
                                        return CallToolResult.builder()
                                                .content(List
                                                        .of(new TextContent("{\"error\":\"" + e.getMessage() + "\"}")))
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
        }

        logger.info("MCP HTTP Streaming Server initialized with {} tools", toolCount);

        return server;
    }
}
