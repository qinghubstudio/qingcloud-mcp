package com.qingcloud.mcp.autoclip.model;

/**
 * 音频片段类
 */
public class AudioSegment extends BaseSegment {
    private double speed = 1.0;
    private double volume = 1.0;
    private String effectType;

    public AudioSegment() {
        super();
    }

    public AudioSegment(String materialId, Timerange target, Timerange source) {
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

    public String getEffectType() {
        return effectType;
    }

    public void setEffectType(String effectType) {
        this.effectType = effectType;
    }
}
