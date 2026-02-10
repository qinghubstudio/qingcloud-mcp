package com.qingcloud.mcp.autoclip.model;

import java.util.UUID;

/**
 * 视频素材类（包含图片，图片的type为"photo"）
 */
public class VideoMaterial {
    private String materialId;
    private String materialName;
    private String materialType; // "video" or "photo"
    private String remoteUrl;
    private String replacePath;
    private long duration; // 微秒
    private int width;
    private int height;

    public VideoMaterial() {
        this.materialId = UUID.randomUUID().toString().replace("-", "");
    }

    public static VideoMaterial createVideo(String remoteUrl, String materialName) {
        VideoMaterial m = new VideoMaterial();
        m.materialType = "video";
        m.remoteUrl = remoteUrl;
        m.materialName = materialName;
        return m;
    }

    public static VideoMaterial createImage(String remoteUrl, String materialName) {
        VideoMaterial m = new VideoMaterial();
        m.materialType = "photo";
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

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
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

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
