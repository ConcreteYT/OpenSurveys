package com.opensurveys.security;

import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window limiter keyed by caller. Stale windows are dropped so the map stays bounded.
 */
@Component
public class ClientRateLimiter {

    private static final int CLEANUP_THRESHOLD = 4_096;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public boolean allow(String key, int limit, long windowMs) {
        long now = System.currentTimeMillis();
        Window window = windows.computeIfAbsent(key, ignored -> new Window(now));
        boolean allowed;
        synchronized (window) {
            if (now - window.start >= windowMs) {
                window.start = now;
                window.count.set(0);
            }
            allowed = window.count.incrementAndGet() <= limit;
        }
        if (windows.size() > CLEANUP_THRESHOLD) {
            cleanup(now);
        }
        return allowed;
    }

    private void cleanup(long now) {
        Iterator<Map.Entry<String, Window>> iterator = windows.entrySet().iterator();
        while (iterator.hasNext()) {
            Window window = iterator.next().getValue();
            if (now - window.start > 3_600_000L) {
                iterator.remove();
            }
        }
    }

    private static final class Window {
        private long start;
        private final AtomicInteger count = new AtomicInteger();

        private Window(long start) {
            this.start = start;
        }
    }
}
