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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.core.execution.DeciderService;
import com.netflix.conductor.core.execution.mapper.TaskMapperContext;
import com.netflix.conductor.core.utils.IDGenerator;
import com.netflix.conductor.core.utils.ParametersUtils;
import com.netflix.conductor.dao.MetadataDAO;
import com.netflix.conductor.model.TaskModel;
import com.netflix.conductor.model.WorkflowModel;

import com.fasterxml.jackson.databind.ObjectMapper;

import static com.netflix.conductor.common.metadata.tasks.TaskType.TASK_TYPE_FOR;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ForTaskMapperTest {

    private TaskMapperContext taskMapperContext;
    private MetadataDAO metadataDAO;
    private ParametersUtils parametersUtils;

    @Before
    public void setup() {
        WorkflowTask workflowTask = new WorkflowTask();
        workflowTask.setType(TASK_TYPE_FOR);
        workflowTask.setName("for");
        workflowTask.setTaskReferenceName("for_ref");
        workflowTask.setInputParameters(Map.of("items", "${workflow.input.foo}"));
        WorkflowTask body = new WorkflowTask();
        body.setTaskReferenceName("body_ref");
        workflowTask.setLoopOver(List.of(body));

        WorkflowModel workflow = new WorkflowModel();
        workflow.setWorkflowDefinition(new WorkflowDef());
        workflow.setInput(Map.of("foo", "bar"));

        taskMapperContext =
                TaskMapperContext.newBuilder()
                        .withDeciderService(Mockito.mock(DeciderService.class))
                        .withWorkflowModel(workflow)
                        .withTaskDefinition(new TaskDef())
                        .withWorkflowTask(workflowTask)
                        .withRetryCount(0)
                        .withTaskId(new IDGenerator().generate())
                        .build();

        metadataDAO = Mockito.mock(MetadataDAO.class);
        parametersUtils = new ParametersUtils(new ObjectMapper());
    }

    @Test
    public void mapsForHeadOnly() {
        List<TaskModel> mapped =
                new ForTaskMapper(metadataDAO, parametersUtils).getMappedTasks(taskMapperContext);

        assertNotNull(mapped);
        assertEquals(1, mapped.size());
        assertEquals(TASK_TYPE_FOR, mapped.get(0).getTaskType());
        assertEquals(TaskModel.Status.IN_PROGRESS, mapped.get(0).getStatus());
        assertEquals(Map.of("items", "bar"), mapped.get(0).getInputData());
    }

    @Test
    public void reportsForTaskType() {
        assertEquals(TASK_TYPE_FOR, new ForTaskMapper(metadataDAO, parametersUtils).getTaskType());
    }
}
