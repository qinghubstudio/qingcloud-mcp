package com.qingcloud.mcp.autoclip.model;

import java.util.UUID;

/**
 * 关键帧类
 */
public class Keyframe {
    private String keyframeId;
    private KeyframeProperty property;
    private long time; // 时间点（微秒，相对于片段开始）
    private double value; // 属性值
    private String easing; // 缓动函数

    public Keyframe() {
        this.keyframeId = UUID.randomUUID().toString().replace("-", "");
        this.easing = "linear";
    }

    public Keyframe(KeyframeProperty property, long time, double value) {
        this();
        this.property = property;
        this.time = time;
        this.value = value;
    }

    public Keyframe(KeyframeProperty property, double timeSec, double value, String easing) {
        this();
        this.property = property;
        this.time = (long) (timeSec * 1_000_000);
        this.value = value;
        this.easing = easing != null ? easing : "linear";
    }

    // Getters and Setters
    public String getKeyframeId() {
        return keyframeId;
    }

    public void setKeyframeId(String keyframeId) {
        this.keyframeId = keyframeId;
    }

    public KeyframeProperty getProperty() {
        return property;
    }

    public void setProperty(KeyframeProperty property) {
        this.property = property;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getEasing() {
        return easing;
    }

    public void setEasing(String easing) {
        this.easing = easing;
    }
}
