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
package cn.taskflow.conductor.core.model;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM 调用返回结果。
 *
 * @author kevin.luan
 * @since 2025-04-08
 */
public class AIResult {
    public static final AIResult EMPTY = new AIResult();

    private String result;
    private String finishReason;
    private Long tokenUsed;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public Long getTokenUsed() {
        return tokenUsed;
    }

    public void setTokenUsed(Long tokenUsed) {
        this.tokenUsed = tokenUsed;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("result", result);
        map.put("finishReason", finishReason);
        map.put("tokenUsed", tokenUsed);
        return map;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final AIResult value = new AIResult();

        public Builder result(String result) {
            value.setResult(result);
            return this;
        }

        public Builder finishReason(String finishReason) {
            value.setFinishReason(finishReason);
            return this;
        }

        public Builder tokenUsed(Long tokenUsed) {
            value.setTokenUsed(tokenUsed);
            return this;
        }

        public AIResult build() {
            return value;
        }
    }
}
