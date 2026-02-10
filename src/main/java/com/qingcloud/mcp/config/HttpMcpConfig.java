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
    public McpSyncServer mcpServer(
            HttpServletStreamableServerTransportProvider transportProvider,
            PlaywrightBrowserManager browserManager,
            CookieManager cookieManager,
            ImageDownloader imageDownloader,
            @Autowired(required = false) SunoToolFactory sunoToolFactory) {

        logger.info("Initializing MCP HTTP Streaming Server...");

        McpSyncServer server = McpServer.sync(transportProvider)
                .serverInfo("qingcloud-mcp-server", "1.0.0")
                .capabilities(ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .build();

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

        int toolCount = 10;

        // Register Suno tools if available
        if (sunoToolFactory != null) {
            logger.info("Registering Suno AI music generation tools...");
            server.addTool(SunoGenerateToolFactory.create(sunoToolFactory.getSunoApiService(), objectMapper()));
            server.addTool(SunoCustomGenerateToolFactory.create(sunoToolFactory.getSunoApiService(), objectMapper()));
            server.addTool(SunoGetMusicToolFactory.create(sunoToolFactory.getSunoApiService(), objectMapper()));
            server.addTool(SunoGetQuotaToolFactory.create(sunoToolFactory.getSunoApiService(), objectMapper()));
            toolCount += 4;
            logger.info("Suno tools registered successfully");
        }

        logger.info("MCP HTTP Streaming Server initialized with {} tools", toolCount);

        return server;
    }
}
