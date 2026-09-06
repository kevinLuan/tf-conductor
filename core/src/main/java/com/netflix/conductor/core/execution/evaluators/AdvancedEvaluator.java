/*
 * Copyright 2026 Conductor Authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.core.execution.evaluators;

import java.util.Map;
import java.util.Optional;

import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.model.WorkflowModel;

/**
 * 高级表达式评估器扩展点（Taskflow）。
 *
 * <p>原有 {@link Evaluator} 仅接收表达式与任务输入，无法访问工作流上下文。 SWITCH 任务中的 {@code if-else}、{@code ai} 等评估器需要根据
 * {@link WorkflowModel} 解析参数，因此在不改动现有评估器实现的前提下增加本接口。
 */
public interface AdvancedEvaluator extends Evaluator {

    /**
     * 结合工作流与任务上下文评估表达式。
     *
     * @param workflowModel 当前工作流实例
     * @param workflowTask SWITCH 任务定义
     * @param taskId 本次调度生成的任务 ID
     * @param expression 待评估表达式（JSON 或脚本）
     * @param input 已解析的任务输入
     * @return 命中的分支值；未命中时返回 empty，由调用方走 defaultCase
     */
    Optional<Object> evaluate(
            WorkflowModel workflowModel,
            WorkflowTask workflowTask,
            String taskId,
            String expression,
            Map<String, Object> input)
            throws Exception;
}
