// package com.qingcloud.mcp.xhs.config;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.qingcloud.mcp.xhs.browser.PlaywrightBrowserManager;
// import com.qingcloud.mcp.xhs.cookie.CookieManager;
// import com.qingcloud.mcp.xhs.tools.CheckLoginStatusToolFactory;
// import com.qingcloud.mcp.xhs.tools.CommentToolFactory;
// import com.qingcloud.mcp.xhs.tools.FeedsToolFactory;
// import com.qingcloud.mcp.xhs.tools.LoginToolFactory;
// import com.qingcloud.mcp.xhs.tools.PostDetailToolFactory;
// import com.qingcloud.mcp.xhs.tools.PublishContentToolFactory;
// import com.qingcloud.mcp.xhs.tools.PublishVideoToolFactory;
// import com.qingcloud.mcp.xhs.tools.SearchToolFactory;
// import com.qingcloud.mcp.xhs.tools.SetCookiesToolFactory;
// import com.qingcloud.mcp.xhs.tools.UserProfileToolFactory;
// import com.qingcloud.mcp.xhs.util.ImageDownloader;
// import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
// import io.modelcontextprotocol.server.McpServer;
// import io.modelcontextprotocol.server.McpSyncServer;
// import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
// import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
// import
// org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

// /**
// * Spring configuration for XHS MCP server with STDIO transport.
// * Uses browser-based automation approach (no API calls, no signatures).
// * This configuration is active when mcp.tool.mode is set to "factory"
// * (default).
// */
// @Configuration
// @ConditionalOnProperty(name = "mcp.tool.mode", havingValue = "factory",
// matchIfMissing = true)
// public class XhsMcpConfig {

// @Bean
// public ImageDownloader imageDownloader() {
// return new ImageDownloader();
// }

// @Bean
// public McpSyncServer mcpServer(PlaywrightBrowserManager browserManager,
// CookieManager cookieManager,
// ImageDownloader imageDownloader) {
// ObjectMapper objectMapper = new ObjectMapper();
// StdioServerTransportProvider transportProvider = new
// StdioServerTransportProvider(
// new JacksonMcpJsonMapper(objectMapper));

// McpSyncServer server = McpServer.sync(transportProvider)
// .serverInfo("xhs-mcp-server", "1.0.0")
// .capabilities(ServerCapabilities.builder()
// .tools(true)
// .build())
// .build();

// // Register browser-based tools
// server.addTool(LoginToolFactory.create(browserManager));
// server.addTool(CheckLoginStatusToolFactory.create(browserManager));
// server.addTool(SetCookiesToolFactory.create(cookieManager));
// server.addTool(PublishContentToolFactory.create(browserManager,
// imageDownloader));
// server.addTool(PublishVideoToolFactory.create(browserManager));
// server.addTool(SearchToolFactory.create(browserManager));
// server.addTool(FeedsToolFactory.create(browserManager));
// server.addTool(PostDetailToolFactory.create(browserManager));
// server.addTool(CommentToolFactory.create(browserManager));
// server.addTool(UserProfileToolFactory.create(browserManager));

// return server;
// }
// }
