package com.ontology.platform.rule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicRuleLoader {

    private final KieServices kieServices;
    private KieContainer kieContainer;
    private final Map<String, String> ruleStore = new ConcurrentHashMap<>();

    /**
     * Add or update a rule and rebuild the entire KieContainer with all rules.
     */
    public synchronized void loadRule(String ruleName, String drlContent) {
        ruleStore.put(ruleName, drlContent);
        rebuildContainer();
        log.info("Rule '{}' loaded. Total active rules: {}", ruleName, ruleStore.size());
    }

    /**
     * Remove a rule and rebuild.
     */
    public synchronized void unloadRule(String ruleName) {
        ruleStore.remove(ruleName);
        rebuildContainer();
        log.info("Rule '{}' unloaded. Total active rules: {}", ruleName, ruleStore.size());
    }

    /**
     * Batch load multiple rules in a single rebuild.
     */
    public synchronized void loadAllRules(Map<String, String> rules) {
        ruleStore.putAll(rules);
        rebuildContainer();
        log.info("Batch loaded {} rules. Total active: {}", rules.size(), ruleStore.size());
    }

    private void rebuildContainer() {
        if (ruleStore.isEmpty()) {
            kieContainer = null;
            return;
        }

        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        for (Map.Entry<String, String> entry : ruleStore.entrySet()) {
            String rulePath = "src/main/resources/rules/" + entry.getKey() + ".drl";
            kieFileSystem.write(rulePath,
                    ResourceFactory.newByteArrayResource(entry.getValue().getBytes()));
        }

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();

        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            String errors = kieBuilder.getResults().getMessages().stream()
                    .map(Message::getText)
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Unknown compilation error");
            log.error("Rule compilation failed: {}", errors);
            throw new IllegalArgumentException("Rule compilation failed: " + errors);
        }

        KieModule kieModule = kieBuilder.getKieModule();
        kieContainer = kieServices.newKieContainer(kieModule.getReleaseId());
    }

    public KieContainer getKieContainer() {
        return kieContainer;
    }

    public int getActiveRuleCount() {
        return ruleStore.size();
    }
}
