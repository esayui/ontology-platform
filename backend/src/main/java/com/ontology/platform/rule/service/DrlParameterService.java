package com.ontology.platform.rule.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DrlParameterService {

    private static final Pattern INDICATOR_PARAM =
            Pattern.compile("IndicatorFact\\(indicatorName\\s*==\\s*\"([^\"]+)\"");

    /**
     * Inject indicator parameters into a DRL rule's when clause.
     * Only adds parameters that don't already exist.
     */
    public String injectParameters(String drlContent, List<String> indicatorKeys) {
        if (indicatorKeys == null || indicatorKeys.isEmpty()) return drlContent;

        Set<String> existing = extractIndicatorParams(drlContent);
        List<String> toAdd = indicatorKeys.stream()
                .filter(k -> k != null && !k.isEmpty() && !existing.contains(k))
                .distinct()
                .toList();

        if (toAdd.isEmpty()) return drlContent;

        StringBuilder newParams = new StringBuilder();
        for (String key : toAdd) {
            newParams.append("        $fact_").append(sanitizeVar(key))
              .append(" : IndicatorFact(indicatorName == \"").append(key).append("\", value != null)\n");
        }

        // Find "then" keyword to insert before it
        int thenIdx = findKeyword(drlContent, "\nthen");
        if (thenIdx > 0) {
            return drlContent.substring(0, thenIdx) + newParams.toString() + drlContent.substring(thenIdx);
        }

        // Fallback: append to end of when block
        int whenIdx = findKeyword(drlContent, "when");
        if (whenIdx > 0) {
            return drlContent.substring(0, whenIdx + 4) + "\n" + newParams + drlContent.substring(whenIdx + 4);
        }

        log.warn("Could not locate when/then block in DRL");
        return drlContent;
    }

    /**
     * Extract existing indicator parameter names from DRL.
     */
    public Set<String> extractIndicatorParams(String drlContent) {
        Set<String> params = new LinkedHashSet<>();
        Matcher m = INDICATOR_PARAM.matcher(drlContent);
        while (m.find()) {
            params.add(m.group(1));
        }
        return params;
    }

    /**
     * Check if all expected indicators are present in the DRL.
     */
    public List<String> checkMissingParams(String drlContent, List<String> expectedKeys) {
        if (drlContent == null || expectedKeys == null || expectedKeys.isEmpty()) return Collections.emptyList();
        Set<String> existing = extractIndicatorParams(drlContent);
        return expectedKeys.stream()
                .filter(k -> k != null && !k.isEmpty() && !existing.contains(k))
                .distinct()
                .toList();
    }

    private int findKeyword(String text, String keyword) {
        // Case-insensitive search for keyword preceded by whitespace or start
        Pattern p = Pattern.compile("(?i)(^|\\s)" + Pattern.quote(keyword.trim()) + "\\b", Pattern.MULTILINE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.start() + m.group(1).length();
        }
        return -1;
    }

    private String sanitizeVar(String key) {
        return key.replaceAll("[^a-zA-Z0-9_]", "_");
    }
}
