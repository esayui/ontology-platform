package com.ontology.platform.ontology.service;

import com.ontology.platform.ontology.entity.CapabilityIndicator;
import com.ontology.platform.ontology.entity.CapabilityRelationship;
import com.ontology.platform.ontology.repository.OntologyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OntologySyncService {

    private final Driver neo4jDriver;
    private final OntologyRepository.IndicatorMapper indicatorMapper;
    private final OntologyRepository.RelationshipMapper relationshipMapper;

    /**
     * Sync MySQL ontology data to Neo4j graph database.
     * Creates Indicator nodes and relationship edges.
     */
    public void syncToNeo4j() {
        try {
            List<CapabilityIndicator> indicators = indicatorMapper.selectList(null);
            List<CapabilityRelationship> relationships = relationshipMapper.selectList(null);

            try (Session session = neo4jDriver.session()) {
                session.run("MATCH (n) DETACH DELETE n");

                for (CapabilityIndicator indicator : indicators) {
                    session.run("""
                        CREATE (i:Indicator {
                            id: $id,
                            iri: $iri,
                            name: $name,
                            domain: $domain,
                            category: $category,
                            unit: $unit,
                            thresholdMin: $thresholdMin,
                            thresholdMax: $thresholdMax
                        })
                        """, mapOf(
                            "id", indicator.getId(),
                            "iri", indicator.getIri(),
                            "name", indicator.getName(),
                            "domain", indicator.getDomain(),
                            "category", indicator.getCategory(),
                            "unit", indicator.getUnit(),
                            "thresholdMin", indicator.getThresholdMin(),
                            "thresholdMax", indicator.getThresholdMax()
                    ));
                }

                for (CapabilityRelationship rel : relationships) {
                    session.run("""
                        MATCH (src:Indicator {id: $srcId})
                        MATCH (tgt:Indicator {id: $tgtId})
                        CREATE (src)-[:AFFECTS {
                            id: $id,
                            type: $type,
                            ruleName: $ruleName,
                            weight: $weight,
                            priority: $priority,
                            enabled: $enabled
                        }]->(tgt)
                        """, mapOf(
                            "srcId", rel.getSourceIndicatorId(),
                            "tgtId", rel.getTargetIndicatorId(),
                            "id", rel.getId(),
                            "type", rel.getRelationshipType(),
                            "ruleName", rel.getDroolsRuleName(),
                            "weight", rel.getWeight(),
                            "priority", rel.getPriority(),
                            "enabled", rel.getEnabled()
                    ));
                }
            }

            log.info("Synced {} indicators and {} relationships from MySQL to Neo4j",
                    indicators.size(), relationships.size());
        } catch (Exception e) {
            log.error("Failed to sync ontology to Neo4j", e);
            throw new RuntimeException("Ontology sync failed", e);
        }
    }

    private static java.util.Map<String, Object> mapOf(Object... keysAndValues) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }
}
