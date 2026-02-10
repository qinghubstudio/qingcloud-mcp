package com.qingcloud.mcp.suno.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

/**
 * 音频片段响应 DTO
 * 
 * @author qingcloud-mcp
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AudioClipResponse {

    /**
     * 音频 ID
     */
    private String id;

    /**
     * 音乐标题
     */
    private String title;

    /**
     * 封面图片 URL
     */
    @JsonProperty("image_url")
    private String imageUrl;

    /**
     * 音频文件 URL
     */
    @JsonProperty("audio_url")
    private String audioUrl;

    /**
     * 视频文件 URL
     */
    @JsonProperty("video_url")
    private String videoUrl;

    /**
     * 状态: submitted, queued, streaming, complete, error
     */
    private String status;

    /**
     * 创建时间
     */
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    /**
     * 歌词
     */
    private String lyric;

    /**
     * 模型名称
     */
    @JsonProperty("model_name")
    private String modelName;

    /**
     * GPT 描述提示词
     */
    @JsonProperty("gpt_description_prompt")
    private String gptDescriptionPrompt;

    /**
     * 用户提示词
     */
    private String prompt;

    /**
     * 音乐类型
     */
    private String type;

    /**
     * 音乐风格标签
     */
    private String tags;

    /**
     * 负向标签
     */
    @JsonProperty("negative_tags")
    private String negativeTags;

    /**
     * 时长
     */
    private String duration;

    /**
     * 错误信息
     */
    @JsonProperty("error_message")
    private String errorMessage;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getLyric() {
        return lyric;
    }

    public void setLyric(String lyric) {
        this.lyric = lyric;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getGptDescriptionPrompt() {
        return gptDescriptionPrompt;
    }

    public void setGptDescriptionPrompt(String gptDescriptionPrompt) {
        this.gptDescriptionPrompt = gptDescriptionPrompt;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getNegativeTags() {
        return negativeTags;
    }

    public void setNegativeTags(String negativeTags) {
        this.negativeTags = negativeTags;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
