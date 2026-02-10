package com.qingcloud.mcp.autoclip.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 素材管理类
 */
public class ScriptMaterial {
    private List<VideoMaterial> videos;
    private List<AudioMaterial> audios;
    private List<Object> texts;
    private List<Object> stickers;

    public ScriptMaterial() {
        this.videos = new ArrayList<>();
        this.audios = new ArrayList<>();
        this.texts = new ArrayList<>();
        this.stickers = new ArrayList<>();
    }

    public void addVideo(VideoMaterial video) {
        videos.add(video);
    }

    public void addAudio(AudioMaterial audio) {
        audios.add(audio);
    }

    public VideoMaterial findVideoById(String materialId) {
        return videos.stream()
                .filter(v -> v.getMaterialId().equals(materialId))
                .findFirst()
                .orElse(null);
    }

    public AudioMaterial findAudioById(String materialId) {
        return audios.stream()
                .filter(a -> a.getMaterialId().equals(materialId))
                .findFirst()
                .orElse(null);
    }

    // Getters and Setters
    public List<VideoMaterial> getVideos() {
        return videos;
    }

    public void setVideos(List<VideoMaterial> videos) {
        this.videos = videos;
    }

    public List<AudioMaterial> getAudios() {
        return audios;
    }

    public void setAudios(List<AudioMaterial> audios) {
        this.audios = audios;
    }

    public List<Object> getTexts() {
        return texts;
    }

    public void setTexts(List<Object> texts) {
        this.texts = texts;
    }

    public List<Object> getStickers() {
        return stickers;
    }

    public void setStickers(List<Object> stickers) {
        this.stickers = stickers;
    }
}
