package com.qingcloud.mcp.xhs.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 发布图文内容的数据模型
 */
public class PublishImageContent {

    private String title;
    private String content;
    private List<String> tags;
    private List<String> imagePaths;

    public PublishImageContent() {
        this.tags = new ArrayList<>();
        this.imagePaths = new ArrayList<>();
    }

    public PublishImageContent(String title, String content, List<String> imagePaths, List<String> tags) {
        this.title = title;
        this.content = content;
        this.imagePaths = imagePaths != null ? imagePaths : new ArrayList<>();
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

    public List<String> getImagePaths() {
        return imagePaths;
    }

    public void setImagePaths(List<String> imagePaths) {
        this.imagePaths = imagePaths != null ? imagePaths : new ArrayList<>();
    }
}
