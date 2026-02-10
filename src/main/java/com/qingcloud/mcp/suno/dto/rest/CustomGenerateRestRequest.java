package com.qingcloud.mcp.suno.dto.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * REST API 自定义生成请求
 * 
 * @author qingcloud-mcp
 */
public class CustomGenerateRestRequest {

    private String prompt;
    private String tags;
    private String title;

    @JsonProperty("make_instrumental")
    private Boolean makeInstrumental = false;

    @JsonProperty("negative_tags")
    private String negativeTags;

    private String model;

    @JsonProperty("wait_audio")
    private Boolean waitAudio = false;

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
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

    public Boolean getMakeInstrumental() {
        return makeInstrumental;
    }

    public void setMakeInstrumental(Boolean makeInstrumental) {
        this.makeInstrumental = makeInstrumental;
    }

    public String getNegativeTags() {
        return negativeTags;
    }

    public void setNegativeTags(String negativeTags) {
        this.negativeTags = negativeTags;
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
}
