package com.qingcloud.mcp.suno.dto.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * REST API 生成音乐请求
 * 
 * @author qingcloud-mcp
 */
public class GenerateRestRequest {

    private String prompt;

    @JsonProperty("make_instrumental")
    private Boolean makeInstrumental = false;

    private String model;

    @JsonProperty("wait_audio")
    private Boolean waitAudio = false;

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
}
