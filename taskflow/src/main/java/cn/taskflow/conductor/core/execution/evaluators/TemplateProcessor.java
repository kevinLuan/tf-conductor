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

/**
 * 简单模板处理器：将 {@code ${a.b.c}} 替换为 Map 中的嵌套值。
 *
 * @author kevin.luan
 * @since 2025-01-31
 */
public final class TemplateProcessor {

    private TemplateProcessor() {
    }

    /**
     * 处理模版。
     *
     * <pre>
     * 测试变量一：${x1.x2.x3}
     * 变量二: ${y}
     * </pre>
     *
     * @param template 模版文本
     * @param input    运行时输入
     */
    public static String processTemplate(String template, Map<String, Object> input) {
        if (template == null || input == null) {
            return template;
        }

        String pattern = "\\$\\{([^}]+)}";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(template);

        StringBuffer result = new StringBuffer();

        while (m.find()) {
            String placeholder = m.group(1);
            String replacement = getValueFromMap(placeholder, input);
            m.appendReplacement(result, replacement != null ? replacement : "");
        }
        m.appendTail(result);

        return result.toString();
    }

    /**
     * 按点号路径从嵌套 Map 取值，中途不是 Map 则视为未命中。
     */
    private static String getValueFromMap(String key, Map<String, Object> input) {
        String[] parts = key.split("\\.");
        Object current = input;
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }
        return current != null ? current.toString() : "";
    }
}
