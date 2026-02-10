package com.qingcloud.mcp.autoclip.model;

/**
 * 时间范围类
 * 所有时间使用微秒(microseconds)作为单位
 */
public class Timerange {
    private long start; // 开始时间（微秒）
    private long duration; // 持续时间（微秒）

    public Timerange() {
    }

    public Timerange(long start, long duration) {
        this.start = start;
        this.duration = duration;
    }

    /**
     * 从秒创建时间范围
     */
    public static Timerange fromSeconds(double startSec, double durationSec) {
        return new Timerange(
                (long) (startSec * 1_000_000),
                (long) (durationSec * 1_000_000));
    }

    public long getEnd() {
        return start + duration;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    /**
     * 检查是否与另一个时间范围重叠
     */
    public boolean overlaps(Timerange other) {
        return this.start < other.getEnd() && other.start < this.getEnd();
    }
}
