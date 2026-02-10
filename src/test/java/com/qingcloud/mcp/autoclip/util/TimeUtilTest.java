package com.qingcloud.mcp.autoclip.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TimeUtil 单元测试
 */
class TimeUtilTest {

    @Test
    void testSecondsToMicroseconds() {
        assertEquals(1_000_000, TimeUtil.secondsToMicroseconds(1.0));
        assertEquals(1_500_000, TimeUtil.secondsToMicroseconds(1.5));
        assertEquals(500_000, TimeUtil.secondsToMicroseconds(0.5));
    }

    @Test
    void testMicrosecondsToSeconds() {
        assertEquals(1.0, TimeUtil.microsecondsToSeconds(1_000_000), 0.001);
        assertEquals(1.5, TimeUtil.microsecondsToSeconds(1_500_000), 0.001);
    }

    @Test
    void testParseTime() {
        assertEquals(1_000_000, TimeUtil.parseTime("1s"));
        assertEquals(1_500_000, TimeUtil.parseTime("1.5s"));
        assertEquals(500_000, TimeUtil.parseTime("500ms"));
        assertEquals(2_000_000, TimeUtil.parseTime("2"));
    }

    @Test
    void testFormatSrtTimestamp() {
        assertEquals("00:00:01,000", TimeUtil.formatSrtTimestamp(1_000_000));
        assertEquals("00:01:30,500", TimeUtil.formatSrtTimestamp(90_500_000));
        assertEquals("01:00:00,000", TimeUtil.formatSrtTimestamp(3_600_000_000L));
    }
}
