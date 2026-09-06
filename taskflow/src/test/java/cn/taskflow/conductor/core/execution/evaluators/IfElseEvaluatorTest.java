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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.core.utils.ParametersUtils;
import com.netflix.conductor.model.WorkflowModel;

import cn.taskflow.conductor.core.model.condition.ConditionRouter;
import cn.taskflow.conductor.core.model.condition.LogicOperatorEnum;
import cn.taskflow.conductor.core.model.condition.OperatorEnum;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IfElseEvaluatorTest {

    private IfElseEvaluator evaluator;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper();
        evaluator = new IfElseEvaluator(objectMapper, new ParametersUtils(objectMapper));
    }

    @Test
    public void matchesEqualsBranchAfterPlaceholderResolution() throws Exception {
        ConditionRouter.ConditionGroup logic =
                new ConditionRouter.ConditionGroup(LogicOperatorEnum.AND);
        logic.addChild(
                new ConditionRouter.ConditionPredicate(
                        "${workflow.input.status}", OperatorEnum.EQUALS, "approved"));
        String expression =
                objectMapper.writeValueAsString(List.of(new ConditionRouter(logic, "pass")));

        Optional<Object> result =
                evaluator.evaluate(
                        workflowWithInput(Map.of("status", "approved")),
                        new WorkflowTask(),
                        "task-1",
                        expression,
                        Collections.emptyMap());

        assertTrue(result.isPresent());
        assertEquals("pass", result.get());
    }

    @Test
    public void unmatchedBranchReturnsEmpty() throws Exception {
        ConditionRouter.ConditionGroup logic =
                new ConditionRouter.ConditionGroup(LogicOperatorEnum.AND);
        logic.addChild(new ConditionRouter.ConditionPredicate("x", OperatorEnum.EQUALS, "y"));
        String expression =
                objectMapper.writeValueAsString(List.of(new ConditionRouter(logic, "pass")));

        Optional<Object> result =
                evaluator.evaluate(
                        workflowWithInput(Collections.emptyMap()),
                        new WorkflowTask(),
                        "task-1",
                        expression,
                        Collections.emptyMap());

        assertFalse(result.isPresent());
    }

    @Test
    public void orLogicMatchesIfAnyChildSucceeds() throws Exception {
        ConditionRouter.ConditionGroup logic =
                new ConditionRouter.ConditionGroup(LogicOperatorEnum.OR);
        logic.addChild(new ConditionRouter.ConditionPredicate("a", OperatorEnum.EQUALS, "no"));
        logic.addChild(
                new ConditionRouter.ConditionPredicate("10", OperatorEnum.GREATER_THAN, "3"));
        String expression =
                objectMapper.writeValueAsString(List.of(new ConditionRouter(logic, "ok")));

        Optional<Object> result =
                evaluator.evaluate(
                        workflowWithInput(Collections.emptyMap()),
                        new WorkflowTask(),
                        "task-1",
                        expression,
                        Collections.emptyMap());

        assertEquals("ok", result.orElse(null));
    }

    private WorkflowModel workflowWithInput(Map<String, Object> input) {
        WorkflowDef def = new WorkflowDef();
        def.setSchemaVersion(2);
        WorkflowModel workflow = new WorkflowModel();
        workflow.setWorkflowDefinition(def);
        workflow.setInput(new HashMap<>(input));
        return workflow;
    }
}
