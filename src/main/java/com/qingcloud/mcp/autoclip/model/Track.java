package com.qingcloud.mcp.autoclip.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 轨道类
 */
public class Track {
    private String trackId;
    private String name;
    private TrackType trackType;
    private int renderIndex;
    private boolean mute;
    private List<BaseSegment> segments;

    public Track() {
        this.trackId = UUID.randomUUID().toString().replace("-", "");
        this.segments = new ArrayList<>();
    }

    public Track(TrackType type, String name) {
        this();
        this.trackType = type;
        this.name = name;
        this.renderIndex = type.getRenderIndex();
    }

    public void addSegment(BaseSegment segment) {
        segments.add(segment);
    }

    public long getEndTime() {
        return segments.stream()
                .mapToLong(BaseSegment::getEnd)
                .max()
                .orElse(0);
    }

    // Getters and Setters
    public String getTrackId() {
        return trackId;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TrackType getTrackType() {
        return trackType;
    }

    public void setTrackType(TrackType trackType) {
        this.trackType = trackType;
    }

    public int getRenderIndex() {
        return renderIndex;
    }

    public void setRenderIndex(int renderIndex) {
        this.renderIndex = renderIndex;
    }

    public boolean isMute() {
        return mute;
    }

    public void setMute(boolean mute) {
        this.mute = mute;
    }

    public List<BaseSegment> getSegments() {
        return segments;
    }

    public void setSegments(List<BaseSegment> segments) {
        this.segments = segments;
    }
}
