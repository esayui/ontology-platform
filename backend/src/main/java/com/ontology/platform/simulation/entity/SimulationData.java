package com.ontology.platform.simulation.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ontology.platform.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("simulation_data")
public class SimulationData extends BaseEntity {

    @TableId
    private String id;

    private String indicatorId;

    private Double value;

    private LocalDateTime timestamp;

    private String source;
}
