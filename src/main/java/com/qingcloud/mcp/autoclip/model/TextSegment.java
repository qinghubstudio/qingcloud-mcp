package com.qingcloud.mcp.autoclip.model;

/**
 * 文本片段类
 */
public class TextSegment extends BaseSegment {
    private String text;
    private String font;
    private String fontColor = "#ffffff";
    private double fontSize = 8.0;
    private double fontAlpha = 1.0;
    private double transformX = 0;
    private double transformY = -0.8; // 默认底部
    private boolean vertical = false;

    // 描边
    private String borderColor;
    private double borderWidth = 0;
    private double borderAlpha = 1.0;

    // 背景
    private String backgroundColor;
    private double backgroundAlpha = 0;

    // 阴影
    private boolean shadowEnabled = false;
    private String shadowColor = "#000000";
    private double shadowAlpha = 0.9;

    // 动画
    private String introAnimation;
    private String outroAnimation;

    public TextSegment() {
        super();
    }

    public TextSegment(String text, Timerange targetRange) {
        super();
        this.text = text;
        this.targetTimerange = targetRange;
        this.sourceTimerange = targetRange;
    }

    // Getters and Setters
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getFont() {
        return font;
    }

    public void setFont(String font) {
        this.font = font;
    }

    public String getFontColor() {
        return fontColor;
    }

    public void setFontColor(String fontColor) {
        this.fontColor = fontColor;
    }

    public double getFontSize() {
        return fontSize;
    }

    public void setFontSize(double fontSize) {
        this.fontSize = fontSize;
    }

    public double getFontAlpha() {
        return fontAlpha;
    }

    public void setFontAlpha(double fontAlpha) {
        this.fontAlpha = fontAlpha;
    }

    public double getTransformX() {
        return transformX;
    }

    public void setTransformX(double transformX) {
        this.transformX = transformX;
    }

    public double getTransformY() {
        return transformY;
    }

    public void setTransformY(double transformY) {
        this.transformY = transformY;
    }

    public boolean isVertical() {
        return vertical;
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }

    public String getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(String borderColor) {
        this.borderColor = borderColor;
    }

    public double getBorderWidth() {
        return borderWidth;
    }

    public void setBorderWidth(double borderWidth) {
        this.borderWidth = borderWidth;
    }

    public double getBorderAlpha() {
        return borderAlpha;
    }

    public void setBorderAlpha(double borderAlpha) {
        this.borderAlpha = borderAlpha;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public double getBackgroundAlpha() {
        return backgroundAlpha;
    }

    public void setBackgroundAlpha(double backgroundAlpha) {
        this.backgroundAlpha = backgroundAlpha;
    }

    public boolean isShadowEnabled() {
        return shadowEnabled;
    }

    public void setShadowEnabled(boolean shadowEnabled) {
        this.shadowEnabled = shadowEnabled;
    }

    public String getShadowColor() {
        return shadowColor;
    }

    public void setShadowColor(String shadowColor) {
        this.shadowColor = shadowColor;
    }

    public double getShadowAlpha() {
        return shadowAlpha;
    }

    public void setShadowAlpha(double shadowAlpha) {
        this.shadowAlpha = shadowAlpha;
    }

    public String getIntroAnimation() {
        return introAnimation;
    }

    public void setIntroAnimation(String introAnimation) {
        this.introAnimation = introAnimation;
    }

    public String getOutroAnimation() {
        return outroAnimation;
    }

    public void setOutroAnimation(String outroAnimation) {
        this.outroAnimation = outroAnimation;
    }
}
