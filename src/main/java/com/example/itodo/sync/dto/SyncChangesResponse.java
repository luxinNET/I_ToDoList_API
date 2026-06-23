package com.example.itodo.sync.dto;

import java.util.List;

public record SyncChangesResponse(
        Long sinceVersion,
        Long currentVersion,
        List<SyncChangeResponse> changes
) {
}
