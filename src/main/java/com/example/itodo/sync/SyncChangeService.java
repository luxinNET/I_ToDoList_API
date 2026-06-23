package com.example.itodo.sync;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.itodo.sync.dto.SyncChangeResponse;
import com.example.itodo.sync.entity.SyncChange;
import com.example.itodo.sync.mapper.SyncChangeMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SyncChangeService {

    private final SyncChangeMapper syncChangeMapper;

    public SyncChangeService(SyncChangeMapper syncChangeMapper) {
        this.syncChangeMapper = syncChangeMapper;
    }

    public void recordChange(UUID userId, String resourceType, UUID resourceId, String operation) {
        Long version = syncChangeMapper.nextSyncVersion();
        SyncChange change = new SyncChange();
        change.setId(version);
        change.setUserId(userId);
        change.setResourceType(resourceType);
        change.setResourceId(resourceId);
        change.setOperation(operation);
        change.setVersion(version);
        change.setChangedAt(Instant.now());
        syncChangeMapper.insert(change);
    }

    public List<SyncChangeResponse> changesSince(UUID userId, Long sinceVersion, Integer limit) {
        int boundedLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 500));
        List<SyncChange> changes = syncChangeMapper.selectList(new LambdaQueryWrapper<SyncChange>()
                .eq(SyncChange::getUserId, userId)
                .gt(SyncChange::getVersion, sinceVersion == null ? 0L : sinceVersion)
                .orderByAsc(SyncChange::getVersion)
                .last("LIMIT " + boundedLimit));
        return changes.stream()
                .map(change -> new SyncChangeResponse(
                        change.getVersion(),
                        change.getResourceType(),
                        change.getResourceId(),
                        change.getOperation(),
                        change.getChangedAt()))
                .toList();
    }

    public Long currentVersion(UUID userId) {
        SyncChange change = syncChangeMapper.selectOne(new LambdaQueryWrapper<SyncChange>()
                .select(SyncChange::getVersion)
                .eq(SyncChange::getUserId, userId)
                .orderByDesc(SyncChange::getVersion)
                .last("LIMIT 1"));
        return change == null || change.getVersion() == null ? 0L : change.getVersion();
    }
}
