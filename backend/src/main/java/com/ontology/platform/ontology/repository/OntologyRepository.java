package com.ontology.platform.ontology.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ontology.platform.ontology.entity.CapabilityIndicator;
import com.ontology.platform.ontology.entity.CapabilityRelationship;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OntologyRepository {

    @Mapper
    interface IndicatorMapper extends BaseMapper<CapabilityIndicator> {
        @Select("SELECT * FROM capability_indicator WHERE domain = #{domain}")
        List<CapabilityIndicator> findByDomain(String domain);
    }

    @Mapper
    interface RelationshipMapper extends BaseMapper<CapabilityRelationship> {
        @Select("SELECT * FROM ontology_relationship WHERE source_indicator_id = #{indicatorId} OR target_indicator_id = #{indicatorId}")
        List<CapabilityRelationship> findByIndicatorId(String indicatorId);
    }
}
