package com.qingcloud.mcp.autoclip.model;

/**
 * 蒙版类型枚举
 */
public enum MaskType {
    CIRCLE("圆形"),
    RECTANGLE("矩形"),
    LINEAR("线性渐变"),
    MIRROR("镜像"),
    HEART("心形"),
    STAR("星形"),
    DIAMOND("菱形"),
    OVAL("椭圆");

    private final String displayName;

    MaskType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
