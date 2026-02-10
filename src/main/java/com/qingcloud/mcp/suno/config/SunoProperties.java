package com.qingcloud.mcp.suno.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Suno API 配置属性
 * 
 * @author qingcloud-mcp
 */
@Configuration
@ConfigurationProperties(prefix = "suno")
public class SunoProperties {

    /**
     * Suno Cookie (必需)
     */
    private String cookie;

    /**
     * 2Captcha API Key (必需)
     */
    private CaptchaConfig captcha = new CaptchaConfig();

    /**
     * 浏览器配置
     */
    private BrowserConfig browser = new BrowserConfig();

    public static class CaptchaConfig {
        private String key;
        private int timeout = 30000; // 默认 30 秒

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }

    public static class BrowserConfig {
        private boolean headless = true;
        private String locale = "zh-CN";
        private int instanceLimit = 5; // 最大并发浏览器上下文数

        public boolean isHeadless() {
            return headless;
        }

        public void setHeadless(boolean headless) {
            this.headless = headless;
        }

        public String getLocale() {
            return locale;
        }

        public void setLocale(String locale) {
            this.locale = locale;
        }

        public int getInstanceLimit() {
            return instanceLimit;
        }

        public void setInstanceLimit(int instanceLimit) {
            this.instanceLimit = instanceLimit;
        }
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public CaptchaConfig getCaptcha() {
        return captcha;
    }

    public void setCaptcha(CaptchaConfig captcha) {
        this.captcha = captcha;
    }

    public BrowserConfig getBrowser() {
        return browser;
    }

    public void setBrowser(BrowserConfig browser) {
        this.browser = browser;
    }
}
