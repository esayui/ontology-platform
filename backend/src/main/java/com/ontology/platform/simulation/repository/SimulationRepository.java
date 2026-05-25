package com.ontology.platform.simulation.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ontology.platform.simulation.entity.SimulationData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SimulationRepository extends BaseMapper<SimulationData> {

    @Select("SELECT * FROM simulation_data WHERE indicator_id = #{indicatorId} ORDER BY timestamp DESC")
    List<SimulationData> findByIndicatorId(String indicatorId);

    @Select("SELECT * FROM simulation_data ORDER BY timestamp DESC LIMIT #{limit}")
    List<SimulationData> findLatest(int limit);
}
