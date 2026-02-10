package com.qingcloud.mcp.autoclip.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ScriptFile 单元测试
 */
class ScriptFileTest {

    @Test
    void testCreateDraft() {
        ScriptFile script = new ScriptFile(1080, 1920);

        assertNotNull(script.getDraftId());
        assertTrue(script.getDraftId().startsWith("dfd_cat_"));
        assertEquals(1080, script.getWidth());
        assertEquals(1920, script.getHeight());
        assertEquals(30, script.getFps());
        assertNotNull(script.getMaterials());
        assertNotNull(script.getTracks());
    }

    @Test
    void testAddTrack() {
        ScriptFile script = new ScriptFile(1080, 1920);

        Track videoTrack = script.addTrack(TrackType.VIDEO, "main");
        Track audioTrack = script.addTrack(TrackType.AUDIO, "audio");

        assertNotNull(videoTrack);
        assertEquals("main", videoTrack.getName());
        assertEquals(TrackType.VIDEO, videoTrack.getTrackType());

        assertEquals(2, script.getTracks().size());
        assertSame(videoTrack, script.getTrack("main"));
    }

    @Test
    void testDuplicateTrack() {
        ScriptFile script = new ScriptFile(1080, 1920);

        Track track1 = script.addTrack(TrackType.VIDEO, "main");
        Track track2 = script.addTrack(TrackType.VIDEO, "main");

        assertSame(track1, track2);
        assertEquals(1, script.getTracks().size());
    }
}
