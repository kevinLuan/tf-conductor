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
package cn.taskflow.conductor.core.execution.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.core.dal.ExecutionDAOFacade;
import com.netflix.conductor.core.execution.WorkflowExecutor;
import com.netflix.conductor.core.utils.ParametersUtils;
import com.netflix.conductor.model.TaskModel;
import com.netflix.conductor.model.WorkflowModel;

import com.fasterxml.jackson.databind.ObjectMapper;

import static com.netflix.conductor.common.metadata.tasks.TaskType.TASK_TYPE_FOR;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ForTest {

    @Mock private ExecutionDAOFacade executionDAOFacade;
    @Mock private WorkflowExecutor workflowExecutor;

    private For forTask;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        forTask = new For(new ParametersUtils(new ObjectMapper()), executionDAOFacade);
    }

    @Test
    public void emptyElementsCompletesWithoutSchedulingBody() {
        WorkflowModel workflow = workflow();
        workflow.setInput(Map.of("items", List.of()));
        TaskModel task = forHead("${workflow.input.items}");
        workflow.getTasks().add(task);

        boolean changed = forTask.execute(workflow, task, workflowExecutor);

        assertTrue(changed);
        assertEquals(TaskModel.Status.COMPLETED, task.getStatus());
        assertEquals(0, task.getOutputData().get("iteration"));
        verify(workflowExecutor, never()).scheduleNextIteration(any(), any());
    }

    @Test
    public void nonEmptyElementsSchedulesFirstIterationWithLoopItem() {
        WorkflowModel workflow = workflow();
        workflow.setInput(Map.of("items", List.of("a", "b")));
        TaskModel task = forHead("${workflow.input.items}");
        workflow.getTasks().add(task);

        boolean changed = forTask.execute(workflow, task, workflowExecutor);

        assertTrue(changed);
        assertNotEquals(TaskModel.Status.COMPLETED, task.getStatus());
        assertEquals(1, task.getIteration());
        assertEquals("a", task.getOutputData().get("loopItem"));
        assertEquals(0, task.getOutputData().get("loopIndex"));
        verify(workflowExecutor, times(1)).scheduleNextIteration(any(), any());
    }

    @Test
    public void systemTaskTypeIsFor() {
        assertEquals(TASK_TYPE_FOR, forTask.getTaskType());
    }

    @Test
    public void forIsLoopAndBuiltInTask() {
        assertTrue(TaskType.isLoopTask(TASK_TYPE_FOR));
        assertTrue(TaskType.isBuiltIn(TASK_TYPE_FOR));
        assertFalse(TaskType.isLoopTask(TaskType.TASK_TYPE_SIMPLE));
    }

    @Test
    public void loopOverIsVisibleToGraphTraversal() {
        WorkflowTask body = new WorkflowTask();
        body.setName("body");
        body.setTaskReferenceName("body_ref");
        body.setType(TaskType.SIMPLE.name());

        WorkflowTask head = new WorkflowTask();
        head.setName("for");
        head.setTaskReferenceName("for_ref");
        head.setType(TASK_TYPE_FOR);
        head.setLoopOver(List.of(body));

        assertTrue(head.has("body_ref"));
        assertEquals(body, head.get("body_ref"));
        assertEquals(head, head.next("body_ref", null));
    }

    private static WorkflowModel workflow() {
        WorkflowModel workflow = new WorkflowModel();
        WorkflowDef def = new WorkflowDef();
        def.setName("for-test");
        def.setVersion(1);
        workflow.setWorkflowDefinition(def);
        workflow.setWorkflowId("for-wf");
        workflow.setTasks(new ArrayList<>());
        return workflow;
    }

    private static TaskModel forHead(String elementsExpression) {
        TaskModel task = new TaskModel();
        task.setTaskId("for-task");
        task.setReferenceTaskName("for_ref");
        task.setTaskType(TASK_TYPE_FOR);
        task.setStatus(TaskModel.Status.IN_PROGRESS);

        WorkflowTask body = new WorkflowTask();
        body.setTaskReferenceName("body_ref");
        body.setType("SIMPLE");

        WorkflowTask workflowTask = new WorkflowTask();
        workflowTask.setName("for");
        workflowTask.setTaskReferenceName("for_ref");
        workflowTask.setType(TASK_TYPE_FOR);
        workflowTask.setLoopOver(List.of(body));
        Map<String, Object> input = new HashMap<>();
        input.put("items", elementsExpression);
        workflowTask.setInputParameters(input);
        task.setWorkflowTask(workflowTask);
        return task;
    }
}
