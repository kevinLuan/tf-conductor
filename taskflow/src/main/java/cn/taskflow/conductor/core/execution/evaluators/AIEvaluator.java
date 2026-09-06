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
package cn.taskflow.conductor.core.execution.evaluators;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.core.execution.evaluators.AdvancedEvaluator;
import com.netflix.conductor.model.WorkflowModel;

import cn.taskflow.conductor.core.model.SwitchAiParams;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * SWITCH 任务的 AI 分类评估器（evaluatorType = {@code ai}）。
 *
 * <p>expression 为 {@link SwitchAiParams} JSON，根据指令与输入从 decisionCases 中选出语义最接近的分支。
 *
 * @author kevin.luan
 * @since 2025-05-06
 */
@Component(AIEvaluator.NAME)
public class AIEvaluator implements AdvancedEvaluator {

    public static final String NAME = "ai";
    private static final Logger LOGGER = LoggerFactory.getLogger(AIEvaluator.class);

    private final ObjectMapper objectMapper;
    private final AIFactory aiFactory;

    public AIEvaluator(ObjectMapper objectMapper, AIFactory aiFactory) {
        this.objectMapper = objectMapper;
        this.aiFactory = aiFactory;
    }

    /**
     * 无工作流上下文时不可用。
     */
    @Override
    public Object evaluate(String expression, Object input) {
        throw new UnsupportedOperationException(
                "AIEvaluator 需要工作流上下文，请使用带 WorkflowModel 的 evaluate 方法");
    }

    /**
     * 将 SWITCH expression 解析为 AI 分类参数。
     */
    private SwitchAiParams parseParameter(String expression) {
        try {
            return objectMapper.readValue(expression, SwitchAiParams.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("解析 AICondition 失败: " + expression, e);
        }
    }

    /**
     * 按指令与任务输入，从 decisionCases 里选出语义最接近的分支名。
     */
    @Override
    public Optional<Object> evaluate(
            WorkflowModel workflow,
            WorkflowTask task,
            String taskId,
            String expression,
            Map<String, Object> input)
            throws Exception {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.debug("AI evaluator -- taskId: {}, expression: {}", taskId, expression);
        }
        SwitchAiParams condition = parseParameter(expression);
        Set<String> cases = task.getDecisionCases().keySet();
        return aiFactory.classify(condition, input, cases).map(result -> result);
    }
}
