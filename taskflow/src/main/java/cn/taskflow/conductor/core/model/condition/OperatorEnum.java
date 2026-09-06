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

/**
 * if-else 比较运算符。
 *
 * @author kevin.luan
 * @since 2025-05-06
 */
public enum OperatorEnum {
    EQUALS("="),
    NOT_EQUALS("!="),
    GREATER_THAN(">"),
    LESS_THAN("<"),
    GREATER_THAN_OR_EQUAL(">="),
    LESS_THAN_OR_EQUAL("<="),
    EXISTS("存在"),
    NOT_EXISTS("不为空"),
    EMPTY("为空");

    private final String symbol;

    OperatorEnum(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    /** 按画布符号解析运算符，未知则抛错。 */
    public static OperatorEnum fromSymbol(String symbol) {
        for (OperatorEnum op : OperatorEnum.values()) {
            if (op.getSymbol().equals(symbol)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unknown operator: " + symbol);
    }

    /** 值为空类运算符不要求右侧比较值。 */
    public boolean isOptionalValue() {
        return this == EXISTS || this == NOT_EXISTS || this == EMPTY;
    }

    @Override
    public String toString() {
        return symbol;
    }
}
