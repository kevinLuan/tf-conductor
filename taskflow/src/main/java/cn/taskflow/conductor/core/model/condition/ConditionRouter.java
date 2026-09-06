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
package cn.taskflow.conductor.core.model.condition;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 条件路由器：评估条件组，并返回匹配的 action。 if-else 分支：条件命中后返回 {@code action} 作为 SWITCH case。
 *
 * @author kevin.luan
 * @since 2025-05-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConditionRouter {
    private ConditionGroup condition;

    /** 对应 SWITCH decisionCases 的 key */
    private String action;

    /**
     * 条件组：组合多个原子谓词，执行 AND/OR 逻辑运算。 AND/OR 逻辑节点，子节点为 {@link ConditionPredicate}。
     *
     * @author kevin.luan
     * @since 2025-05-06
     */
    @Data
    public static class ConditionGroup {
        private LogicOperatorEnum operator;
        private List<ConditionPredicate> children = new ArrayList<>();

        public ConditionGroup() {}

        public ConditionGroup(LogicOperatorEnum operator) {
            this.operator = operator;
        }

        /** 追加一条原子谓词。 */
        public void addChild(ConditionPredicate child) {
            this.children.add(child);
        }

        @Override
        public String toString() {
            return "LogicNode{" + "operator='" + operator + '\'' + ", children=" + children + '}';
        }
    }

    /**
     * 条件谓词：执行单次原子判断。
     *
     * @author kevin.luan
     * @since 2025-05-06
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConditionPredicate {
        /** 表达式，支持 ${workflow.input.xxx} 占位符 */
        private String expression;

        private OperatorEnum operator;

        /** 右侧比较值，同样支持表达式 */
        private String value;
    }
}
