package com.example.itodo.sync.dto;

import java.time.Instant;
import java.util.UUID;

public record SyncChangeResponse(
        Long version,
        String resourceType,
        UUID resourceId,
        String operation,
        Instant changedAt
) {
}
