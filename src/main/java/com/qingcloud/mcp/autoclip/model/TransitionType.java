package com.qingcloud.mcp.autoclip.model;

/**
 * 转场类型枚举
 * 参考剪映/CapCut 支持的转场效果
 */
public enum TransitionType {
    // 基础转场
    DISSOLVE("溶解"),
    FADE("淡入淡出"),
    WIPE_LEFT("左擦除"),
    WIPE_RIGHT("右擦除"),
    WIPE_UP("上擦除"),
    WIPE_DOWN("下擦除"),

    // 滑动转场
    SLIDE_LEFT("左滑动"),
    SLIDE_RIGHT("右滑动"),
    SLIDE_UP("上滑动"),
    SLIDE_DOWN("下滑动"),

    // 缩放转场
    ZOOM_IN("放大"),
    ZOOM_OUT("缩小"),

    // 旋转转场
    ROTATE_CW("顺时针旋转"),
    ROTATE_CCW("逆时针旋转"),

    // 特效转场
    BLUR("模糊"),
    FLASH("闪白"),
    CIRCLE("圆形"),
    HEART("心形"),
    STAR("星形");

    private final String displayName;

    TransitionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
