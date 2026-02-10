package com.qingcloud.mcp.suno.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 
 * @author qingcloud-mcp
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理 Suno 异常
     */
    @ExceptionHandler(SunoException.class)
    public ResponseEntity<Map<String, Object>> handleSunoException(SunoException e) {
        logger.error("Suno exception: {}", e.getMessage(), e);

        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", e.getMessage());
        error.put("type", "suno_error");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        logger.error("Unexpected exception: {}", e.getMessage(), e);

        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", "Internal server error: " + e.getMessage());
        error.put("type", "internal_error");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * 处理参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("Invalid argument: {}", e.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", e.getMessage());
        error.put("type", "invalid_argument");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
