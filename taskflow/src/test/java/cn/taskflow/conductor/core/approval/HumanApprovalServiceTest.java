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
package cn.taskflow.conductor.core.approval;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.core.dal.ExecutionDAOFacade;
import com.netflix.conductor.model.TaskModel;
import com.netflix.conductor.model.WorkflowModel;

import cn.taskflow.conductor.core.model.approval.ApprovalForm;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HumanApprovalServiceTest {

    private MockApprovalFormClient client;
    private ExecutionDAOFacade executionDAOFacade;
    private HumanApprovalService service;

    @Before
    public void setUp() {
        client = new MockApprovalFormClient();
        executionDAOFacade = mock(ExecutionDAOFacade.class);
        service = new HumanApprovalService(client, executionDAOFacade, new ObjectMapper());
    }

    @Test
    public void skipsWhenDefinitionMissing() {
        TaskModel task = humanTask(Map.of("amount", 1));
        service.onHumanArrived(task);
        assertTrue(client.createdForms().isEmpty());
    }

    @Test
    public void createsFormForAssigneeAndTemplate() {
        WorkflowModel workflow = new WorkflowModel();
        workflow.setOwnerApp("app-9");
        when(executionDAOFacade.getWorkflowModel("wf-1", false)).thenReturn(workflow);
        TaskModel task =
                humanTask(
                        Map.of(
                                HumanApprovalService.DEF_KEY,
                                Map.of(
                                        "displayName",
                                        "测试审批单",
                                        "assignee",
                                        "user-1",
                                        "owner",
                                        "kevin@example.com",
                                        "userFormTemplate",
                                        Map.of("name", "test_form", "version", 1)),
                                "field_number_tizdhegn2g",
                                0,
                                HumanApprovalService.CREATED_BY_KEY,
                                "ignored"));

        service.onHumanArrived(task);

        ApprovalForm form = client.createdForms().get("task-1");
        assertEquals("mock-task-1", form.getFormId());
        assertEquals("user-1", form.getApprover());
        assertEquals("wf-1", form.getWorkflowId());
        assertEquals("mock-task-1", task.getOutputData().get("formId"));
    }

    @Test
    public void approverFallsBackToOwnerThenCreatedByThenWorkflowOwner() {
        TaskModel owned =
                humanTask(
                        Map.of(
                                HumanApprovalService.DEF_KEY,
                                Map.of("displayName", "单", "owner", "from-owner")));
        service.onHumanArrived(owned);
        assertEquals("from-owner", client.createdForms().get("task-1").getApprover());
    }

    @Test
    public void approverFallsBackToCreatedByThenWorkflowOwner() {
        TaskModel task =
                humanTask(
                        Map.of(
                                HumanApprovalService.DEF_KEY,
                                Map.of("displayName", "单"),
                                HumanApprovalService.CREATED_BY_KEY,
                                "from-input"));
        service.onHumanArrived(task);
        assertEquals("from-input", client.createdForms().get("task-1").getApprover());

        TaskModel task2 =
                humanTask(Map.of(HumanApprovalService.DEF_KEY, Map.of("displayName", "单")));
        task2.setTaskId("task-2");
        WorkflowModel workflow = new WorkflowModel();
        workflow.setCreatedBy("wf-owner");
        when(executionDAOFacade.getWorkflowModel("wf-1", false)).thenReturn(workflow);
        service.onHumanArrived(task2);
        assertEquals("wf-owner", client.createdForms().get("task-2").getApprover());
    }

    @Test
    public void formDataOmitsInternalKeys() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put(HumanApprovalService.DEF_KEY, Map.of("displayName", "单"));
        input.put(HumanApprovalService.PROCESS_CTX_KEY, Map.of("state", "IN_PROGRESS"));
        input.put(HumanApprovalService.CREATED_BY_KEY, "a@b.com");
        input.put("field_date_mqzhlb47z", 3);
        Map<String, Object> data = service.formData(input);
        assertEquals(Map.of("field_date_mqzhlb47z", 3), data);
    }

    @Test
    public void createIsIdempotentByTaskId() {
        TaskModel task = humanTask(Map.of(HumanApprovalService.DEF_KEY, Map.of("owner", "a")));
        service.onHumanArrived(task);
        service.onHumanArrived(task);
        assertEquals(1, client.createdForms().size());
    }

    private static TaskModel humanTask(Map<String, Object> input) {
        TaskModel task = new TaskModel();
        task.setTaskType(TaskType.TASK_TYPE_HUMAN);
        task.setTaskId("task-1");
        task.setWorkflowInstanceId("wf-1");
        task.setReferenceTaskName("human_ref");
        task.setInputData(new LinkedHashMap<>(input));
        return task;
    }
}
