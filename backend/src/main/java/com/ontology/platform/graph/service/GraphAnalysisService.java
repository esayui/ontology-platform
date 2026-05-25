package com.ontology.platform.graph.service;

import com.ontology.platform.graph.repository.GraphRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GraphAnalysisService {

    private final GraphRepository graphRepository;

    public List<Map<String, Object>> findShortestPath(String sourceId, String targetId) {
        return graphRepository.findShortestPath(sourceId, targetId);
    }

    /**
     * Calculate approximate centrality scores from the graph.
     */
    public Map<String, Object> analyzeCentrality() {
        List<Map<String, Object>> allNodes = graphRepository.findAllNodes();
        Map<String, Integer> degreeMap = new HashMap<>();
        List<String> topNodes = new ArrayList<>();

        for (Map<String, Object> nodeData : allNodes) {
            @SuppressWarnings("unchecked")
            List<Object> relationships = (List<Object>) nodeData.get("relationships");
            @SuppressWarnings("unchecked")
            Map<String, Object> node = (Map<String, Object>) nodeData.get("node");
            String name = (String) node.get("name");
            int degree = relationships != null ? relationships.size() : 0;
            degreeMap.put(name, degree);
        }

        degreeMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> topNodes.add(e.getKey() + "(" + e.getValue() + ")"));

        return Map.of(
                "degreeCentrality", degreeMap,
                "topNodes", topNodes,
                "totalNodes", allNodes.size()
        );
    }

    /**
     * Find isolated indicators (no relationships).
     */
    public List<String> findIsolatedIndicators() {
        List<Map<String, Object>> allNodes = graphRepository.findAllNodes();
        List<String> isolated = new ArrayList<>();

        for (Map<String, Object> nodeData : allNodes) {
            @SuppressWarnings("unchecked")
            List<Object> relationships = (List<Object>) nodeData.get("relationships");
            if (relationships == null || relationships.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> node = (Map<String, Object>) nodeData.get("node");
                isolated.add((String) node.get("name"));
            }
        }

        return isolated;
    }
}
