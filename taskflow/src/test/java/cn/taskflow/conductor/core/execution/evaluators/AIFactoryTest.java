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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.DefaultResourceLoader;

import cn.taskflow.conductor.core.model.SwitchAiParams;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AIFactoryTest {

    private AIFactory aiFactory;

    @Before
    public void setUp() throws Exception {
        LLMService stub = query -> "{\"id\": 2}";
        aiFactory = new AIFactory(stub, new ObjectMapper(), new DefaultResourceLoader());
    }

    @Test
    public void classifyMapsReturnedIdToCaseName() throws Exception {
        SwitchAiParams condition = new SwitchAiParams();
        condition.setInstructions("按意图分类");
        Set<String> cases = new LinkedHashSet<>(List.of("refund", "consult"));

        Optional<String> result = aiFactory.classify(condition, Map.of("q", "怎么退货"), cases);

        assertEquals("consult", result.orElse(null));
    }

    @Test
    public void generatePromptContainsSanitizedInstructions() throws Exception {
        String prompt =
                aiFactory.generatePrompt(
                        "hello <b>world</b>",
                        Map.of("k", "v"),
                        List.of(AIFactory.CaseInfo.of(1, "caseA")));

        assertTrue(prompt.contains("hello world"));
        assertTrue(prompt.contains("caseA"));
    }
}
