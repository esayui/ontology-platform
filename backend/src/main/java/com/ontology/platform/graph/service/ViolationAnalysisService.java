package com.ontology.platform.graph.service;

import com.ontology.platform.rule.service.DroolsEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViolationAnalysisService {

    private final Driver neo4jDriver;
    private final DroolsEngineService droolsEngineService;

    /**
     * Run Drools rules on provided facts, then trace propagation paths
     * for each violated indicator through Neo4j.
     */
    public Map<String, Object> analyze(Map<String, Object> facts) {
        // Step 1: Execute Drools rules
        List<DroolsEngineService.ViolationResult> violations =
                droolsEngineService.executeRules(facts);

        Map<String, Object> result = new HashMap<>();
        result.put("violations", violations);
        result.put("factsCount", facts.size());

        if (violations.isEmpty()) {
            result.put("paths", Collections.emptyList());
            result.put("summary", "所有指标符合规则约束，无违规链路");
            return result;
        }

        // Step 2: For each violated indicator, trace downstream propagation
        List<Map<String, Object>> propagationPaths = new ArrayList<>();
        Set<String> visitedIndicatorIds = new HashSet<>();

        for (DroolsEngineService.ViolationResult v : violations) {
            if (v.getIndicatorId() == null) continue;
            List<Map<String, Object>> paths = traceDownstream(v.getIndicatorId());
            for (Map<String, Object> path : paths) {
                path.put("violation", Map.of(
                        "ruleName", v.getRuleName(),
                        "indicatorId", v.getIndicatorId(),
                        "reason", v.getReason(),
                        "expectedMin", v.getExpectedMin(),
                        "expectedMax", v.getExpectedMax(),
                        "actualValue", v.getActualValue()
                ));
            }
            propagationPaths.addAll(paths);
        }

        result.put("paths", propagationPaths);
        result.put("violationCount", violations.size());
        result.put("pathCount", propagationPaths.size());
        result.put("summary", String.format(
                "共检测到 %d 条规则违规，追溯 %d 条传播链路",
                violations.size(), propagationPaths.size()));
        return result;
    }

    private List<Map<String, Object>> traceDownstream(String indicatorId) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            var queryResult = session.run("""
                MATCH path = (n:Indicator)-[r:AFFECTS*1..4]->(m:Indicator)
                WHERE n.iri ENDS WITH $indicatorKey
                  AND all(rel IN relationships(path) WHERE rel.enabled = true)
                RETURN
                  [node IN nodes(path) | {id: node.id, name: node.name, domain: node.domain, category: node.category}] as nodes,
                  [rel IN relationships(path) | {id: rel.id, type: rel.type, ruleName: rel.ruleName, weight: rel.weight}] as rels,
                  size(relationships(path)) as depth
                ORDER BY depth DESC
                LIMIT 20
                """, Map.of("indicatorKey", indicatorId));

            while (queryResult.hasNext()) {
                var record = queryResult.next();
                Map<String, Object> map = new HashMap<>();
                map.put("rootIndicatorId", indicatorId);
                map.put("nodes", record.get("nodes").asList());
                map.put("rels", record.get("rels").asList());
                map.put("depth", record.get("depth").asLong());
                results.add(map);
            }
        } catch (Exception e) {
            log.warn("Failed to trace downstream for {}: {}", indicatorId, e.getMessage());
        }
        return results;
    }
}
