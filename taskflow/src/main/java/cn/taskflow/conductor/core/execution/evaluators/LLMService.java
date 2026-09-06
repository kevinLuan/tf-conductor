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
package cn.taskflow.conductor.core.execution.evaluators;

import cn.taskflow.conductor.core.model.AICallerException;

/**
 * LLM 调用抽象。当前分类评估只需要问答接口。
 *
 * @author kevin.luan
 * @since 2025-01-31
 */
public interface LLMService {

    /**
     * 向模型发起一次问答，返回模型文本。
     *
     * @param query 已拼装的完整提示词
     */
    String query(String query) throws AICallerException;
}
