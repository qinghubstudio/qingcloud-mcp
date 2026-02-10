package com.qingcloud.mcp.suno.browser;

import com.qingcloud.mcp.suno.dto.CaptchaSolution;

/**
 * CAPTCHA 解决器接口
 * 支持多种实现方式的可插拔架构
 * 
 * @author qingcloud-mcp
 */
public interface ICaptchaSolver {

    /**
     * 解决坐标类型的 CAPTCHA
     * 
     * @param screenshot   截图的字节数组
     * @param instructions 文本指令 (可选)
     * @return CAPTCHA 解决方案
     */
    CaptchaSolution solve(byte[] screenshot, String instructions);

    /**
     * 报告错误的 CAPTCHA 解答
     * 
     * @param taskId 任务 ID
     */
    void reportBad(String taskId);

    /**
     * 获取解决器名称
     * 
     * @return 解决器名称
     */
    String getName();
}
