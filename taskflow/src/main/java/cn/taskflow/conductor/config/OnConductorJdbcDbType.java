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
package cn.taskflow.conductor.config;

import java.util.Locale;
import java.util.Set;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * taskflow 的 secret / environment / schema 只挂在 SQL 库上：{@code conductor.db.type} 为 sqlite / postgres
 * / mysql。
 */
public class OnConductorJdbcDbType implements Condition {

    static final Set<String> JDBC_TYPES = Set.of("sqlite", "postgres", "mysql");

    /** {@code conductor.db.type} 是否为 sqlite / postgres / mysql。 */
    static boolean isJdbc(Environment environment) {
        String type = environment.getProperty("conductor.db.type", "memory");
        return JDBC_TYPES.contains(type.toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return isJdbc(context.getEnvironment());
    }
}
