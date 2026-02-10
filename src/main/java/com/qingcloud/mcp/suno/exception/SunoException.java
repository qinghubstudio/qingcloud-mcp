package com.qingcloud.mcp.suno.exception;

/**
 * Suno API 异常基类
 * 
 * @author qingcloud-mcp
 */
public class SunoException extends RuntimeException {

    private final int status;

    public SunoException(String message) {
        super(message);
        this.status = 500;
    }

    public SunoException(String message, int status) {
        super(message);
        this.status = status;
    }

    public SunoException(String message, Throwable cause) {
        super(message, cause);
        this.status = 500;
    }

    public SunoException(String message, Throwable cause, int status) {
        super(message, cause);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
