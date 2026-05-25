package com.ontology.platform.rule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DroolsEngineService {

    private final DynamicRuleLoader dynamicRuleLoader;
    private final KieContainer kieContainer;

    private KieContainer getActiveContainer() {
        KieContainer dynamic = dynamicRuleLoader.getKieContainer();
        return dynamic != null ? dynamic : kieContainer;
    }

    /**
     * Execute rules against a set of facts (simulation data).
     * Returns violation results including rule name, violated indicator, and reason.
     */
    public List<ViolationResult> executeRules(Map<String, Object> facts) {
        KieContainer container = getActiveContainer();
        if (container == null) {
            log.warn("No KieContainer available for rule execution");
            return Collections.emptyList();
        }

        KieSession kieSession = container.newKieSession();
        List<ViolationResult> violations = new ArrayList<>();

        try {
            // Set global for collecting violations
            kieSession.setGlobal("violations", violations);

            // Insert each fact
            for (Map.Entry<String, Object> entry : facts.entrySet()) {
                kieSession.insert(createFact(entry.getKey(), entry.getValue()));
            }

            // Fire all rules
            int fired = kieSession.fireAllRules();
            log.info("Fired {} rules, {} violations found", fired, violations.size());

        } finally {
            kieSession.dispose();
        }

        return violations;
    }

    /**
     * Execute rules and get detailed results including matched rule names.
     */
    public List<Map<String, Object>> executeWithDetails(Map<String, Object> facts) {
        KieContainer container = getActiveContainer();
        if (container == null) {
            return Collections.emptyList();
        }

        KieSession kieSession = container.newKieSession();
        List<Map<String, Object>> results = new ArrayList<>();

        try {
            for (Map.Entry<String, Object> entry : facts.entrySet()) {
                kieSession.insert(createFact(entry.getKey(), entry.getValue()));
            }

            int fired = kieSession.fireAllRules();

            Map<String, Object> summary = new HashMap<>();
            summary.put("rulesFired", fired);
            summary.put("factsCount", facts.size());
            results.add(summary);

        } finally {
            kieSession.dispose();
        }

        return results;
    }

    private Object createFact(String name, Object value) {
        return new IndicatorFact(name, value);
    }

    /**
     * Fact object for Drools rules.
     */
    public record IndicatorFact(String indicatorName, Object value) {}

    /**
     * Violation result produced by rules.
     */
    public static class ViolationResult {
        private String ruleName;
        private String indicatorId;
        private String reason;
        private double expectedMin;
        private double expectedMax;
        private double actualValue;

        public ViolationResult() {}

        public ViolationResult(String ruleName, String indicatorId, String reason,
                                double expectedMin, double expectedMax, double actualValue) {
            this.ruleName = ruleName;
            this.indicatorId = indicatorId;
            this.reason = reason;
            this.expectedMin = expectedMin;
            this.expectedMax = expectedMax;
            this.actualValue = actualValue;
        }

        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }
        public String getIndicatorId() { return indicatorId; }
        public void setIndicatorId(String indicatorId) { this.indicatorId = indicatorId; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public double getExpectedMin() { return expectedMin; }
        public void setExpectedMin(double expectedMin) { this.expectedMin = expectedMin; }
        public double getExpectedMax() { return expectedMax; }
        public void setExpectedMax(double expectedMax) { this.expectedMax = expectedMax; }
        public double getActualValue() { return actualValue; }
        public void setActualValue(double actualValue) { this.actualValue = actualValue; }
    }
}
