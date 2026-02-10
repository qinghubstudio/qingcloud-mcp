package com.qingcloud.mcp.autoclip;

import com.qingcloud.mcp.autoclip.model.*;
import com.qingcloud.mcp.autoclip.service.DraftCache;
import com.qingcloud.mcp.autoclip.service.DraftExportService;
import com.qingcloud.mcp.autoclip.service.DraftService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 真实草稿生成测试
 * 使用 docs/assets 中的素材生成剪映可导入的草稿
 * 
 * 运行: mvn test -Dtest="RealDraftGeneratorTest"
 */
class RealDraftGeneratorTest {

    @Test
    void generateRealDraft() throws Exception {
        // 素材路径 (使用绝对路径)
        String basePath = "c:/workspace/jinghui/backend/common/qingcloud-mcp/docs/assets";
        String image1 = basePath + "/1.png";
        String image2 = basePath + "/2.png";
        String audio = basePath + "/4.mp3";
        String video = basePath + "/5.mp4";

        // 输出目录
        String outputDir = "c:/workspace/jinghui/backend/common/qingcloud-mcp/output/drafts";
        Files.createDirectories(Path.of(outputDir));

        System.out.println("=== 剪映草稿生成器 ===");
        System.out.println("素材目录: " + basePath);
        System.out.println("输出目录: " + outputDir);

        // 初始化服务
        DraftCache draftCache = new DraftCache();

        DraftService draftService = new DraftService();
        var draftCacheField = DraftService.class.getDeclaredField("draftCache");
        draftCacheField.setAccessible(true);
        draftCacheField.set(draftService, draftCache);

        DraftExportService exportService = new DraftExportService();
        var exportCacheField = DraftExportService.class.getDeclaredField("draftCache");
        exportCacheField.setAccessible(true);
        exportCacheField.set(exportService, draftCache);

        // 1. 创建草稿 (1080x1920 竖屏)
        System.out.println("\n1. 创建草稿 (1080x1920)...");
        ScriptFile script = draftService.createDraft(1080, 1920);
        String draftId = script.getDraftId();
        System.out.println("   草稿ID: " + draftId);

        // 2. 添加视频 (0-10秒)
        System.out.println("2. 添加视频: 5.mp4 (0-10秒)...");
        Map<String, Object> videoResult = draftService.addVideo(
                draftId, video,
                0, 10, 0, // 源0-10秒，目标从0开始
                1080, 1920, "main",
                1.0, 1.0 // 正常速度和音量
        );
        System.out.println("   片段ID: " + videoResult.get("segmentId"));

        // 3. 添加背景音乐 (0-10秒，音量50%)
        System.out.println("3. 添加背景音乐: 4.mp3...");
        Map<String, Object> audioResult = draftService.addAudio(
                draftId, audio,
                0, 10, 0,
                1080, 1920, "bgm",
                1.0, 0.5 // 音量50%
        );
        System.out.println("   片段ID: " + audioResult.get("segmentId"));

        // 4. 添加图片1 (10-13秒)
        System.out.println("4. 添加图片: 1.png (10-13秒)...");
        Map<String, Object> img1Result = draftService.addImage(
                draftId, image1,
                10, 13,
                1080, 1920, "overlay");
        System.out.println("   片段ID: " + img1Result.get("segmentId"));

        // 5. 添加图片2 (13-16秒)
        System.out.println("5. 添加图片: 2.png (13-16秒)...");
        Map<String, Object> img2Result = draftService.addImage(
                draftId, image2,
                13, 16,
                1080, 1920, "overlay");
        System.out.println("   片段ID: " + img2Result.get("segmentId"));

        // 6. 添加标题文字 (0-5秒)
        System.out.println("6. 添加标题文字 (0-5秒)...");
        draftService.addText(
                draftId, "Autoclip 测试视频",
                0, 5,
                "#ffffff", 12.0, 0, 0.7,
                1080, 1920, "title");

        // 7. 添加副标题 (5-10秒)
        System.out.println("7. 添加副标题 (5-10秒)...");
        draftService.addText(
                draftId, "自动生成的剪映草稿",
                5, 10,
                "#ffff00", 8.0, 0, -0.8,
                1080, 1920, "subtitle");

        // 8. 获取草稿信息
        System.out.println("\n8. 草稿概览:");
        Map<String, Object> info = draftService.getDraftInfo(draftId);
        System.out.println("   尺寸: " + info.get("width") + "x" + info.get("height"));
        System.out.println("   时长: " + info.get("duration") + " 秒");
        System.out.println("   视频数: " + info.get("videoCount"));
        System.out.println("   音频数: " + info.get("audioCount"));
        System.out.println("   轨道数: " + info.get("trackCount"));

        // 9. 保存草稿
        System.out.println("\n9. 保存草稿...");
        Map<String, Object> saveResult = exportService.saveDraft(draftId, outputDir);
        String savedPath = (String) saveResult.get("draftPath");

        System.out.println("\n=== 完成 ===");
        System.out.println("草稿已保存到: " + savedPath);
        System.out.println("\n导入剪映步骤:");
        System.out.println("1. 将整个草稿文件夹复制到剪映草稿目录");
        System.out.println("   Windows: %USERPROFILE%\\Documents\\JianyingPro Drafts\\");
        System.out.println("2. 重启剪映");
        System.out.println("3. 草稿应该出现在\"我的草稿\"中");
    }
}
