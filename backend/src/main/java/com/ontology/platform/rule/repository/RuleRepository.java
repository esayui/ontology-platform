package com.ontology.platform.rule.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ontology.platform.rule.entity.RuleDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RuleRepository extends BaseMapper<RuleDefinition> {

    @Select("SELECT * FROM rule_definition WHERE rule_name = #{ruleName} ORDER BY update_time DESC LIMIT 1")
    RuleDefinition findByName(String ruleName);

    @Select("SELECT * FROM rule_definition WHERE status = 'ACTIVE'")
    List<RuleDefinition> findActiveRules();

    @Select("SELECT * FROM rule_definition WHERE rule_name = #{ruleName} ORDER BY version DESC")
    List<RuleDefinition> findVersionsByName(String ruleName);

    @Update("UPDATE rule_definition SET status = #{status} WHERE id = #{id}")
    int updateStatus(String id, String status);
}
