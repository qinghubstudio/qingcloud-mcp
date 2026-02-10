package com.qingcloud.mcp.suno.dto.openai;

/**
 * OpenAI Chat Message
 * 
 * @author qingcloud-mcp
 */
public class ChatMessage {

    private String role;
    private String content;

    public ChatMessage() {
    }

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
