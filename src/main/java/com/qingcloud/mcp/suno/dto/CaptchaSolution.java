package com.qingcloud.mcp.suno.dto;

import java.util.List;

/**
 * CAPTCHA 解决方案
 * 
 * @author qingcloud-mcp
 */
public class CaptchaSolution {

    private String id;
    private List<Coordinate> data;

    public CaptchaSolution() {
    }

    public CaptchaSolution(String id, List<Coordinate> data) {
        this.id = id;
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Coordinate> getData() {
        return data;
    }

    public void setData(List<Coordinate> data) {
        this.data = data;
    }
}
