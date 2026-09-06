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

import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TemplateProcessorTest {

    @Test
    public void replacesNestedPlaceholders() {
        Map<String, Object> input = Map.of("x1", Map.of("x2", Map.of("x3", "ok")), "y", "done");
        String result = TemplateProcessor.processTemplate("a=${x1.x2.x3},b=${y}", input);
        assertEquals("a=ok,b=done", result);
    }
}
