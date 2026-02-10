package com.qingcloud.mcp.suno.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 音乐生成请求 DTO
 * 
 * @author qingcloud-mcp
 */
public class GenerateRequest {

    /**
     * 提示词 (必需)
     */
    private String prompt;

    /**
     * 是否生成纯音乐 (无人声)
     */
    @JsonProperty("make_instrumental")
    private Boolean makeInstrumental = false;

    /**
     * 模型版本
     */
    private String model = "chirp-v3.5";

    /**
     * 是否等待音频生成完成
     */
    @JsonProperty("wait_audio")
    private Boolean waitAudio = false;

    /**
     * 音乐风格标签 (自定义模式)
     */
    private String tags;

    /**
     * 音乐标题 (自定义模式)
     */
    private String title;

    /**
     * 负向标签 (自定义模式)
     */
    @JsonProperty("negative_tags")
    private String negativeTags;

    // Getters and Setters
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Boolean getMakeInstrumental() {
        return makeInstrumental;
    }

    public void setMakeInstrumental(Boolean makeInstrumental) {
        this.makeInstrumental = makeInstrumental;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Boolean getWaitAudio() {
        return waitAudio;
    }

    public void setWaitAudio(Boolean waitAudio) {
        this.waitAudio = waitAudio;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNegativeTags() {
        return negativeTags;
    }

    public void setNegativeTags(String negativeTags) {
        this.negativeTags = negativeTags;
    }
}
