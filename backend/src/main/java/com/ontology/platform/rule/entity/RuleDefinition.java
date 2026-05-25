package com.ontology.platform.rule.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ontology.platform.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rule_definition")
public class RuleDefinition extends BaseEntity {

    @TableId
    private String id;

    private String ruleName;

    private String drlContent;

    private String version;

    private String status;

    private String tags;

    private String description;

    private LocalDateTime publishTime;
}
