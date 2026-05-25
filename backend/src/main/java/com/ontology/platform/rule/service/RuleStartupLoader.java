package com.ontology.platform.rule.service;

import com.ontology.platform.rule.entity.RuleDefinition;
import com.ontology.platform.rule.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads all ACTIVE rules from the database into the DynamicRuleLoader on every startup.
 * Runs after DataInitializer to ensure rules are always loaded.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class RuleStartupLoader implements CommandLineRunner {

    private final RuleRepository ruleRepository;
    private final DynamicRuleLoader dynamicRuleLoader;

    @Override
    public void run(String... args) {
        List<RuleDefinition> activeRules = ruleRepository.findActiveRules();

        if (activeRules.isEmpty()) {
            log.info("No active rules found in database");
            return;
        }

        Map<String, String> rulesMap = new HashMap<>();
        for (RuleDefinition rule : activeRules) {
            rulesMap.put(rule.getRuleName(), rule.getDrlContent());
        }

        try {
            dynamicRuleLoader.loadAllRules(rulesMap);
            log.info("Startup: loaded {} active rules from database", rulesMap.size());
        } catch (Exception e) {
            log.error("Startup: failed to load rules - {}", e.getMessage());
        }
    }
}
