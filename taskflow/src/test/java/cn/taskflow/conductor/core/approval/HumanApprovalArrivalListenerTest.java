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

import org.junit.Test;

import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.model.TaskModel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class HumanApprovalArrivalListenerTest {

    @Test
    public void ignoresNonHumanTasks() {
        HumanApprovalService service = mock(HumanApprovalService.class);
        HumanApprovalArrivalListener listener = new HumanApprovalArrivalListener(service);
        TaskModel simple = new TaskModel();
        simple.setTaskType(TaskType.TASK_TYPE_SIMPLE);
        listener.onTaskInProgress(simple);
        verify(service, never()).onHumanArrived(simple);
    }

    @Test
    public void forwardsHumanTasks() {
        HumanApprovalService service = mock(HumanApprovalService.class);
        HumanApprovalArrivalListener listener = new HumanApprovalArrivalListener(service);
        TaskModel human = new TaskModel();
        human.setTaskType(TaskType.TASK_TYPE_HUMAN);
        listener.onTaskInProgress(human);
        verify(service).onHumanArrived(human);
    }
}
