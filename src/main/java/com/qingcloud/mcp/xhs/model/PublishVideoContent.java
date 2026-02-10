package com.qingcloud.mcp.xhs.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 发布视频内容的数据模型
 */
public class PublishVideoContent {

    private String title;
    private String content;
    private List<String> tags;
    private String videoPath;

    public PublishVideoContent() {
        this.tags = new ArrayList<>();
    }

    public PublishVideoContent(String title, String content, String videoPath, List<String> tags) {
        this.title = title;
        this.content = content;
        this.videoPath = videoPath;
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }
}
