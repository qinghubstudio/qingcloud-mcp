package com.qingcloud.mcp.autoclip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingcloud.mcp.autoclip.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DraftExportService 完整测试
 * 测试保存草稿的完整流程
 */
class DraftExportServiceTest {

    private DraftService draftService;
    private DraftExportService exportService;
    private DraftCache draftCache;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        draftCache = new DraftCache();

        // 设置 DraftService
        draftService = new DraftService();
        var draftCacheField = DraftService.class.getDeclaredField("draftCache");
        draftCacheField.setAccessible(true);
        draftCacheField.set(draftService, draftCache);

        // 设置 DraftExportService
        exportService = new DraftExportService();
        var exportCacheField = DraftExportService.class.getDeclaredField("draftCache");
        exportCacheField.setAccessible(true);
        exportCacheField.set(exportService, draftCache);

        var draftsDirField = DraftExportService.class.getDeclaredField("draftsDir");
        draftsDirField.setAccessible(true);
        draftsDirField.set(exportService, tempDir.toString());
    }

    @Test
    void testSaveDraft_BasicDraft() throws IOException {
        // 1. 创建草稿
        ScriptFile script = draftService.createDraft(1080, 1920);
        String draftId = script.getDraftId();

        // 2. 保存草稿
        Map<String, Object> result = exportService.saveDraft(draftId, null);

        // 3. 验证结果
        assertNotNull(result);
        assertEquals(draftId, result.get("draftId"));

        // 4. 验证文件结构
        Path draftFolder = Path.of(result.get("draftPath").toString());
        assertTrue(Files.exists(draftFolder));
        assertTrue(Files.exists(draftFolder.resolve("draft_info.json")));
        assertTrue(Files.exists(draftFolder.resolve("draft_meta_info.json")));
        assertTrue(Files.exists(draftFolder.resolve("assets")));
    }

    @Test
    void testSaveDraft_WithVideo() throws IOException {
        // 1. 创建草稿并添加视频
        Map<String, Object> addResult = draftService.addVideo(
                null, "http://example.com/video.mp4",
                0, 10, 0, 1080, 1920, "main", 1.0, 1.0);
        String draftId = (String) addResult.get("draftId");

        // 2. 保存草稿
        Map<String, Object> result = exportService.saveDraft(draftId, null);

        // 3. 验证 draft_info.json 内容
        Path draftInfoPath = Path.of(result.get("draftPath").toString()).resolve("draft_info.json");
        String content = Files.readString(draftInfoPath);

        assertTrue(content.contains("\"width\" : 1080"));
        assertTrue(content.contains("\"height\" : 1920"));
        assertTrue(content.contains("\"type\" : \"video\""));
        assertTrue(content.contains("http://example.com/video.mp4"));
    }

    @Test
    void testSaveDraft_WithMultipleMedia() throws IOException {
        // 1. 创建草稿
        ScriptFile script = draftService.createDraft(1080, 1920);
        String draftId = script.getDraftId();

        // 2. 添加视频
        draftService.addVideo(draftId, "http://example.com/video1.mp4",
                0, 5, 0, 1080, 1920, "main", 1.0, 1.0);
        draftService.addVideo(draftId, "http://example.com/video2.mp4",
                0, 5, 5, 1080, 1920, "main", 1.0, 1.0);

        // 3. 添加音频
        draftService.addAudio(draftId, "http://example.com/bgm.mp3",
                0, 10, 0, 1080, 1920, "audio", 1.0, 0.5);

        // 4. 添加图片
        draftService.addImage(draftId, "http://example.com/image.jpg",
                10, 13, 1080, 1920, "overlay");

        // 5. 添加文字
        draftService.addText(draftId, "Hello World",
                0, 3, "#ffffff", 8.0, 0, -0.8, 1080, 1920, "text");

        // 6. 保存草稿
        Map<String, Object> result = exportService.saveDraft(draftId, null);

        // 7. 验证文件内容
        Path draftInfoPath = Path.of(result.get("draftPath").toString()).resolve("draft_info.json");
        String content = Files.readString(draftInfoPath);

        // 验证包含所有素材
        assertTrue(content.contains("video1.mp4"));
        assertTrue(content.contains("video2.mp4"));
        assertTrue(content.contains("bgm.mp3"));
        assertTrue(content.contains("image.jpg"));

        // 解析JSON验证结构
        ObjectMapper mapper = new ObjectMapper();
        Map<?, ?> draftInfo = mapper.readValue(content, Map.class);

        Map<?, ?> canvasConfig = (Map<?, ?>) draftInfo.get("canvas_config");
        assertNotNull(canvasConfig);
        assertEquals(1080, canvasConfig.get("width"));
        assertEquals(1920, canvasConfig.get("height"));
        assertNotNull(draftInfo.get("tracks"));
        assertNotNull(draftInfo.get("materials"));
    }

    @Test
    void testSaveDraft_CustomOutputFolder() throws IOException {
        // 1. 创建自定义输出目录
        Path customDir = tempDir.resolve("custom_output");
        Files.createDirectories(customDir);

        // 2. 创建草稿
        ScriptFile script = draftService.createDraft(720, 1280);
        String draftId = script.getDraftId();

        // 3. 保存到自定义目录
        Map<String, Object> result = exportService.saveDraft(draftId, customDir.toString());

        // 4. 验证保存位置
        String savedPath = result.get("draftPath").toString();
        assertTrue(savedPath.contains("custom_output"));
        assertTrue(Files.exists(Path.of(savedPath)));
    }

    @Test
    void testSaveDraft_MetaInfo() throws IOException {
        // 1. 创建草稿并添加内容
        ScriptFile script = draftService.createDraft(1080, 1920);
        String draftId = script.getDraftId();
        draftService.addVideo(draftId, "http://test.com/v.mp4",
                0, 5, 0, 1080, 1920, "main", 1.0, 1.0);

        // 2. 保存草稿
        Map<String, Object> result = exportService.saveDraft(draftId, null);

        // 3. 验证 meta_info 文件
        Path metaInfoPath = Path.of(result.get("draftPath").toString()).resolve("draft_meta_info.json");
        String content = Files.readString(metaInfoPath);

        assertTrue(content.contains("\"draft_id\""));
        assertTrue(content.contains(draftId));
        assertTrue(content.contains("\"draft_name\""));
        assertTrue(content.contains("\"tm_draft_create\""));
    }

    @Test
    void testSaveDraft_NonExistentDraft() {
        // 保存不存在的草稿应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            exportService.saveDraft("non_existent_id", null);
        });
    }
}
