package com.qingcloud.mcp.autoclip.model;

import java.util.UUID;

/**
 * 音频素材类
 */
public class AudioMaterial {
    private String materialId;
    private String materialName;
    private String remoteUrl;
    private String replacePath;
    private long duration; // 微秒

    public AudioMaterial() {
        this.materialId = UUID.randomUUID().toString().replace("-", "");
    }

    public static AudioMaterial create(String remoteUrl, String materialName) {
        AudioMaterial m = new AudioMaterial();
        m.remoteUrl = remoteUrl;
        m.materialName = materialName;
        return m;
    }

    // Getters and Setters
    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    public String getReplacePath() {
        return replacePath;
    }

    public void setReplacePath(String replacePath) {
        this.replacePath = replacePath;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
