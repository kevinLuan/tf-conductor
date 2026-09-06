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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cn.taskflow.conductor.core.model.approval.ApprovalForm;
import cn.taskflow.conductor.core.model.approval.ApprovalFormRequest;

/** 内存 MOCK：按 taskId 幂等建单，只打日志。后续用远程实现替换本 Bean。 */
@Component
@ConditionalOnProperty(
        name = "conductor.taskflow.approval.client",
        havingValue = "mock",
        matchIfMissing = true)
public class MockApprovalFormClient implements ApprovalFormClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockApprovalFormClient.class);

    private final Map<String, ApprovalForm> created = new ConcurrentHashMap<>();

    /** 内存建单：formId 为 {@code mock-{taskId}}，同 taskId 只建一次。 */
    @Override
    public ApprovalForm create(ApprovalFormRequest request) {
        return created.computeIfAbsent(
                request.getTaskId(),
                taskId -> {
                    ApprovalForm form =
                            ApprovalForm.builder()
                                    .formId("mock-" + taskId)
                                    .workflowId(request.getWorkflowId())
                                    .taskId(taskId)
                                    .approver(request.getApprover())
                                    .build();
                    LOGGER.info(
                            "MOCK 创建审批单 formId={}, workflowId={}, taskId={}, approver={}, template={}/{}, displayName={}",
                            form.getFormId(),
                            request.getWorkflowId(),
                            taskId,
                            request.getApprover(),
                            request.getFormTemplateName(),
                            request.getFormTemplateVersion(),
                            request.getDisplayName());
                    return form;
                });
    }

    /** 测试用：已创建的审批单。 */
    public Map<String, ApprovalForm> createdForms() {
        return Map.copyOf(created);
    }
}
