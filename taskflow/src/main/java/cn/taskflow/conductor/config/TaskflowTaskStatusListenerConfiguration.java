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
package cn.taskflow.conductor.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.netflix.conductor.core.listener.TaskStatusListener;
import com.netflix.conductor.model.TaskModel;

/**
 * AI 关闭时 Conductor 只注入一个 {@link TaskStatusListener}。本 composite 作为 {@code @Primary}，把 stub 与 {@link
 * cn.taskflow.conductor.core.approval.HumanApprovalArrivalListener} 一起扇出。
 *
 * <p>AI 开启时 agentspan 已有 composite，本配置不生效，避免两个 Primary。
 */
@Configuration
@ConditionalOnProperty(
        name = "conductor.integrations.ai.enabled",
        havingValue = "false",
        matchIfMissing = true)
public class TaskflowTaskStatusListenerConfiguration {

    /** AI 关闭时作为唯一 Primary，把内置 stub 与 HUMAN 建单监听器一起扇出。 */
    @Bean
    @Primary
    public TaskStatusListener taskflowCompositeTaskStatusListener(
            ObjectProvider<TaskStatusListener> listeners) {
        return new TaskflowCompositeTaskStatusListener(listeners);
    }

    static final class TaskflowCompositeTaskStatusListener implements TaskStatusListener {
        private final ObjectProvider<TaskStatusListener> delegates;

        TaskflowCompositeTaskStatusListener(ObjectProvider<TaskStatusListener> delegates) {
            this.delegates = delegates;
        }

        /** 跳过自身，避免递归；单个监听器异常不影响其余。 */
        private void fanOut(java.util.function.Consumer<TaskStatusListener> action) {
            delegates.stream()
                    .filter(listener -> listener != this)
                    .forEach(
                            listener -> {
                                try {
                                    action.accept(listener);
                                } catch (Exception ignored) {
                                    // 单个监听器失败不影响其余
                                }
                            });
        }

        @Override
        public void onTaskScheduledIfEnabled(TaskModel task) {
            fanOut(l -> l.onTaskScheduledIfEnabled(task));
        }

        @Override
        public void onTaskInProgressIfEnabled(TaskModel task) {
            fanOut(l -> l.onTaskInProgressIfEnabled(task));
        }

        @Override
        public void onTaskCanceledIfEnabled(TaskModel task) {
            fanOut(l -> l.onTaskCanceledIfEnabled(task));
        }

        @Override
        public void onTaskFailedIfEnabled(TaskModel task) {
            fanOut(l -> l.onTaskFailedIfEnabled(task));
        }

        @Override
        public void onTaskFailedWithTerminalErrorIfEnabled(TaskModel task) {
            fanOut(l -> l.onTaskFailedWithTerminalErrorIfEnabled(task));
        }

        @Override
        public void onTaskCompletedIfEnabled(TaskModel task) {
            fanOut(l -> l.onTaskCompletedIfEnabled(task));
        }

        @Override
        public void onTaskCompletedWithErrorsIfEnabled(TaskModel task) {
            fanOut(l -> l.onTaskCompletedWithErrorsIfEnabled(task));
        }

        @Override
        public void onTaskTimedOutIfEnabled(TaskModel task) {
            fanOut(l -> l.onTaskTimedOutIfEnabled(task));
        }

        @Override
        public void onTaskSkippedIfEnabled(TaskModel task) {
            fanOut(l -> l.onTaskSkippedIfEnabled(task));
        }

        @Override
        public void onTaskScheduled(TaskModel task) {
            fanOut(l -> l.onTaskScheduled(task));
        }

        @Override
        public void onTaskInProgress(TaskModel task) {
            fanOut(l -> l.onTaskInProgress(task));
        }

        @Override
        public void onTaskCanceled(TaskModel task) {
            fanOut(l -> l.onTaskCanceled(task));
        }

        @Override
        public void onTaskFailed(TaskModel task) {
            fanOut(l -> l.onTaskFailed(task));
        }

        @Override
        public void onTaskFailedWithTerminalError(TaskModel task) {
            fanOut(l -> l.onTaskFailedWithTerminalError(task));
        }

        @Override
        public void onTaskCompleted(TaskModel task) {
            fanOut(l -> l.onTaskCompleted(task));
        }

        @Override
        public void onTaskCompletedWithErrors(TaskModel task) {
            fanOut(l -> l.onTaskCompletedWithErrors(task));
        }

        @Override
        public void onTaskTimedOut(TaskModel task) {
            fanOut(l -> l.onTaskTimedOut(task));
        }

        @Override
        public void onTaskSkipped(TaskModel task) {
            fanOut(l -> l.onTaskSkipped(task));
        }
    }
}
