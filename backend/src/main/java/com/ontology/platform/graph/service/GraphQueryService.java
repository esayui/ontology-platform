package com.ontology.platform.graph.service;

import com.ontology.platform.graph.entity.GraphNode;
import com.ontology.platform.graph.repository.GraphRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GraphQueryService {

    private final GraphRepository graphRepository;

    public List<GraphNode> getNeighbors(String nodeId) {
        return graphRepository.findNeighbors(nodeId);
    }

    public List<Map<String, Object>> getUpstream(String nodeId) {
        return graphRepository.findUpstream(nodeId);
    }

    public List<Map<String, Object>> getDownstream(String nodeId) {
        return graphRepository.findDownstream(nodeId);
    }

    public List<Map<String, Object>> getViolationPaths() {
        return graphRepository.findViolationPaths();
    }

    public List<Map<String, Object>> getAllNodes() {
        return graphRepository.findAllNodes();
    }
}
