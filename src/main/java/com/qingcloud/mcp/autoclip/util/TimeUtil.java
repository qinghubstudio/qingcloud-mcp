package com.qingcloud.mcp.autoclip.util;

/**
 * 时间工具类
 */
public class TimeUtil {

    /**
     * 秒转微秒
     */
    public static long secondsToMicroseconds(double seconds) {
        return (long) (seconds * 1_000_000);
    }

    /**
     * 微秒转秒
     */
    public static double microsecondsToSeconds(long microseconds) {
        return microseconds / 1_000_000.0;
    }

    /**
     * 解析时间字符串，例如 "1.5s" -> 1500000
     */
    public static long parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return 0;
        }
        timeStr = timeStr.trim().toLowerCase();
        // Check 'ms' before 's' since 'ms' also ends with 's'
        if (timeStr.endsWith("ms")) {
            double ms = Double.parseDouble(timeStr.substring(0, timeStr.length() - 2));
            return (long) (ms * 1000);
        }
        if (timeStr.endsWith("s")) {
            double seconds = Double.parseDouble(timeStr.substring(0, timeStr.length() - 1));
            return secondsToMicroseconds(seconds);
        }
        // 默认当作秒处理
        return secondsToMicroseconds(Double.parseDouble(timeStr));
    }

    /**
     * 格式化时间戳用于SRT字幕
     * 格式：00:00:00,000
     */
    public static String formatSrtTimestamp(long microseconds) {
        long ms = microseconds / 1000;
        long hours = ms / 3600000;
        long minutes = (ms % 3600000) / 60000;
        long seconds = (ms % 60000) / 1000;
        long millis = ms % 1000;
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis);
    }
}
