package com.qingcloud.mcp.autoclip.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SRT 字幕解析工具
 */
public class SrtParser {

    // SRT 时间戳格式: 00:00:00,000
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})\\s*-->\\s*(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})");

    /**
     * 字幕条目
     */
    public static class SubtitleEntry {
        public int index;
        public long startTime; // 微秒
        public long endTime; // 微秒
        public String text;

        public SubtitleEntry(int index, long startTime, long endTime, String text) {
            this.index = index;
            this.startTime = startTime;
            this.endTime = endTime;
            this.text = text;
        }
    }

    /**
     * 解析 SRT 内容
     */
    public static List<SubtitleEntry> parse(String srtContent) throws IOException {
        List<SubtitleEntry> entries = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new StringReader(srtContent));

        String line;
        int index = 0;
        long startTime = 0;
        long endTime = 0;
        StringBuilder textBuilder = new StringBuilder();
        int state = 0; // 0=index, 1=time, 2=text

        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (line.isEmpty()) {
                // 空行表示一条字幕结束
                if (textBuilder.length() > 0) {
                    entries.add(new SubtitleEntry(index, startTime, endTime, textBuilder.toString().trim()));
                    textBuilder = new StringBuilder();
                }
                state = 0;
                continue;
            }

            switch (state) {
                case 0: // 序号行
                    try {
                        index = Integer.parseInt(line);
                        state = 1;
                    } catch (NumberFormatException e) {
                        // 可能是没有序号的格式，尝试按时间解析
                        Matcher m = TIME_PATTERN.matcher(line);
                        if (m.find()) {
                            startTime = parseTimestamp(m, 1);
                            endTime = parseTimestamp(m, 5);
                            state = 2;
                        }
                    }
                    break;
                case 1: // 时间行
                    Matcher matcher = TIME_PATTERN.matcher(line);
                    if (matcher.find()) {
                        startTime = parseTimestamp(matcher, 1);
                        endTime = parseTimestamp(matcher, 5);
                        state = 2;
                    }
                    break;
                case 2: // 文本行
                    if (textBuilder.length() > 0) {
                        textBuilder.append("\n");
                    }
                    textBuilder.append(line);
                    break;
            }
        }

        // 处理最后一条字幕
        if (textBuilder.length() > 0) {
            entries.add(new SubtitleEntry(index, startTime, endTime, textBuilder.toString().trim()));
        }

        return entries;
    }

    /**
     * 解析时间戳为微秒
     */
    private static long parseTimestamp(Matcher m, int startGroup) {
        int hours = Integer.parseInt(m.group(startGroup));
        int minutes = Integer.parseInt(m.group(startGroup + 1));
        int seconds = Integer.parseInt(m.group(startGroup + 2));
        int millis = Integer.parseInt(m.group(startGroup + 3));

        return ((hours * 3600L + minutes * 60L + seconds) * 1000L + millis) * 1000L;
    }
}
