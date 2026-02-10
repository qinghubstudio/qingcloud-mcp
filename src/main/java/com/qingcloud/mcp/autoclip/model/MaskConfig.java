package com.qingcloud.mcp.autoclip.model;

import java.util.UUID;

/**
 * 蒙版配置类
 */
public class MaskConfig {
    private String maskId;
    private MaskType type;
    private double centerX = 0.5; // 中心X (0-1)
    private double centerY = 0.5; // 中心Y (0-1)
    private double size = 1.0; // 大小 (0-1)
    private double rotation = 0; // 旋转角度
    private double feather = 0; // 羽化 (0-1)
    private boolean invert = false; // 是否反转
    private double rectWidth = 1.0; // 矩形宽度
    private double roundCorner = 0; // 圆角 (0-100)

    public MaskConfig() {
        this.maskId = UUID.randomUUID().toString().replace("-", "");
    }

    public MaskConfig(MaskType type) {
        this();
        this.type = type;
    }

    // Getters and Setters
    public String getMaskId() {
        return maskId;
    }

    public void setMaskId(String maskId) {
        this.maskId = maskId;
    }

    public MaskType getType() {
        return type;
    }

    public void setType(MaskType type) {
        this.type = type;
    }

    public double getCenterX() {
        return centerX;
    }

    public void setCenterX(double centerX) {
        this.centerX = centerX;
    }

    public double getCenterY() {
        return centerY;
    }

    public void setCenterY(double centerY) {
        this.centerY = centerY;
    }

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = size;
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    public double getFeather() {
        return feather;
    }

    public void setFeather(double feather) {
        this.feather = feather;
    }

    public boolean isInvert() {
        return invert;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    public double getRectWidth() {
        return rectWidth;
    }

    public void setRectWidth(double rectWidth) {
        this.rectWidth = rectWidth;
    }

    public double getRoundCorner() {
        return roundCorner;
    }

    public void setRoundCorner(double roundCorner) {
        this.roundCorner = roundCorner;
    }
}
