package com.qingcloud.mcp.xhs.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * 图片下载处理器
 * 支持URL图片下载和本地路径处理
 */
@Component
public class ImageDownloader {

    private static final Logger logger = LoggerFactory.getLogger(ImageDownloader.class);

    private final HttpClient httpClient;
    private final Path downloadDir;

    public ImageDownloader() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        // 创建临时下载目录
        try {
            this.downloadDir = Files.createTempDirectory("xhs_images_");
            logger.info("Image download directory: {}", downloadDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp directory for images", e);
        }
    }

    /**
     * 处理图片列表，返回本地文件路径
     * 支持两种输入格式：
     * 1. URL格式 (http/https开头) - 自动下载到本地
     * 2. 本地文件路径 - 直接使用
     */
    public List<String> processImages(List<String> images) throws IOException {
        List<String> localPaths = new ArrayList<>();

        for (String image : images) {
            if (isImageUrl(image)) {
                // URL图片：下载到本地
                String localPath = downloadImage(image);
                localPaths.add(localPath);
            } else {
                // 验证本地文件是否存在
                Path path = Path.of(image);
                if (!Files.exists(path)) {
                    throw new IOException("Image file not found: " + image);
                }
                localPaths.add(image);
            }
        }

        if (localPaths.isEmpty()) {
            throw new IOException("No valid images found");
        }

        return localPaths;
    }

    /**
     * 下载图片并返回本地路径
     */
    public String downloadImage(String imageUrl) throws IOException {
        logger.info("Downloading image: {}", imageUrl);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new IOException("Download failed with status: " + response.statusCode());
            }

            // 生成唯一文件名
            String fileName = generateFileName(imageUrl);
            Path targetPath = downloadDir.resolve(fileName);

            // 如果文件已存在，直接返回
            if (Files.exists(targetPath)) {
                logger.info("Image already cached: {}", targetPath);
                return targetPath.toString();
            }

            // 保存文件
            try (InputStream inputStream = response.body()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            logger.info("Image downloaded to: {}", targetPath);
            return targetPath.toString();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted", e);
        }
    }

    /**
     * 判断是否为图片URL
     */
    public boolean isImageUrl(String path) {
        if (path == null) {
            return false;
        }
        String lower = path.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    /**
     * 生成唯一文件名
     */
    private String generateFileName(String imageUrl) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(imageUrl.getBytes());
            String hashStr = HexFormat.of().formatHex(hash);
            String shortHash = hashStr.substring(0, 16);
            long timestamp = System.currentTimeMillis();

            // 尝试获取原始扩展名
            String extension = getExtensionFromUrl(imageUrl);

            return String.format("img_%s_%d%s", shortHash, timestamp, extension);
        } catch (NoSuchAlgorithmException e) {
            // 备用方案
            long timestamp = System.currentTimeMillis();
            return String.format("img_%d.jpg", timestamp);
        }
    }

    /**
     * 从URL获取文件扩展名
     */
    private String getExtensionFromUrl(String url) {
        try {
            String path = URI.create(url).getPath();
            int dotIndex = path.lastIndexOf('.');
            if (dotIndex > 0) {
                String ext = path.substring(dotIndex).toLowerCase();
                if (ext.matches("\\.(jpg|jpeg|png|gif|webp|bmp)")) {
                    return ext;
                }
            }
        } catch (Exception ignored) {
        }
        return ".jpg"; // 默认扩展名
    }
}
