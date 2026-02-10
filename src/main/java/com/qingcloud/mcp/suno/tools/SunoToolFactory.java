package com.qingcloud.mcp.suno.tools;

import com.qingcloud.mcp.suno.service.SunoApiService;
import org.springframework.stereotype.Component;

/**
 * Suno 工具工厂 - 提供对 SunoApiService 的访问
 * 
 * @author qingcloud-mcp
 */
@Component
public class SunoToolFactory {

    private final SunoApiService sunoApiService;

    public SunoToolFactory(SunoApiService sunoApiService) {
        this.sunoApiService = sunoApiService;
    }

    /**
     * 获取 SunoApiService 实例
     */
    public SunoApiService getSunoApiService() {
        return sunoApiService;
    }
}
