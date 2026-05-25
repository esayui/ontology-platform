package com.ontology.platform.simulation.service;

import com.ontology.platform.rule.service.DroolsEngineService;
import com.ontology.platform.simulation.entity.SimulationData;
import com.ontology.platform.simulation.repository.SimulationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationService {

    private final SimulationRepository simulationRepository;
    private final DroolsEngineService droolsEngineService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Ingest simulation data, validate against rules, and push results via WebSocket.
     */
    public List<DroolsEngineService.ViolationResult> ingestAndValidate(
            List<SimulationData> dataList) {

        // Save all data points
        for (SimulationData data : dataList) {
            data.setId(UUID.randomUUID().toString());
            simulationRepository.insert(data);
        }

        // Convert to facts map for Drools
        Map<String, Object> facts = dataList.stream()
                .collect(Collectors.toMap(
                        SimulationData::getIndicatorId,
                        SimulationData::getValue,
                        (v1, v2) -> v2));

        // Execute rules
        List<DroolsEngineService.ViolationResult> violations =
                droolsEngineService.executeRules(facts);

        // Push violations via WebSocket
        if (!violations.isEmpty()) {
            messagingTemplate.convertAndSend("/topic/violations", violations);
            log.info("Pushed {} violations to WebSocket", violations.size());
        }

        // Push updated data
        messagingTemplate.convertAndSend("/topic/simulation", dataList);

        return violations;
    }

    public List<SimulationData> getByIndicator(String indicatorId) {
        return simulationRepository.findByIndicatorId(indicatorId);
    }

    public List<SimulationData> getLatest(int limit) {
        return simulationRepository.findLatest(limit);
    }
}
