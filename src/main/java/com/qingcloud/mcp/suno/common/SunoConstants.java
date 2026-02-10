package com.qingcloud.mcp.suno.common;

/**
 * Suno API 常量定义
 * 
 * @author qingcloud-mcp
 */
public class SunoConstants {

    /**
     * Suno API 基础 URL
     */
    public static final String BASE_URL = "https://studio-api.prod.suno.com";

    /**
     * Clerk 认证基础 URL
     */
    public static final String CLERK_BASE_URL = "https://clerk.suno.com";

    /**
     * Clerk JS 版本
     */
    public static final String CLERK_VERSION = "5.15.0";

    /**
     * 默认模型版本
     */
    public static final String DEFAULT_MODEL = "chirp-v3.5";

    /**
     * Suno 创作页面 URL
     */
    public static final String CREATE_PAGE_URL = "https://suno.com/create";

    /**
     * API 端点
     */
    public static class Endpoints {
        public static final String GENERATE_V2 = "/api/generate/v2/";
        public static final String GENERATE_LYRICS = "/api/generate/lyrics/";
        public static final String EXTEND_AUDIO = "/api/extend_audio/";
        public static final String FEED_V2 = "/api/feed/v2";
        public static final String BILLING_INFO = "/api/billing/info/";
        public static final String GENERATE_STEMS = "/api/edit/stems/";
        public static final String CONCAT_V2 = "/api/generate/concat/v2/";
        public static final String CLIP = "/api/clip/";
        public static final String ALIGNED_LYRICS = "/api/gen/{id}/aligned_lyrics/v2/";
        public static final String CAPTCHA_CHECK = "/api/c/check";
    }

    /**
     * 音频状态枚举
     */
    public enum AudioStatus {
        SUBMITTED("submitted"),
        QUEUED("queued"),
        STREAMING("streaming"),
        COMPLETE("complete"),
        ERROR("error");

        private final String value;

        AudioStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static AudioStatus fromValue(String value) {
            for (AudioStatus status : values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            return SUBMITTED;
        }
    }

    /**
     * 超时配置 (毫秒)
     */
    public static class Timeouts {
        public static final int PAGE_NAVIGATION = 0; // 无限制
        public static final int PAGE_API_RESPONSE = 30000; // 30秒
        public static final int POPUP_CLOSE = 2000; // 2秒
        public static final int TEXTAREA_WAIT = 3000; // 3秒
        public static final int CREATE_BUTTON_WAIT = 5000; // 5秒
        public static final int CAPTCHA_SCREENSHOT = 5000; // 5秒
        public static final int API_GENERATE = 60000; // 60秒
        public static final int AUDIO_GENERATION_MAX = 100000; // 100秒
    }
}
