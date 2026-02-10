package com.qingcloud.mcp.autoclip.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 草稿文件主类
 */
public class ScriptFile {
    private String draftId;
    private int width;
    private int height;
    private int fps;
    private long duration; // 微秒
    private ScriptMaterial materials;
    private Map<String, Track> tracks;
    private long createdAt;
    private long updatedAt;

    public ScriptFile() {
        this.draftId = generateDraftId();
        this.materials = new ScriptMaterial();
        this.tracks = new HashMap<>();
        this.fps = 30;
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
    }

    public ScriptFile(int width, int height) {
        this();
        this.width = width;
        this.height = height;
    }

    private String generateDraftId() {
        long timestamp = Instant.now().getEpochSecond();
        String uuid8 = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return String.format("dfd_cat_%d_%s", timestamp, uuid8);
    }

    public Track addTrack(TrackType type, String name) {
        if (tracks.containsKey(name)) {
            return tracks.get(name);
        }
        Track track = new Track(type, name);
        tracks.put(name, track);
        return track;
    }

    public Track getTrack(String name) {
        return tracks.get(name);
    }

    public void addSegment(BaseSegment segment, String trackName) {
        Track track = tracks.get(trackName);
        if (track != null) {
            track.addSegment(segment);
            updateDuration();
        }
    }

    private void updateDuration() {
        this.duration = tracks.values().stream()
                .mapToLong(Track::getEndTime)
                .max()
                .orElse(0);
        this.updatedAt = Instant.now().toEpochMilli();
    }

    // Getters and Setters
    public String getDraftId() {
        return draftId;
    }

    public void setDraftId(String draftId) {
        this.draftId = draftId;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public ScriptMaterial getMaterials() {
        return materials;
    }

    public void setMaterials(ScriptMaterial materials) {
        this.materials = materials;
    }

    public Map<String, Track> getTracks() {
        return tracks;
    }

    public void setTracks(Map<String, Track> tracks) {
        this.tracks = tracks;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
