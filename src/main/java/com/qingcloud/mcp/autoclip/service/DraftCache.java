package com.qingcloud.mcp.autoclip.service;

import com.qingcloud.mcp.autoclip.model.ScriptFile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 草稿缓存管理
 * 使用内存缓存存储草稿对象
 */
@Component
public class DraftCache {
    private final Map<String, ScriptFile> cache = new ConcurrentHashMap<>();

    public void put(String draftId, ScriptFile script) {
        cache.put(draftId, script);
    }

    public ScriptFile get(String draftId) {
        return cache.get(draftId);
    }

    public boolean contains(String draftId) {
        return cache.containsKey(draftId);
    }

    public void remove(String draftId) {
        cache.remove(draftId);
    }

    public int size() {
        return cache.size();
    }

    public void clear() {
        cache.clear();
    }
}
