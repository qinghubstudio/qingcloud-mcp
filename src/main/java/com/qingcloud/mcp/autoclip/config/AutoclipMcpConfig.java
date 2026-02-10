package com.qingcloud.mcp.autoclip.config;

import com.qingcloud.mcp.autoclip.service.DraftExportService;
import com.qingcloud.mcp.autoclip.service.DraftService;
import com.qingcloud.mcp.autoclip.tools.*;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Autoclip MCP 配置类
 */
@Configuration
@Profile("autoclip")
public class AutoclipMcpConfig {

    private static final Logger logger = LoggerFactory.getLogger(AutoclipMcpConfig.class);

    @Autowired
    private DraftService draftService;

    @Autowired
    private DraftExportService exportService;

    @Bean
    public McpSyncServer autoclipMcpServer(HttpServletStreamableServerTransportProvider transportProvider) {
        logger.info("Initializing Autoclip MCP Server...");

        McpSyncServer server = McpServer.sync(transportProvider)
                .serverInfo("autoclip-mcp-server", "1.0.0")
                .capabilities(ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .build();

        // 草稿管理 (4个)
        server.addTool(CreateDraftToolFactory.create(draftService));
        server.addTool(GetDraftInfoToolFactory.create(draftService));
        server.addTool(SaveDraftToolFactory.create(exportService));
        server.addTool(DeleteDraftToolFactory.create(draftService));

        // 媒体添加 (5个)
        server.addTool(AddVideoToolFactory.create(draftService));
        server.addTool(AddAudioToolFactory.create(draftService));
        server.addTool(AddImageToolFactory.create(draftService));
        server.addTool(AddTextToolFactory.create(draftService));
        server.addTool(AddSubtitleToolFactory.create(draftService));

        // 效果设置 (5个)
        server.addTool(AddKeyframeToolFactory.create(draftService));
        server.addTool(SetTransitionToolFactory.create(draftService));
        server.addTool(SetMaskToolFactory.create(draftService));
        server.addTool(SetFilterToolFactory.create(draftService));
        server.addTool(SetSpeedToolFactory.create(draftService));

        logger.info("Autoclip MCP Server initialized with 14 tools");

        return server;
    }
}
