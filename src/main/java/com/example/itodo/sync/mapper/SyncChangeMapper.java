package com.example.itodo.sync.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.itodo.sync.entity.SyncChange;
import org.apache.ibatis.annotations.Select;

public interface SyncChangeMapper extends BaseMapper<SyncChange> {

    @Select("SELECT nextval('sync_changes_id_seq')")
    Long nextSyncVersion();
}
