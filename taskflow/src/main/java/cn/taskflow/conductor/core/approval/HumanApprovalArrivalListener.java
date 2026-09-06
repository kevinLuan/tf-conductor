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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.core.listener.TaskStatusListener;
import com.netflix.conductor.model.TaskModel;

/**
 * 订阅 HUMAN 进入 IN_PROGRESS，触发轻量建单。
 *
 * <p>不注册为 {@code @Primary}：AI 开启时由 agentspan 的 composite 扇出；关闭时由 {@link
 * TaskflowTaskStatusListenerConfiguration} 扇出。
 */
@Component
@ConditionalOnProperty(
        name = "conductor.taskflow.approval.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class HumanApprovalArrivalListener implements TaskStatusListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HumanApprovalArrivalListener.class);

    private final HumanApprovalService humanApprovalService;

    public HumanApprovalArrivalListener(HumanApprovalService humanApprovalService) {
        this.humanApprovalService = humanApprovalService;
    }

    /** 仅处理 HUMAN；建单失败只记日志，不阻断工作流。 */
    @Override
    public void onTaskInProgress(TaskModel task) {
        if (task == null || !TaskType.TASK_TYPE_HUMAN.equals(task.getTaskType())) {
            return;
        }
        try {
            humanApprovalService.onHumanArrived(task);
        } catch (Exception e) {
            LOGGER.error(
                    "创建审批单失败 taskId={} workflowId={}",
                    task.getTaskId(),
                    task.getWorkflowInstanceId(),
                    e);
        }
    }
}
