package com.ontology.platform.killchain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ontology.platform.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kill_chain_task")
public class KillChainTask extends BaseEntity {

    @TableId
    private String id;

    private String name;

    private String description;

    /** JSON: { nodes: [], edges: [], metadata: {} } */
    private String modelData;

    private String status;  // DRAFT, ACTIVE, ARCHIVED
}
