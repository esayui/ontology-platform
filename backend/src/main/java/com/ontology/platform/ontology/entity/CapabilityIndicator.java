package com.ontology.platform.ontology.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ontology.platform.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("capability_indicator")
public class CapabilityIndicator extends BaseEntity {

    @TableId
    private String id;

    private String iri;

    private String name;

    private String domain;

    private String category;

    private String description;

    private String unit;

    private String dataType;

    private Double thresholdMin;

    private Double thresholdMax;
}
