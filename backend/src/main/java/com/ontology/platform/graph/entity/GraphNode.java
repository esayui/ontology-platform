package com.ontology.platform.graph.entity;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class GraphNode {

    private String id;
    private String iri;
    private String name;
    private String domain;
    private String category;
    private String unit;
    private List<String> labels;
    private Map<String, Object> properties;
}
