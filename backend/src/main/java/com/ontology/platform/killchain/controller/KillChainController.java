package com.ontology.platform.killchain.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ontology.platform.common.Result;
import com.ontology.platform.killchain.entity.KillChainTask;
import com.ontology.platform.killchain.service.KillChainService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/killchain")
@RequiredArgsConstructor
public class KillChainController {

    private final KillChainService service;

    @GetMapping("/tasks")
    public Result<Page<KillChainTask>> listTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(service.listTasks(page, size, keyword));
    }

    @GetMapping("/tasks/{id}")
    public Result<KillChainTask> getTask(@PathVariable String id) {
        return Result.ok(service.getTask(id));
    }

    @PostMapping("/tasks")
    public Result<KillChainTask> createTask(@RequestBody KillChainTask task) {
        return Result.ok(service.createTask(task));
    }

    @PutMapping("/tasks")
    public Result<KillChainTask> updateTask(@RequestBody KillChainTask task) {
        return Result.ok(service.updateTask(task));
    }

    @DeleteMapping("/tasks/{id}")
    public Result<?> deleteTask(@PathVariable String id) {
        service.deleteTask(id);
        return Result.ok();
    }

    /** Auto-save model data during editing */
    @PostMapping("/tasks/{id}/save-model")
    public Result<?> saveModel(@PathVariable String id, @RequestBody Map<String, Object> body) {
        KillChainTask task = service.getTask(id);
        if (task == null) return Result.notFound("任务不存在");
        // body contains modelData as JSON string
        Object modelData = body.get("modelData");
        task.setModelData(modelData != null ? modelData.toString() : null);
        if (body.containsKey("name")) task.setName((String) body.get("name"));
        if (body.containsKey("status")) task.setStatus((String) body.get("status"));
        service.updateTask(task);
        return Result.ok();
    }
}
