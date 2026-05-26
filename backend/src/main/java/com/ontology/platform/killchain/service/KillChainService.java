package com.ontology.platform.killchain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ontology.platform.killchain.entity.KillChainTask;
import com.ontology.platform.killchain.repository.KillChainTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KillChainService {

    private final KillChainTaskRepository taskRepository;

    public Page<KillChainTask> listTasks(int page, int size, String keyword) {
        LambdaQueryWrapper<KillChainTask> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(KillChainTask::getName, keyword)
                   .or().like(KillChainTask::getDescription, keyword);
        }
        wrapper.orderByDesc(KillChainTask::getUpdateTime);
        return taskRepository.selectPage(new Page<>(page, size), wrapper);
    }

    public KillChainTask getTask(String id) {
        return taskRepository.selectById(id);
    }

    @Transactional
    public KillChainTask createTask(KillChainTask task) {
        task.setId(UUID.randomUUID().toString());
        task.setStatus("DRAFT");
        taskRepository.insert(task);
        return task;
    }

    @Transactional
    public KillChainTask updateTask(KillChainTask task) {
        taskRepository.updateById(task);
        return task;
    }

    @Transactional
    public void deleteTask(String id) {
        taskRepository.deleteById(id);
    }

    public List<KillChainTask> findActiveTasks() {
        return taskRepository.findActiveTasks();
    }
}
