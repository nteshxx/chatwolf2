package com.chatwolf.storage.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StorageMetricsService {

    private final MeterRegistry meterRegistry;

    public void recordUpload(long fileSize, String contentType, boolean success, long durationMs) {

        Counter.builder("storage.uploads.total")
                .tag("type", getTypeCategory(contentType))
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .increment();

        meterRegistry
                .summary("storage.upload.size", "type", getTypeCategory(contentType))
                .record(fileSize);

        meterRegistry
                .timer("storage.upload.duration", "type", getTypeCategory(contentType))
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordDownload(String objectKey, boolean success) {
        Counter.builder("storage.downloads.total")
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .increment();
    }

    public void recordDeletion(String objectKey, boolean success) {
        Counter.builder("storage.deletions.total")
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .increment();
    }

    private String getTypeCategory(String contentType) {
        if (contentType == null) return "unknown";
        if (contentType.startsWith("image/")) return "image";
        if (contentType.startsWith("video/")) return "video";
        if (contentType.startsWith("application/")) return "document";
        return "other";
    }
}
