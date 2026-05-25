package com.ontology.platform.rule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ontology.platform.rule.entity.RuleDefinition;
import com.ontology.platform.rule.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleManagementService {

    private final RuleRepository ruleRepository;
    private final DynamicRuleLoader dynamicRuleLoader;

    public Page<RuleDefinition> listRules(int pageNum, int pageSize,
                                          String keyword, String status,
                                          String sortBy, String sortOrder) {
        LambdaQueryWrapper<RuleDefinition> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w
                    .like(RuleDefinition::getRuleName, keyword)
                    .or()
                    .like(RuleDefinition::getTags, keyword)
                    .or()
                    .like(RuleDefinition::getDescription, keyword));
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(RuleDefinition::getStatus, status);
        }

        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        if ("publishTime".equals(sortBy)) {
            wrapper.orderBy(true, asc, RuleDefinition::getPublishTime);
        } else {
            wrapper.orderBy(true, asc, RuleDefinition::getUpdateTime);
        }

        return ruleRepository.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public RuleDefinition getRule(String id) {
        return ruleRepository.selectById(id);
    }

    public List<RuleDefinition> getActiveRules() {
        return ruleRepository.findActiveRules();
    }

    @Transactional
    public RuleDefinition createRule(RuleDefinition rule) {
        rule.setId(UUID.randomUUID().toString());
        rule.setVersion("v1");
        rule.setStatus("DRAFT");
        ruleRepository.insert(rule);
        return rule;
    }

    @Transactional
    public RuleDefinition updateRule(RuleDefinition rule) {
        ruleRepository.updateById(rule);
        return rule;
    }

    @Transactional
    public void publishRule(String id) {
        RuleDefinition rule = ruleRepository.selectById(id);
        if (rule == null) {
            throw new IllegalArgumentException("Rule not found: " + id);
        }
        rule.setStatus("ACTIVE");
        rule.setPublishTime(LocalDateTime.now());
        ruleRepository.updateById(rule);

        // Dynamically load the rule
        dynamicRuleLoader.loadRule(rule.getRuleName(), rule.getDrlContent());
    }

    @Transactional
    public void disableRule(String id) {
        RuleDefinition rule = ruleRepository.selectById(id);
        if (rule == null) {
            throw new IllegalArgumentException("Rule not found: " + id);
        }
        rule.setStatus("DISABLED");
        ruleRepository.updateById(rule);

        try {
            dynamicRuleLoader.unloadRule(rule.getRuleName());
        } catch (Exception e) {
            log.warn("Failed to unload rule '{}': {}", rule.getRuleName(), e.getMessage());
        }
    }

    @Transactional
    public void deleteRule(String id) {
        ruleRepository.deleteById(id);
    }

    public List<RuleDefinition> getVersions(String ruleName) {
        return ruleRepository.findVersionsByName(ruleName);
    }

    @Transactional
    public RuleDefinition rollback(String ruleName, String targetVersionId) {
        RuleDefinition target = ruleRepository.selectById(targetVersionId);
        if (target == null) {
            throw new IllegalArgumentException("Target version not found");
        }

        RuleDefinition current = ruleRepository.findVersionsByName(ruleName).stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()))
                .findFirst()
                .orElse(null);

        if (current != null) {
            current.setStatus("ARCHIVED");
            ruleRepository.updateById(current);
        }

        RuleDefinition newVersion = new RuleDefinition();
        newVersion.setRuleName(ruleName);
        newVersion.setDrlContent(target.getDrlContent());
        newVersion.setVersion("v" + (ruleRepository.findVersionsByName(ruleName).size() + 1));
        newVersion.setStatus("ACTIVE");
        newVersion.setPublishTime(LocalDateTime.now());
        ruleRepository.insert(newVersion);

        dynamicRuleLoader.loadRule(ruleName, newVersion.getDrlContent());

        return newVersion;
    }
}
