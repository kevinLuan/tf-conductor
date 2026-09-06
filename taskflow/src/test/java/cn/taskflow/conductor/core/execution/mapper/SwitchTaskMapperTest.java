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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.DefaultResourceLoader;

import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.core.execution.DeciderService;
import com.netflix.conductor.core.execution.evaluators.Evaluator;
import com.netflix.conductor.core.execution.mapper.SwitchTaskMapper;
import com.netflix.conductor.core.execution.mapper.TaskMapperContext;
import com.netflix.conductor.core.utils.IDGenerator;
import com.netflix.conductor.core.utils.ParametersUtils;
import com.netflix.conductor.model.TaskModel;
import com.netflix.conductor.model.WorkflowModel;

import cn.taskflow.conductor.core.execution.evaluators.AIEvaluator;
import cn.taskflow.conductor.core.execution.evaluators.AIFactory;
import cn.taskflow.conductor.core.execution.evaluators.IfElseEvaluator;
import cn.taskflow.conductor.core.model.SwitchAiParams;
import cn.taskflow.conductor.core.model.condition.ConditionRouter;
import cn.taskflow.conductor.core.model.condition.LogicOperatorEnum;
import cn.taskflow.conductor.core.model.condition.OperatorEnum;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SwitchTaskMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private IfElseEvaluator ifElseEvaluator;
    private AIEvaluator aiEvaluator;
    private DeciderService deciderService;
    private IDGenerator idGenerator;

    @Before
    public void setUp() throws Exception {
        ifElseEvaluator = new IfElseEvaluator(objectMapper, new ParametersUtils(objectMapper));
        AIFactory aiFactory =
                new AIFactory(query -> "{\"id\": 2}", objectMapper, new DefaultResourceLoader());
        aiEvaluator = new AIEvaluator(objectMapper, aiFactory);
        deciderService = mock(DeciderService.class);
        idGenerator = new IDGenerator();
    }

    @Test
    public void ifElseEvaluatorSelectsMatchingCase() throws Exception {
        WorkflowTask pass = branch("pass_ref");
        WorkflowTask fallback = branch("default_ref");
        WorkflowTask switchTask =
                switchTask(
                        IfElseEvaluator.NAME,
                        ifElseExpression("${workflow.input.status}", "approved", "pass"),
                        Map.of("pass", List.of(pass)),
                        List.of(fallback));

        WorkflowModel workflow = workflowWithInput(Map.of("status", "approved"));
        TaskModel scheduled = scheduled("pass_body");
        when(deciderService.getTasksToBeScheduled(workflow, pass, 0, null))
                .thenReturn(List.of(scheduled));

        List<TaskModel> mapped =
                mapper(IfElseEvaluator.NAME, ifElseEvaluator)
                        .getMappedTasks(context(workflow, switchTask));

        assertEquals(2, mapped.size());
        assertEquals("switchTask", mapped.get(0).getReferenceTaskName());
        assertEquals("pass", mapped.get(0).getOutputData().get("selectedCase"));
        assertEquals("pass_body", mapped.get(1).getReferenceTaskName());
    }

    @Test
    public void ifElseEvaluatorFallsBackToDefaultWhenUnmatched() throws Exception {
        WorkflowTask pass = branch("pass_ref");
        WorkflowTask fallback = branch("default_ref");
        WorkflowTask switchTask =
                switchTask(
                        IfElseEvaluator.NAME,
                        ifElseExpression("${workflow.input.status}", "approved", "pass"),
                        Map.of("pass", List.of(pass)),
                        List.of(fallback));

        WorkflowModel workflow = workflowWithInput(Map.of("status", "rejected"));
        TaskModel scheduled = scheduled("default_body");
        when(deciderService.getTasksToBeScheduled(workflow, fallback, 0, null))
                .thenReturn(List.of(scheduled));

        List<TaskModel> mapped =
                mapper(IfElseEvaluator.NAME, ifElseEvaluator)
                        .getMappedTasks(context(workflow, switchTask));

        assertEquals(2, mapped.size());
        assertEquals("", mapped.get(0).getOutputData().get("selectedCase"));
        assertEquals("default_body", mapped.get(1).getReferenceTaskName());
    }

    @Test
    public void aiEvaluatorSelectsCaseByReturnedId() throws Exception {
        WorkflowTask refund = branch("refund_ref");
        WorkflowTask consult = branch("consult_ref");
        Map<String, List<WorkflowTask>> cases = new LinkedHashMap<>();
        cases.put("refund", List.of(refund));
        cases.put("consult", List.of(consult));

        SwitchAiParams params = new SwitchAiParams();
        params.setInstructions("按意图分类");
        WorkflowTask switchTask =
                switchTask(
                        AIEvaluator.NAME,
                        objectMapper.writeValueAsString(params),
                        cases,
                        List.of());

        WorkflowModel workflow = workflowWithInput(Map.of("q", "怎么退货"));
        TaskModel scheduled = scheduled("consult_body");
        when(deciderService.getTasksToBeScheduled(workflow, consult, 0, null))
                .thenReturn(List.of(scheduled));

        List<TaskModel> mapped =
                mapper(AIEvaluator.NAME, aiEvaluator)
                        .getMappedTasks(context(workflow, switchTask, Map.of("q", "怎么退货")));

        assertEquals(2, mapped.size());
        assertEquals("consult", mapped.get(0).getOutputData().get("selectedCase"));
        assertEquals("consult_body", mapped.get(1).getReferenceTaskName());
    }

    private SwitchTaskMapper mapper(String name, Evaluator evaluator) {
        return new SwitchTaskMapper(Map.of(name, evaluator));
    }

    private TaskMapperContext context(WorkflowModel workflow, WorkflowTask switchTask) {
        return context(workflow, switchTask, Collections.emptyMap());
    }

    private TaskMapperContext context(
            WorkflowModel workflow, WorkflowTask switchTask, Map<String, Object> taskInput) {
        return TaskMapperContext.newBuilder()
                .withWorkflowModel(workflow)
                .withWorkflowTask(switchTask)
                .withTaskInput(taskInput)
                .withRetryCount(0)
                .withTaskId(idGenerator.generate())
                .withDeciderService(deciderService)
                .build();
    }

    private static WorkflowTask switchTask(
            String evaluatorType,
            String expression,
            Map<String, List<WorkflowTask>> decisionCases,
            List<WorkflowTask> defaultCase) {
        WorkflowTask switchTask = new WorkflowTask();
        switchTask.setType(TaskType.SWITCH.name());
        switchTask.setName("Switch");
        switchTask.setTaskReferenceName("switchTask");
        switchTask.setEvaluatorType(evaluatorType);
        switchTask.setExpression(expression);
        switchTask.setDecisionCases(decisionCases);
        switchTask.setDefaultCase(defaultCase);
        return switchTask;
    }

    private static WorkflowTask branch(String ref) {
        WorkflowTask task = new WorkflowTask();
        task.setName(ref);
        task.setTaskReferenceName(ref);
        return task;
    }

    private TaskModel scheduled(String ref) {
        TaskModel task = new TaskModel();
        task.setReferenceTaskName(ref);
        task.setTaskId(idGenerator.generate());
        return task;
    }

    private String ifElseExpression(String left, String right, String action) throws Exception {
        ConditionRouter.ConditionGroup logic =
                new ConditionRouter.ConditionGroup(LogicOperatorEnum.AND);
        logic.addChild(new ConditionRouter.ConditionPredicate(left, OperatorEnum.EQUALS, right));
        return objectMapper.writeValueAsString(List.of(new ConditionRouter(logic, action)));
    }

    private static WorkflowModel workflowWithInput(Map<String, Object> input) {
        WorkflowDef def = new WorkflowDef();
        def.setSchemaVersion(2);
        WorkflowModel workflow = new WorkflowModel();
        workflow.setWorkflowDefinition(def);
        workflow.setInput(new HashMap<>(input));
        return workflow;
    }
}
