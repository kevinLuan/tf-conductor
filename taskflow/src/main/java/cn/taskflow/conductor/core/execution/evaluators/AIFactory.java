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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import cn.taskflow.conductor.core.model.AICallerException;
import cn.taskflow.conductor.core.model.SwitchAiParams;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

/**
 * AI/LLM 调用入口：根据分类提示词调用 {@link LLMService}，将 SWITCH cases 映射为模型返回的 id。
 *
 * @author kevin.luan
 * @since 2025-01-31
 */
@Service
public class AIFactory {
    private static final Pattern UNSAFE_CHAR_PATTERN = Pattern.compile("[\\p{Cntrl}\\p{Cc}]");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Logger LOGGER = LoggerFactory.getLogger(AIFactory.class);

    private final LLMService llmService;
    private final ObjectMapper objectMapper;
    private final String prompt;

    public AIFactory(
            LLMService llmService, ObjectMapper objectMapper, ResourceLoader resourceLoader)
            throws IOException {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
        this.prompt = loadContent(resourceLoader, "classify.txt");
    }

    /** 从 classpath {@code ai/prompt/} 读取分类提示词模板。 */
    private String loadContent(ResourceLoader resourceLoader, String resourceName)
            throws IOException {
        Resource resource = resourceLoader.getResource("classpath:/ai/prompt/" + resourceName);
        LOGGER.info("Loading AI classify prompt from: {}", resource.getFilename());
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * 用 LLM 在 SWITCH cases 中做语义分类。
     *
     * @param params 分类指令
     * @param input 任务输入
     * @param cases decisionCases 的 key 集合
     * @return 命中的 case 名称；未命中为空
     */
    public Optional<String> classify(SwitchAiParams params, Object input, Set<String> cases)
            throws JsonProcessingException, AICallerException {
        List<CaseInfo> list = convertCases(cases);
        String query = generatePrompt(params.getInstructions(), input, list);
        try {
            String result = llmService.query(query);
            JsonNode jsonNode = objectMapper.readTree(result);
            LOGGER.info("Ai Result:{}", jsonNode);
            int id = jsonNode.get("id").asInt();
            if (id > 0) {
                Optional<CaseInfo> optional = list.stream().filter(d -> d.getId() == id).findAny();
                if (optional.isPresent()) {
                    return Optional.ofNullable(optional.get().getDescription());
                }
            }
            return Optional.empty();
        } catch (AICallerException e) {
            LOGGER.error("调用 AI 出错 query:`{}`", query);
            throw e;
        }
    }

    /** 把 decisionCases key 编成从 1 起的 id，供模型返回数字编号。 */
    private List<CaseInfo> convertCases(Set<String> cases) {
        List<CaseInfo> list = new ArrayList<>();
        AtomicInteger cnt = new AtomicInteger(0);
        cases.forEach(c -> list.add(CaseInfo.of(cnt.incrementAndGet(), c)));
        return list;
    }

    @Data(staticConstructor = "of")
    public static class CaseInfo {
        private final Integer id;
        private final String description;
    }

    /** 用分类模板填充指令、输入和候选分支，并清洗用户文本。 */
    public String generatePrompt(String instructions, Object inputMap, List<CaseInfo> cases)
            throws JsonProcessingException {
        instructions = sanitizeInput(instructions);
        String input = objectMapper.writeValueAsString(inputMap);
        String switchCases = objectMapper.writeValueAsString(cases);
        return prompt.replace("{{input}}", sanitizeInput(input))
                .replace("{{instructions}}", sanitizeInput(instructions))
                .replace("{{switch_cases}}", sanitizeInput(switchCases));
    }

    /** 清洗用户输入，移除或转义特殊字符，避免干扰 AI 判断。 */
    private String sanitizeInput(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String sanitized = HTML_TAG_PATTERN.matcher(input).replaceAll("");
        sanitized = UNSAFE_CHAR_PATTERN.matcher(sanitized).replaceAll(" ");
        sanitized = sanitized.replace("\\", "\\\\").replace("\"", "\\\"");
        sanitized = sanitized.replace("\n", " ").replace("\r", " ");
        sanitized = sanitized.replaceAll("\\s+", " ").trim();

        return sanitized;
    }
}
