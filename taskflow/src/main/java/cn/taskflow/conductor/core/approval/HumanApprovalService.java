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
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.netflix.conductor.core.dal.ExecutionDAOFacade;
import com.netflix.conductor.model.TaskModel;
import com.netflix.conductor.model.WorkflowModel;

import cn.taskflow.conductor.core.model.approval.ApprovalForm;
import cn.taskflow.conductor.core.model.approval.ApprovalFormRequest;
import cn.taskflow.conductor.core.model.approval.HumanTaskDefinition;
import cn.taskflow.conductor.core.model.approval.UserFormTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

/** HUMAN 到达后根据 {@code __humanTaskDefinition} 组装建单请求。无该字段的 HUMAN 不当审批单。 */
@Service
public class HumanApprovalService {

    static final String DEF_KEY = "__humanTaskDefinition";
    static final String PROCESS_CTX_KEY = "__humanTaskProcessContext";
    static final String CREATED_BY_KEY = "_createdBy";

    private static final Set<String> RESERVED_KEYS =
            Set.of(DEF_KEY, PROCESS_CTX_KEY, CREATED_BY_KEY);
    private static final Logger LOGGER = LoggerFactory.getLogger(HumanApprovalService.class);

    private final ApprovalFormClient approvalFormClient;
    private final ExecutionDAOFacade executionDAOFacade;
    private final ObjectMapper objectMapper;

    public HumanApprovalService(
            ApprovalFormClient approvalFormClient,
            ExecutionDAOFacade executionDAOFacade,
            ObjectMapper objectMapper) {
        this.approvalFormClient = approvalFormClient;
        this.executionDAOFacade = executionDAOFacade;
        this.objectMapper = objectMapper;
    }

    /** HUMAN 到达后建审批单。无 {@code __humanTaskDefinition} 的任务跳过；成功则把 {@code formId} 写回 output。 */
    public void onHumanArrived(TaskModel task) {
        Map<String, Object> input = task.getInputData();
        if (input == null || !input.containsKey(DEF_KEY)) {
            return;
        }
        HumanTaskDefinition definition = parseDefinition(input.get(DEF_KEY));
        if (definition == null) {
            LOGGER.warn("HUMAN task {} 的 __humanTaskDefinition 无法解析，跳过建单", task.getTaskId());
            return;
        }
        UserFormTemplate template = definition.getUserFormTemplate();
        WorkflowModel workflow = loadWorkflow(task);
        ApprovalFormRequest request =
                ApprovalFormRequest.builder()
                        .tenantId(resolveTenantId(workflow))
                        .workflowId(task.getWorkflowInstanceId())
                        .taskId(task.getTaskId())
                        .taskRefName(task.getReferenceTaskName())
                        .approver(resolveApprover(task, definition, input, workflow))
                        .displayName(definition.getDisplayName())
                        .formTemplateName(template != null ? template.getName() : null)
                        .formTemplateVersion(template != null ? template.getVersion() : null)
                        .formData(formData(input))
                        .build();
        ApprovalForm form = approvalFormClient.create(request);
        if (form != null && StringUtils.isNotBlank(form.getFormId())) {
            task.addOutput("formId", form.getFormId());
            executionDAOFacade.updateTask(task);
        }
    }

    /** 把 input 中的定义对象转成 {@link HumanTaskDefinition}，解析失败返回 null。 */
    HumanTaskDefinition parseDefinition(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(raw, HumanTaskDefinition.class);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("解析 __humanTaskDefinition 失败: {}", e.getMessage());
            return null;
        }
    }

    /** 审批人回落：assignee → owner → input {@code _createdBy} → 工作流 createdBy → {@code unknown}。 */
    String resolveApprover(
            TaskModel task,
            HumanTaskDefinition definition,
            Map<String, Object> input,
            WorkflowModel workflow) {
        if (definition != null && StringUtils.isNotBlank(definition.getAssignee())) {
            return definition.getAssignee();
        }
        if (definition != null && StringUtils.isNotBlank(definition.getOwner())) {
            return definition.getOwner();
        }
        Object createdBy = input.get(CREATED_BY_KEY);
        if (createdBy != null && StringUtils.isNotBlank(createdBy.toString())) {
            return createdBy.toString();
        }
        if (workflow != null && StringUtils.isNotBlank(workflow.getCreatedBy())) {
            return workflow.getCreatedBy();
        }
        return "unknown";
    }

    /** 租户键取工作流或定义上的 {@code ownerApp}。 */
    String resolveTenantId(WorkflowModel workflow) {
        if (workflow == null) {
            return null;
        }
        if (StringUtils.isNotBlank(workflow.getOwnerApp())) {
            return workflow.getOwnerApp();
        }
        if (workflow.getWorkflowDefinition() != null
                && StringUtils.isNotBlank(workflow.getWorkflowDefinition().getOwnerApp())) {
            return workflow.getWorkflowDefinition().getOwnerApp();
        }
        return null;
    }

    /** 读工作流上下文；失败不影响建单，租户/创建人回落为空。 */
    private WorkflowModel loadWorkflow(TaskModel task) {
        try {
            return executionDAOFacade.getWorkflowModel(task.getWorkflowInstanceId(), false);
        } catch (Exception e) {
            LOGGER.debug("读取工作流失败 workflowId={}", task.getWorkflowInstanceId(), e);
            return null;
        }
    }

    /** 表单数据 = 任务 input 去掉内部保留字段。 */
    Map<String, Object> formData(Map<String, Object> input) {
        Map<String, Object> data = new LinkedHashMap<>();
        input.forEach(
                (key, value) -> {
                    if (!RESERVED_KEYS.contains(key)) {
                        data.put(key, value);
                    }
                });
        return data;
    }
}
