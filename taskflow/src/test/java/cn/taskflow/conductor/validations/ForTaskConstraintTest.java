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
package cn.taskflow.conductor.validations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.dao.MetadataDAO;
import com.netflix.conductor.validations.ValidationContext;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class ForTaskConstraintTest {

    private static Validator validator;
    private static ValidatorFactory validatorFactory;
    private MetadataDAO mockMetadataDao;

    @BeforeClass
    public static void init() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterClass
    public static void close() {
        validatorFactory.close();
    }

    @Before
    public void setUp() {
        mockMetadataDao = Mockito.mock(MetadataDAO.class);
        ValidationContext.initialize(mockMetadataDao);
    }

    @Test
    public void missingItemsAndLoopOverFails() {
        WorkflowTask workflowTask = sampleForTask();
        when(mockMetadataDao.getTaskDef(anyString())).thenReturn(new TaskDef());

        Set<ConstraintViolation<WorkflowTask>> result = validator.validate(workflowTask);
        List<String> errors = new ArrayList<>();
        result.forEach(e -> errors.add(e.getMessage()));

        assertTrue(errors.contains("items field is required for taskType: FOR taskName: encode"));
        assertTrue(
                errors.contains("loopOver field is required for taskType: FOR taskName: encode"));
        assertEquals(2, result.size());
    }

    @Test
    public void itemsAndLoopOverIsValid() {
        WorkflowTask workflowTask = sampleForTask();
        workflowTask.getInputParameters().put("items", "${workflow.input.items}");
        WorkflowTask body = new WorkflowTask();
        body.setName("body");
        body.setTaskReferenceName("body_ref");
        body.setType("SIMPLE");
        workflowTask.setLoopOver(List.of(body));
        when(mockMetadataDao.getTaskDef(anyString())).thenReturn(new TaskDef());

        assertEquals(0, validator.validate(workflowTask).size());
    }

    private static WorkflowTask sampleForTask() {
        WorkflowTask workflowTask = new WorkflowTask();
        workflowTask.setName("encode");
        workflowTask.setTaskReferenceName("encode");
        workflowTask.setType("FOR");
        workflowTask.setInputParameters(new HashMap<>());
        return workflowTask;
    }
}
