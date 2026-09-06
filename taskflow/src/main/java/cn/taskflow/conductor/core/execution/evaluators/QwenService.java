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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cn.taskflow.conductor.core.model.AICallerException;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationOutput;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;

/**
 * 通义千问 LLM 实现。API Key 从配置读取，禁止硬编码密钥。
 *
 * <p>配置项：{@code conductor.taskflow.qwen.api-key}、{@code conductor.taskflow.qwen.model}
 *
 * @author kevin.luan
 * @since 2025-01-31
 */
@Service
public class QwenService implements LLMService {

    private final String apiKey;
    private final String model;

    public QwenService(
            @Value("${conductor.taskflow.qwen.api-key:}") String apiKey,
            @Value("${conductor.taskflow.qwen.model:qwen-plus}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * 调用通义千问，把整段提示词作为 system 消息发出。
     */
    @Override
    public String query(String query) throws AICallerException {
        if (StringUtils.isBlank(apiKey)) {
            throw new AICallerException("未配置 conductor.taskflow.qwen.api-key，无法调用通义千问");
        }
        Generation gen = new Generation();
        GenerationParam param =
                GenerationParam.builder()
                        .apiKey(apiKey)
                        .model(model)
                        .temperature(0.8f)
                        .maxTokens(2048)
                        .messages(
                                List.of(
                                        Message.builder()
                                                .role(Role.SYSTEM.getValue())
                                                .content(query)
                                                .build()))
                        .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                        .build();
        try {
            GenerationResult result = gen.call(param);
            GenerationOutput.Choice choice = result.getOutput().getChoices().get(0);
            return choice.getMessage().getContent();
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            throw new AICallerException(e.getMessage(), e);
        }
    }
}
