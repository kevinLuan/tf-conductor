/*
 * Copyright 2026 济南飞流数据科技有限公司 (www.taskflow.cn).
 * <p>
 * 本文件为基于原 Conductor 项目的二次开发新增内容。
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *     http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taskflow.conductor.core.execution.mapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.core.execution.mapper.TaskMapper;
import com.netflix.conductor.core.execution.mapper.TaskMapperContext;
import com.netflix.conductor.core.utils.ParametersUtils;
import com.netflix.conductor.dao.MetadataDAO;
import com.netflix.conductor.model.TaskModel;
import com.netflix.conductor.model.WorkflowModel;

/**
 * 将 {@code type=FOR} 映射为进行中的循环头。第一轮循环体仍由 {@code For.execute()} 调度。
 */
@Component
public class ForTaskMapper implements TaskMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForTaskMapper.class);

    private final MetadataDAO metadataDAO;
    private final ParametersUtils parametersUtils;

    public ForTaskMapper(MetadataDAO metadataDAO, ParametersUtils parametersUtils) {
        this.metadataDAO = metadataDAO;
        this.parametersUtils = parametersUtils;
    }

    @Override
    public String getTaskType() {
        return TaskType.FOR.name();
    }

    /**
     * 只产出进行中的 FOR 循环头。已终结的同名任务不再映射，避免重复调度。
     */
    @Override
    public List<TaskModel> getMappedTasks(TaskMapperContext taskMapperContext) {
        LOGGER.debug("TaskMapperContext {} in ForTaskMapper", taskMapperContext);

        WorkflowTask workflowTask = taskMapperContext.getWorkflowTask();
        WorkflowModel workflowModel = taskMapperContext.getWorkflowModel();

        TaskModel existing = workflowModel.getTaskByRefName(workflowTask.getTaskReferenceName());
        if (existing != null && existing.getStatus().isTerminal()) {
            return List.of();
        }

        TaskDef taskDefinition =
                Optional.ofNullable(taskMapperContext.getTaskDefinition())
                        .orElseGet(
                                () ->
                                        Optional.ofNullable(
                                                        metadataDAO.getTaskDef(
                                                                workflowTask.getName()))
                                                .orElseGet(TaskDef::new));

        TaskModel forTask = taskMapperContext.createTaskModel();
        forTask.setTaskType(TaskType.TASK_TYPE_FOR);
        forTask.setStatus(TaskModel.Status.IN_PROGRESS);
        forTask.setStartTime(System.currentTimeMillis());
        forTask.setRateLimitPerFrequency(taskDefinition.getRateLimitPerFrequency());
        forTask.setRateLimitFrequencyInSeconds(taskDefinition.getRateLimitFrequencyInSeconds());
        forTask.setRetryCount(taskMapperContext.getRetryCount());

        Map<String, Object> taskInput =
                parametersUtils.getTaskInputV2(
                        workflowTask.getInputParameters(),
                        workflowModel,
                        forTask.getTaskId(),
                        taskDefinition);
        forTask.setInputData(taskInput);
        return List.of(forTask);
    }
}
