package com.ontology.platform.ontology.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ontology.platform.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ontology_relationship")
public class CapabilityRelationship extends BaseEntity {

    @TableId
    private String id;

    private String sourceIndicatorId;

    private String targetIndicatorId;

    private String relationshipType;

    private String droolsRuleName;

    private Double weight;

    private Integer priority;

    private String influenceDirection;

    private Boolean enabled;
}
