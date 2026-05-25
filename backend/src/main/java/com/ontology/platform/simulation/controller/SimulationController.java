package com.ontology.platform.simulation.controller;

import com.ontology.platform.common.Result;
import com.ontology.platform.rule.service.DroolsEngineService;
import com.ontology.platform.simulation.entity.SimulationData;
import com.ontology.platform.simulation.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    @PostMapping("/ingest")
    public Result<List<DroolsEngineService.ViolationResult>> ingestData(
            @RequestBody List<SimulationData> dataList) {
        return Result.ok(simulationService.ingestAndValidate(dataList));
    }

    @GetMapping("/data/{indicatorId}")
    public Result<List<SimulationData>> getByIndicator(@PathVariable String indicatorId) {
        return Result.ok(simulationService.getByIndicator(indicatorId));
    }

    @GetMapping("/data/latest")
    public Result<List<SimulationData>> getLatest(@RequestParam(defaultValue = "50") int limit) {
        return Result.ok(simulationService.getLatest(limit));
    }
}
