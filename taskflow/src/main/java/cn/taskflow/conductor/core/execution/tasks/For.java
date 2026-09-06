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
package cn.taskflow.conductor.core.execution.tasks;

import org.springframework.stereotype.Component;

import com.netflix.conductor.core.dal.ExecutionDAOFacade;
import com.netflix.conductor.core.execution.tasks.DoWhile;
import com.netflix.conductor.core.utils.ParametersUtils;

import static com.netflix.conductor.common.metadata.tasks.TaskType.TASK_TYPE_FOR;

/**
 * FOR 循环壳：只换任务类型，列表迭代完全复用 {@link DoWhile}（{@code items} → {@code loopItem} / {@code loopIndex}）。
 */
@Component(TASK_TYPE_FOR)
public class For extends DoWhile {

    public For(ParametersUtils parametersUtils, ExecutionDAOFacade executionDAOFacade) {
        super(TASK_TYPE_FOR, parametersUtils, executionDAOFacade);
    }
}
