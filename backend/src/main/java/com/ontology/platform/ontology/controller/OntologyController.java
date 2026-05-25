package com.ontology.platform.ontology.controller;

import com.ontology.platform.common.Result;
import com.ontology.platform.ontology.entity.CapabilityIndicator;
import com.ontology.platform.ontology.entity.CapabilityRelationship;
import com.ontology.platform.ontology.repository.OntologyRepository;
import com.ontology.platform.ontology.service.OntologySyncService;
import com.ontology.platform.rule.entity.RuleDefinition;
import com.ontology.platform.rule.repository.RuleRepository;
import com.ontology.platform.rule.service.DrlParameterService;
import com.ontology.platform.rule.service.DynamicRuleLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/ontology")
@RequiredArgsConstructor
public class OntologyController {

    private final OntologyRepository.IndicatorMapper indicatorMapper;
    private final OntologyRepository.RelationshipMapper relationshipMapper;
    private final OntologySyncService syncService;
    private final RuleRepository ruleRepository;
    private final DrlParameterService drlParameterService;
    private final DynamicRuleLoader dynamicRuleLoader;

    @GetMapping("/indicators")
    public Result<List<CapabilityIndicator>> getAllIndicators() {
        return Result.ok(indicatorMapper.selectList(null));
    }

    @GetMapping("/indicators/{id}")
    public Result<CapabilityIndicator> getIndicator(@PathVariable String id) {
        return Result.ok(indicatorMapper.selectById(id));
    }

    @PostMapping("/indicator")
    public Result<CapabilityIndicator> createIndicator(@RequestBody CapabilityIndicator indicator) {
        if (indicator.getId() == null || indicator.getId().isEmpty()) {
            indicator.setId(java.util.UUID.randomUUID().toString());
        }
        indicatorMapper.insert(indicator);
        return Result.ok(indicator);
    }

    @PutMapping("/indicator")
    public Result<?> updateIndicator(@RequestBody CapabilityIndicator indicator) {
        indicatorMapper.updateById(indicator);
        return Result.ok();
    }

    @DeleteMapping("/indicator/{id}")
    public Result<?> deleteIndicator(@PathVariable String id) {
        indicatorMapper.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/relationships")
    public Result<List<CapabilityRelationship>> getRelationships() {
        return Result.ok(relationshipMapper.selectList(null));
    }

    @GetMapping("/relationships/indicator/{indicatorId}")
    public Result<List<CapabilityRelationship>> getRelationshipsByIndicator(
            @PathVariable String indicatorId) {
        return Result.ok(relationshipMapper.findByIndicatorId(indicatorId));
    }

    @PostMapping("/relationship")
    public Result<String> saveRelationship(@RequestBody CapabilityRelationship relationship) {
        relationshipMapper.insert(relationship);
        injectIndicatorParamsToRule(relationship);
        return Result.ok(relationship.getId());
    }

    @PutMapping("/relationship")
    public Result<?> updateRelationship(@RequestBody CapabilityRelationship relationship) {
        relationshipMapper.updateById(relationship);
        injectIndicatorParamsToRule(relationship);
        return Result.ok();
    }

    @DeleteMapping("/relationship/{id}")
    public Result<?> deleteRelationship(@PathVariable String id) {
        relationshipMapper.deleteById(id);
        return Result.ok();
    }

    @PostMapping("/sync")
    public Result<?> syncToNeo4j() {
        syncService.syncToNeo4j();
        return Result.ok("Sync completed");
    }

    /**
     * When a relationship is bound to a rule, extract source and target
     * indicator IRI fragments and inject them as parameters into the rule's DRL.
     */
    private void injectIndicatorParamsToRule(CapabilityRelationship rel) {
        String ruleName = rel.getDroolsRuleName();
        if (ruleName == null || ruleName.isBlank()) return;

        RuleDefinition rule = ruleRepository.findByName(ruleName);
        if (rule == null) return;

        // Extract indicator key names (IRI fragment after #)
        List<String> keys = new java.util.ArrayList<>();
        addIndicatorKey(keys, rel.getSourceIndicatorId());
        addIndicatorKey(keys, rel.getTargetIndicatorId());

        if (keys.isEmpty()) return;

        String updatedDrl = drlParameterService.injectParameters(rule.getDrlContent(), keys);
        if (!updatedDrl.equals(rule.getDrlContent())) {
            rule.setDrlContent(updatedDrl);
            ruleRepository.updateById(rule);
            log.info("Injected indicator params {} into rule '{}'", keys, ruleName);
            // Reload if active
            if ("ACTIVE".equals(rule.getStatus())) {
                try { dynamicRuleLoader.loadRule(ruleName, updatedDrl); } catch (Exception ignored) {}
            }
        }
    }

    private void addIndicatorKey(List<String> keys, String indicatorId) {
        if (indicatorId == null) return;
        CapabilityIndicator ind = indicatorMapper.selectById(indicatorId);
        if (ind != null && ind.getIri() != null) {
            String iri = ind.getIri();
            int idx = iri.lastIndexOf('#');
            String key = idx >= 0 ? iri.substring(idx + 1) : iri.substring(iri.lastIndexOf('/') + 1);
            keys.add(key);
        }
    }
}
