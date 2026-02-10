package com.qingcloud.mcp.autoclip.util;

import com.qingcloud.mcp.autoclip.util.SrtParser.SubtitleEntry;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SrtParser 单元测试
 */
class SrtParserTest {

    @Test
    void testParseSrt() throws Exception {
        String srt = """
                1
                00:00:01,000 --> 00:00:03,500
                Hello World

                2
                00:00:04,000 --> 00:00:06,000
                Second line
                """;

        List<SubtitleEntry> entries = SrtParser.parse(srt);

        assertEquals(2, entries.size());

        assertEquals(1, entries.get(0).index);
        assertEquals(1_000_000, entries.get(0).startTime);
        assertEquals(3_500_000, entries.get(0).endTime);
        assertEquals("Hello World", entries.get(0).text);

        assertEquals(2, entries.get(1).index);
        assertEquals("Second line", entries.get(1).text);
    }

    @Test
    void testParseMultiLineSubtitle() throws Exception {
        String srt = """
                1
                00:00:01,000 --> 00:00:05,000
                Line one
                Line two
                """;

        List<SubtitleEntry> entries = SrtParser.parse(srt);

        assertEquals(1, entries.size());
        assertEquals("Line one\nLine two", entries.get(0).text);
    }

    @Test
    void testParseEmptySrt() throws Exception {
        List<SubtitleEntry> entries = SrtParser.parse("");
        assertTrue(entries.isEmpty());
    }
}
