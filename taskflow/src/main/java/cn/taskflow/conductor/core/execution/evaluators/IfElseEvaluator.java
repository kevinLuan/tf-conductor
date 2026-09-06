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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.core.execution.evaluators.AdvancedEvaluator;
import com.netflix.conductor.core.utils.ParametersUtils;
import com.netflix.conductor.model.WorkflowModel;

import cn.taskflow.conductor.core.model.condition.ConditionRouter;
import cn.taskflow.conductor.core.model.condition.OperatorEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * SWITCH 任务的条件分支评估器（evaluatorType = {@code if-else}）。
 *
 * <p>expression 为 {@link ConditionRouter} 列表 JSON。先用 {@link ParametersUtils} 解析左右值中的 {@code ${...}}
 * 占位符，再按 AND/OR 与比较运算符求值，返回命中分支的 action。
 *
 * @author kevin.luan
 * @since 2025-05-06
 */
@Component(IfElseEvaluator.NAME)
public class IfElseEvaluator implements AdvancedEvaluator {

    public static final String NAME = "if-else";
    private static final Logger LOGGER = LoggerFactory.getLogger(IfElseEvaluator.class);

    private final ObjectMapper objectMapper;
    private final ParametersUtils parametersUtils;

    public IfElseEvaluator(ObjectMapper objectMapper, ParametersUtils parametersUtils) {
        this.objectMapper = objectMapper;
        this.parametersUtils = parametersUtils;
    }

    /**
     * 无工作流上下文时不可用，避免走错评估入口。
     */
    @Override
    public Object evaluate(String expression, Object input) {
        throw new UnsupportedOperationException(
                "IfElseEvaluator 需要工作流上下文，请使用带 WorkflowModel 的 evaluate 方法");
    }

    /**
     * 将 SWITCH expression 解析为条件分支列表。
     */
    private List<ConditionRouter> parseRouters(String expression) {
        try {
            return objectMapper.readValue(
                    expression, new TypeReference<List<ConditionRouter>>() {
                    });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("解析 if-else 条件失败: " + expression, e);
        }
    }

    /**
     * 先用 {@link ParametersUtils} 解析左右值占位符，再按分支顺序求值，返回第一个命中的 action（decisionCases key）。
     */
    @Override
    public Optional<Object> evaluate(
            WorkflowModel workflow,
            WorkflowTask task,
            String taskId,
            String expression,
            Map<String, Object> input)
            throws Exception {
        List<ConditionRouter> routers = parseRouters(expression);
        if (routers.isEmpty()) {
            return Optional.empty();
        }
        Map<String, Object> exprMap = getStringObjectMap(routers);
        exprMap =
                parametersUtils.getTaskInputV2(exprMap, workflow, taskId, task.getTaskDefinition());
        for (int i = 0; i < routers.size(); i++) {
            ConditionRouter router = routers.get(i);
            ConditionRouter.ConditionGroup group = router.getCondition();
            List<ConditionRouter.ConditionPredicate> predicates = group.getChildren();
            for (int j = 0; j < predicates.size(); j++) {
                ConditionRouter.ConditionPredicate node = predicates.get(j);
                String key = getKey(i, j);
                node.setExpression(asString(exprMap.get(key + "_expr")));
                node.setValue(asString(exprMap.get(key + "_val")));
            }
        }
        LOGGER.debug("if-else 分支求值完成, taskId={}, branches={}", taskId, routers);
        for (ConditionRouter branch : routers) {
            String switchCase = branch.getAction();
            if (isMatches(branch.getCondition())) {
                return Optional.ofNullable(switchCase);
            }
        }
        return Optional.empty();
    }

    /**
     * 把各谓词的左右表达式摊平到 Map，供 ParametersUtils 一次性替换占位符。
     */
    private Map<String, Object> getStringObjectMap(List<ConditionRouter> conditionRouters) {
        Map<String, Object> exprMap = new HashMap<>();
        for (int i = 0; i < conditionRouters.size(); i++) {
            ConditionRouter router = conditionRouters.get(i);
            ConditionRouter.ConditionGroup group = router.getCondition();
            List<ConditionRouter.ConditionPredicate> predicates = group.getChildren();
            for (int j = 0; j < predicates.size(); j++) {
                ConditionRouter.ConditionPredicate node = predicates.get(j);
                String key = getKey(i, j);
                exprMap.put(key + "_expr", node.getExpression());
                exprMap.put(key + "_val", node.getValue());
            }
        }
        return exprMap;
    }

    /**
     * 分支 i、谓词 j 在摊平 Map 中的键前缀。
     */
    private String getKey(int i, int j) {
        return "i_" + i + "__j_" + j;
    }

    private String asString(Object value) {
        if (value == null || value instanceof String) {
            return (String) value;
        }
        return String.valueOf(value);
    }

    /**
     * 按 AND/OR 验证是否匹配该条件分支。
     */
    private boolean isMatches(ConditionRouter.ConditionGroup conditionGroup) {
        if (conditionGroup.getOperator().isOr()) {
            for (ConditionRouter.ConditionPredicate child : conditionGroup.getChildren()) {
                if (evaluateCondition(child)) {
                    return true;
                }
            }
            return false;
        }
        for (ConditionRouter.ConditionPredicate child : conditionGroup.getChildren()) {
            if (!evaluateCondition(child)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 对单个谓词按运算符比较已解析后的左右值。
     */
    private boolean evaluateCondition(ConditionRouter.ConditionPredicate node) {
        String expression = node.getExpression();
        String value = node.getValue();
        OperatorEnum operator = node.getOperator();
        if (StringUtils.isEmpty(expression)) {
            return operator == OperatorEnum.EMPTY;
        }
        try {
            switch (operator) {
                case EQUALS:
                    return expression.equals(value);
                case NOT_EQUALS:
                    return !expression.equals(value);
                case GREATER_THAN:
                    return compareNumbers(expression, value) > 0;
                case GREATER_THAN_OR_EQUAL:
                    return compareNumbers(expression, value) >= 0;
                case LESS_THAN:
                    return compareNumbers(expression, value) < 0;
                case LESS_THAN_OR_EQUAL:
                    return compareNumbers(expression, value) <= 0;
                case EXISTS:
                    return include(expression, value);
                case NOT_EXISTS:
                    return StringUtils.isNotEmpty(expression);
                case EMPTY:
                    return StringUtils.isEmpty(expression);
                default:
                    LOGGER.warn("Unsupported operator type: {}", operator);
                    return false;
            }
        } catch (Exception e) {
            LOGGER.error("Error evaluating condition: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 判断左侧字符串是否包含右侧值（对应 EXISTS）。
     */
    private boolean include(String expression, String value) {
        if (StringUtils.isEmpty(value)) {
            return false;
        }
        return expression.contains(value);
    }

    /**
     * 按 double 比较大小，用于 &gt; / &gt;= / &lt; / &lt;=。
     */
    private int compareNumbers(String expression, String value) {
        double exprNum = Double.parseDouble(expression.trim());
        double valueNum = Double.parseDouble(value.trim());
        return Double.compare(exprNum, valueNum);
    }
}
