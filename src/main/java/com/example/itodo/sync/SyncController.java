package com.example.itodo.sync;

import com.example.itodo.common.api.ApiResponse;
import com.example.itodo.security.CurrentUser;
import com.example.itodo.sync.dto.SyncBootstrapResponse;
import com.example.itodo.sync.dto.SyncChangesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Sync")
@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @Operation(summary = "同步全量快照")
    @GetMapping("/bootstrap")
    ApiResponse<SyncBootstrapResponse> bootstrap(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(syncService.bootstrap(currentUser.id()));
    }

    @Operation(summary = "查询增量变更")
    @GetMapping("/changes")
    ApiResponse<SyncChangesResponse> changes(@AuthenticationPrincipal CurrentUser currentUser,
                                             @RequestParam(required = false) Long sinceVersion,
                                             @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(syncService.changes(currentUser.id(), sinceVersion, limit));
    }
}
