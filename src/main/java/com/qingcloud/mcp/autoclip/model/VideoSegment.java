package com.qingcloud.mcp.autoclip.model;

/**
 * 视频片段类
 */
public class VideoSegment extends BaseSegment {
    private double speed = 1.0;
    private double volume = 1.0;
    private double transformX = 0;
    private double transformY = 0;
    private double scaleX = 1.0;
    private double scaleY = 1.0;
    private String transition;
    private long transitionDuration; // 微秒
    private String maskType;

    public VideoSegment() {
        super();
    }

    public VideoSegment(String materialId, Timerange target, Timerange source) {
        super();
        this.materialId = materialId;
        this.targetTimerange = target;
        this.sourceTimerange = source;
    }

    // Getters and Setters
    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
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

    public double getScaleX() {
        return scaleX;
    }

    public void setScaleX(double scaleX) {
        this.scaleX = scaleX;
    }

    public double getScaleY() {
        return scaleY;
    }

    public void setScaleY(double scaleY) {
        this.scaleY = scaleY;
    }

    public String getTransition() {
        return transition;
    }

    public void setTransition(String transition) {
        this.transition = transition;
    }

    public long getTransitionDuration() {
        return transitionDuration;
    }

    public void setTransitionDuration(long transitionDuration) {
        this.transitionDuration = transitionDuration;
    }

    public String getMaskType() {
        return maskType;
    }

    public void setMaskType(String maskType) {
        this.maskType = maskType;
    }
}
