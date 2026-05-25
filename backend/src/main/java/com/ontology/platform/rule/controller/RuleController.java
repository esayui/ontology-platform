package com.ontology.platform.rule.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ontology.platform.common.Result;
import com.ontology.platform.rule.entity.RuleDefinition;
import com.ontology.platform.ontology.entity.CapabilityIndicator;
import com.ontology.platform.ontology.entity.CapabilityRelationship;
import com.ontology.platform.ontology.repository.OntologyRepository;
import com.ontology.platform.rule.service.DrlParameterService;
import com.ontology.platform.rule.service.DroolsEngineService;
import com.ontology.platform.rule.service.RuleManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleManagementService managementService;
    private final DroolsEngineService engineService;
    private final DrlParameterService drlParameterService;
    private final OntologyRepository.RelationshipMapper relationshipMapper;
    private final OntologyRepository.IndicatorMapper indicatorMapper;

    @GetMapping
    public Result<Page<RuleDefinition>> listRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "updateTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        return Result.ok(managementService.listRules(page, size, keyword, status, sortBy, sortOrder));
    }

    @GetMapping("/{id}")
    public Result<RuleDefinition> getRule(@PathVariable String id) {
        return Result.ok(managementService.getRule(id));
    }

    @GetMapping("/active")
    public Result<List<RuleDefinition>> getActiveRules() {
        return Result.ok(managementService.getActiveRules());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RULE_EDITOR')")
    public Result<RuleDefinition> createRule(@RequestBody RuleDefinition rule) {
        return Result.ok(managementService.createRule(rule));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RULE_EDITOR')")
    public Result<RuleDefinition> updateRule(@RequestBody RuleDefinition rule) {
        return Result.ok(managementService.updateRule(rule));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'RULE_EDITOR')")
    public Result<?> publishRule(@PathVariable String id) {
        managementService.publishRule(id);
        return Result.ok();
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAnyRole('ADMIN', 'RULE_EDITOR')")
    public Result<?> disableRule(@PathVariable String id) {
        managementService.disableRule(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RULE_EDITOR')")
    public Result<?> deleteRule(@PathVariable String id) {
        managementService.deleteRule(id);
        return Result.ok();
    }

    @GetMapping("/{ruleName}/versions")
    public Result<List<RuleDefinition>> getVersions(@PathVariable String ruleName) {
        return Result.ok(managementService.getVersions(ruleName));
    }

    @PostMapping("/{ruleName}/rollback/{versionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RULE_EDITOR')")
    public Result<RuleDefinition> rollback(@PathVariable String ruleName,
                                           @PathVariable String versionId) {
        return Result.ok(managementService.rollback(ruleName, versionId));
    }

    @PostMapping("/execute")
    public Result<List<DroolsEngineService.ViolationResult>> executeRules(
            @RequestBody Map<String, Object> facts) {
        return Result.ok(engineService.executeRules(facts));
    }

    /**
     * Check which rules have bound relationships but missing indicator params in DRL.
     * Returns: { "RULE_NAME": ["missingKey1", "missingKey2"], ... }
     */
    @GetMapping("/completeness")
    public Result<Map<String, Object>> checkCompleteness() {
        List<RuleDefinition> activeRules = managementService.getActiveRules();
        List<CapabilityRelationship> allRels = relationshipMapper.selectList(null);

        // Group relationships by rule name
        Map<String, Set<String>> expectedByRule = new HashMap<>();
        for (CapabilityRelationship rel : allRels) {
            String ruleName = rel.getDroolsRuleName();
            if (ruleName == null || ruleName.isBlank()) continue;
            expectedByRule.computeIfAbsent(ruleName, k -> new LinkedHashSet<>());
            addIriFragment(expectedByRule.get(ruleName), rel.getSourceIndicatorId());
            addIriFragment(expectedByRule.get(ruleName), rel.getTargetIndicatorId());
        }

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> incompleteList = new ArrayList<>();
        int incompleteCount = 0;

        for (RuleDefinition rule : activeRules) {
            Set<String> expected = expectedByRule.get(rule.getRuleName());
            if (expected == null || expected.isEmpty()) continue;

            List<String> missing = drlParameterService.checkMissingParams(
                    rule.getDrlContent(), new ArrayList<>(expected));
            if (!missing.isEmpty()) {
                incompleteCount++;
                Map<String, Object> info = new HashMap<>();
                info.put("ruleName", rule.getRuleName());
                info.put("missingParams", missing);
                info.put("expectedCount", expected.size());
                incompleteList.add(info);
            }
        }

        result.put("incompleteCount", incompleteCount);
        result.put("incompleteRules", incompleteList);
        result.put("totalActiveRules", activeRules.size());
        return Result.ok(result);
    }

    private void addIriFragment(Set<String> set, String indicatorId) {
        if (indicatorId == null) return;
        CapabilityIndicator ind = indicatorMapper.selectById(indicatorId);
        if (ind != null && ind.getIri() != null) {
            String iri = indicatorMapper.selectById(indicatorId).getIri();
            int idx = iri.lastIndexOf('#');
            set.add(idx >= 0 ? iri.substring(idx + 1) : iri.substring(iri.lastIndexOf('/') + 1));
        }
    }
}
