package com.ontology.platform.graph.repository;

import com.ontology.platform.graph.entity.GraphNode;
import lombok.RequiredArgsConstructor;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class GraphRepository {

    private final Driver neo4jDriver;

    public List<Map<String, Object>> findShortestPath(String sourceId, String targetId) {
        try (Session session = neo4jDriver.session()) {
            var result = session.run("""
                MATCH (src:Indicator {id: $sourceId}), (tgt:Indicator {id: $targetId}),
                      path = shortestPath((src)-[*]-(tgt))
                RETURN
                  [node IN nodes(path) | {id: node.id, name: node.name, domain: node.domain, category: node.category}] as nodes,
                  [rel IN relationships(path) | {id: rel.id, type: rel.type, ruleName: rel.ruleName, weight: rel.weight}] as rels,
                  length(path) as length
                """, Map.of("sourceId", sourceId, "targetId", targetId));
            return result.list().stream()
                    .map(r -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("nodes", r.get("nodes").asList());
                        map.put("rels", r.get("rels").asList());
                        map.put("length", r.get("length").asLong());
                        return map;
                    })
                    .toList();
        }
    }

    public List<GraphNode> findNeighbors(String nodeId) {
        try (Session session = neo4jDriver.session()) {
            var result = session.run("""
                MATCH (n:Indicator {id: $id})-[r]-(m:Indicator)
                RETURN m, r, type(r) as relType
                """, Map.of("id", nodeId));

            List<GraphNode> neighbors = new ArrayList<>();
            while (result.hasNext()) {
                var record = result.next();
                var node = record.get("m").asNode();
                GraphNode gn = new GraphNode();
                gn.setId(node.get("id").asString());
                gn.setName(node.get("name").asString());
                List<String> labelList = new ArrayList<>();
                node.labels().forEach(labelList::add);
                gn.setLabels(labelList);
                neighbors.add(gn);
            }
            return neighbors;
        }
    }

    public List<Map<String, Object>> findAllNodes() {
        try (Session session = neo4jDriver.session()) {
            var result = session.run("""
                MATCH (n:Indicator)
                OPTIONAL MATCH (n)-[r:AFFECTS]->(m:Indicator)
                RETURN n,
                  collect(
                    CASE WHEN r IS NOT NULL AND m IS NOT NULL
                    THEN {
                      id: r.id, type: r.type, ruleName: r.ruleName,
                      weight: r.weight, priority: r.priority, enabled: r.enabled,
                      targetId: m.id, targetName: m.name,
                      targetDomain: m.domain, targetCategory: m.category
                    } END
                  ) as relationships
                """);
            return result.list().stream()
                    .map(r -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("node", r.get("n").asNode().asMap());
                        map.put("relationships", r.get("relationships").asList());
                        return map;
                    })
                    .toList();
        }
    }

    public List<Map<String, Object>> findUpstream(String nodeId) {
        try (Session session = neo4jDriver.session()) {
            var result = session.run("""
                MATCH (n:Indicator {id: $id})<-[r*1..5]-(upstream:Indicator)
                RETURN
                  properties(upstream) as node,
                  [rel IN r | {id: rel.id, type: rel.type, ruleName: rel.ruleName, weight: rel.weight}] as rels,
                  size(r) as distance
                LIMIT 50
                """, Map.of("id", nodeId));
            return result.list().stream()
                    .map(r -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("node", r.get("node").asMap());
                        map.put("rels", r.get("rels").asList());
                        map.put("distance", r.get("distance").asLong());
                        return map;
                    })
                    .toList();
        }
    }

    public List<Map<String, Object>> findDownstream(String nodeId) {
        try (Session session = neo4jDriver.session()) {
            var result = session.run("""
                MATCH (n:Indicator {id: $id})-[r*1..5]->(downstream:Indicator)
                RETURN
                  properties(downstream) as node,
                  [rel IN r | {id: rel.id, type: rel.type, ruleName: rel.ruleName, weight: rel.weight}] as rels,
                  size(r) as distance
                LIMIT 50
                """, Map.of("id", nodeId));
            return result.list().stream()
                    .map(r -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("node", r.get("node").asMap());
                        map.put("rels", r.get("rels").asList());
                        map.put("distance", r.get("distance").asLong());
                        return map;
                    })
                    .toList();
        }
    }

    public List<Map<String, Object>> findViolationPaths() {
        try (Session session = neo4jDriver.session()) {
            var result = session.run("""
                MATCH path = (n:Indicator)-[r:AFFECTS*1..5]->(m:Indicator)
                WHERE all(rel IN relationships(path) WHERE rel.enabled = true)
                RETURN
                  [node IN nodes(path) | properties(node)] as nodes,
                  [rel IN relationships(path) | properties(rel)] as rels
                LIMIT 50
                """);
            return result.list().stream()
                    .map(r -> {
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("nodes", r.get("nodes").asList());
                        map.put("rels", r.get("rels").asList());
                        return map;
                    })
                    .toList();
        }
    }
}
