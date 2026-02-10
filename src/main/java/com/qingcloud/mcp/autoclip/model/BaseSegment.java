package com.qingcloud.mcp.autoclip.model;

import java.util.UUID;

/**
 * 片段基类
 */
public abstract class BaseSegment {
    protected String segmentId;
    protected String materialId;
    protected Timerange targetTimerange;
    protected Timerange sourceTimerange;

    public BaseSegment() {
        this.segmentId = UUID.randomUUID().toString().replace("-", "");
    }

    public String getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(String segmentId) {
        this.segmentId = segmentId;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public Timerange getTargetTimerange() {
        return targetTimerange;
    }

    public void setTargetTimerange(Timerange targetTimerange) {
        this.targetTimerange = targetTimerange;
    }

    public Timerange getSourceTimerange() {
        return sourceTimerange;
    }

    public void setSourceTimerange(Timerange sourceTimerange) {
        this.sourceTimerange = sourceTimerange;
    }

    public long getStart() {
        return targetTimerange != null ? targetTimerange.getStart() : 0;
    }

    public long getEnd() {
        return targetTimerange != null ? targetTimerange.getEnd() : 0;
    }

    public boolean overlaps(BaseSegment other) {
        return this.targetTimerange != null && other.targetTimerange != null
                && this.targetTimerange.overlaps(other.targetTimerange);
    }
}
