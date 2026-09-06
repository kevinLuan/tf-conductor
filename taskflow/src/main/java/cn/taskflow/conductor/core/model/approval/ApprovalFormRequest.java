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
package cn.taskflow.conductor.core.model.approval;

import java.util.Map;

import lombok.Builder;
import lombok.Value;

/** 创建审批单请求。后续远程接口可直接复用此结构。 */
@Value
@Builder
public class ApprovalFormRequest {
    /** 工作流 ownerApp，对应 taskflow 租户键。 */
    String tenantId;

    String workflowId;
    String taskId;
    String taskRefName;
    String approver;
    String displayName;
    String formTemplateName;
    Integer formTemplateVersion;
    Map<String, Object> formData;
}
