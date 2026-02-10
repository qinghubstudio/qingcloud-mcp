package com.qingcloud.mcp.autoclip.model;

/**
 * 轨道类型枚举
 */
public enum TrackType {
    VIDEO(100), // 视频轨道
    AUDIO(50), // 音频轨道
    TEXT(200), // 文字轨道
    STICKER(150), // 贴纸轨道
    EFFECT(300), // 特效轨道
    FILTER(250); // 滤镜轨道

    private final int renderIndex;

    TrackType(int renderIndex) {
        this.renderIndex = renderIndex;
    }

    public int getRenderIndex() {
        return renderIndex;
    }
}
