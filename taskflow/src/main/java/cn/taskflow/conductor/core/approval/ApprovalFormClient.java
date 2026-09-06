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

import cn.taskflow.conductor.core.model.approval.ApprovalForm;
import cn.taskflow.conductor.core.model.approval.ApprovalFormRequest;

/** 审批单创建口。{@code mock} 为内存实现；{@code http} 调用 taskflow。监听器无需改。 */
public interface ApprovalFormClient {

    /** 按 taskId 创建审批单；同一 taskId 应幂等。 */
    ApprovalForm create(ApprovalFormRequest request);
}
