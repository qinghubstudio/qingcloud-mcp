package com.qingcloud.mcp.autoclip.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Timerange 单元测试
 */
class TimerangeTest {

    @Test
    void testFromSeconds() {
        Timerange range = Timerange.fromSeconds(1.5, 3.0);

        assertEquals(1_500_000, range.getStart());
        assertEquals(3_000_000, range.getDuration());
        assertEquals(4_500_000, range.getEnd());
    }

    @Test
    void testOverlaps() {
        Timerange range1 = Timerange.fromSeconds(0, 5);
        Timerange range2 = Timerange.fromSeconds(3, 5);
        Timerange range3 = Timerange.fromSeconds(6, 3);

        assertTrue(range1.overlaps(range2));
        assertTrue(range2.overlaps(range1));
        assertFalse(range1.overlaps(range3));
    }

    @Test
    void testGetEnd() {
        Timerange range = new Timerange(1_000_000, 2_000_000);
        assertEquals(3_000_000, range.getEnd());
    }
}
