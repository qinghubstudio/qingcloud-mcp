package com.qingcloud.mcp.autoclip.model;

/**
 * 滤镜类型枚举
 */
public enum FilterType {
    // 基础滤镜
    NONE("无"),
    ORIGINAL("原色"),

    // 色调滤镜
    WARM("暖色"),
    COOL("冷色"),
    VINTAGE("复古"),
    BLACK_WHITE("黑白"),
    SEPIA("棕褐"),

    // 风格滤镜
    FILM("胶片"),
    CINEMATIC("电影"),
    VLOG("日常"),
    FOOD("美食"),
    PORTRAIT("人像"),
    LANDSCAPE("风景"),

    // 氛围滤镜
    DREAMY("梦幻"),
    SUNSET("日落"),
    NIGHT("夜景"),
    MOODY("情绪"),

    // 特效滤镜
    GLITCH("故障"),
    NEON("霓虹"),
    CYBERPUNK("赛博朋克");

    private final String displayName;

    FilterType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
