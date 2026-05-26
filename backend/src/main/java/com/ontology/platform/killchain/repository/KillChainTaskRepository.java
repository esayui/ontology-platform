package com.ontology.platform.killchain.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ontology.platform.killchain.entity.KillChainTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KillChainTaskRepository extends BaseMapper<KillChainTask> {

    @Select("SELECT * FROM kill_chain_task WHERE status = 'ACTIVE'")
    List<KillChainTask> findActiveTasks();
}
