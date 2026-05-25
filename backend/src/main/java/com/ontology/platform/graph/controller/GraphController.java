package com.ontology.platform.graph.controller;

import com.ontology.platform.common.Result;
import com.ontology.platform.graph.entity.GraphNode;
import com.ontology.platform.graph.service.GraphAnalysisService;
import com.ontology.platform.graph.service.GraphQueryService;
import com.ontology.platform.graph.service.ViolationAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Map;

@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphQueryService queryService;
    private final GraphAnalysisService analysisService;
    private final ViolationAnalysisService violationAnalysisService;

    @GetMapping("/nodes")
    public Result<List<Map<String, Object>>> getAllNodes() {
        return Result.ok(queryService.getAllNodes());
    }

    @GetMapping("/neighbors/{id}")
    public Result<List<GraphNode>> getNeighbors(@PathVariable String id) {
        return Result.ok(queryService.getNeighbors(id));
    }

    @GetMapping("/upstream/{id}")
    public Result<List<Map<String, Object>>> getUpstream(@PathVariable String id) {
        return Result.ok(queryService.getUpstream(id));
    }

    @GetMapping("/downstream/{id}")
    public Result<List<Map<String, Object>>> getDownstream(@PathVariable String id) {
        return Result.ok(queryService.getDownstream(id));
    }

    @GetMapping("/path")
    public Result<List<Map<String, Object>>> findPath(
            @RequestParam String source,
            @RequestParam String target) {
        return Result.ok(analysisService.findShortestPath(source, target));
    }

    @GetMapping("/violation-paths")
    public Result<List<Map<String, Object>>> getViolationPaths() {
        return Result.ok(queryService.getViolationPaths());
    }

    @GetMapping("/analysis/centrality")
    public Result<Map<String, Object>> analyzeCentrality() {
        return Result.ok(analysisService.analyzeCentrality());
    }

    @GetMapping("/analysis/isolated")
    public Result<List<String>> findIsolated() {
        return Result.ok(analysisService.findIsolatedIndicators());
    }

    @PostMapping("/analysis/violation")
    public Result<Map<String, Object>> analyzeViolations(@RequestBody Map<String, Object> facts) {
        return Result.ok(violationAnalysisService.analyze(facts));
    }
}
