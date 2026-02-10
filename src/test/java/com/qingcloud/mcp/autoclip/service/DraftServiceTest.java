package com.qingcloud.mcp.autoclip.service;

import com.qingcloud.mcp.autoclip.model.ScriptFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DraftService 单元测试
 */
class DraftServiceTest {

    private DraftService draftService;
    private DraftCache draftCache;

    @BeforeEach
    void setUp() {
        draftCache = new DraftCache();
        draftService = new DraftService();
        // 手动注入 DraftCache
        try {
            var field = DraftService.class.getDeclaredField("draftCache");
            field.setAccessible(true);
            field.set(draftService, draftCache);
        } catch (Exception e) {
            fail("Failed to inject DraftCache");
        }
    }

    @Test
    void testCreateDraft() {
        ScriptFile script = draftService.createDraft(1080, 1920);

        assertNotNull(script);
        assertNotNull(script.getDraftId());
        assertEquals(1080, script.getWidth());
        assertEquals(1920, script.getHeight());

        // 验证缓存
        assertTrue(draftCache.contains(script.getDraftId()));
    }

    @Test
    void testGetDraft() {
        ScriptFile created = draftService.createDraft(1080, 1920);
        ScriptFile fetched = draftService.getDraft(created.getDraftId());

        assertSame(created, fetched);
    }

    @Test
    void testAddVideo() {
        Map<String, Object> result = draftService.addVideo(
                null, "http://test.com/video.mp4",
                0, 10, 0, 1080, 1920, "main", 1.0, 1.0);

        assertNotNull(result);
        assertNotNull(result.get("draftId"));
        assertNotNull(result.get("segmentId"));
    }

    @Test
    void testAddAudio() {
        ScriptFile script = draftService.createDraft(1080, 1920);

        Map<String, Object> result = draftService.addAudio(
                script.getDraftId(), "http://test.com/audio.mp3",
                0, 30, 0, 1080, 1920, "audio", 1.0, 0.8);

        assertEquals(script.getDraftId(), result.get("draftId"));
        assertNotNull(result.get("segmentId"));
    }

    @Test
    void testAddText() {
        ScriptFile script = draftService.createDraft(1080, 1920);

        Map<String, Object> result = draftService.addText(
                script.getDraftId(), "Test Text",
                0, 5, "#ffffff", 8.0, 0, -0.8,
                1080, 1920, "text");

        assertEquals(script.getDraftId(), result.get("draftId"));
    }

    @Test
    void testDeleteDraft() {
        ScriptFile script = draftService.createDraft(1080, 1920);
        String draftId = script.getDraftId();

        assertTrue(draftCache.contains(draftId));
        assertTrue(draftService.deleteDraft(draftId));
        assertFalse(draftCache.contains(draftId));
    }

    @Test
    void testDeleteNonExistentDraft() {
        assertFalse(draftService.deleteDraft("non_existent_id"));
    }

    @Test
    void testGetDraftInfo() {
        ScriptFile script = draftService.createDraft(1080, 1920);
        draftService.addVideo(script.getDraftId(), "test.mp4", 0, 5, 0, 1080, 1920, "main", 1.0, 1.0);

        Map<String, Object> info = draftService.getDraftInfo(script.getDraftId());

        assertNotNull(info);
        assertEquals(1080, info.get("width"));
        assertEquals(1920, info.get("height"));
        assertEquals(1, info.get("videoCount"));
    }
}
