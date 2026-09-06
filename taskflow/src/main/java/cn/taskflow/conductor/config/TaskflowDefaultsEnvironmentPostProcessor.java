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

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * conductor-taskflow 在 classpath 上、且 {@code conductor.db.type} 为 sqlite / postgres / mysql 时，默认接管可写
 * secrets / environment，无需在 {@code application.properties} 里再写开关。已显式配置的 {@code
 * conductor.secrets.type} / {@code conductor.environment.type} 不覆盖。
 */
public class TaskflowDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String PROPERTY_SOURCE_NAME = "taskflowDefaults";
    static final String SECRETS_TYPE = "conductor.secrets.type";
    static final String ENVIRONMENT_TYPE = "conductor.environment.type";
    static final String TASKFLOW = "taskflow";

    /** 仅在 SQL 库上补默认 {@code type=taskflow}，已显式配置的不覆盖。 */
    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        if (!OnConductorJdbcDbType.isJdbc(environment)) {
            return;
        }
        Map<String, Object> defaults = new HashMap<>();
        if (!environment.containsProperty(SECRETS_TYPE)) {
            defaults.put(SECRETS_TYPE, TASKFLOW);
        }
        if (!environment.containsProperty(ENVIRONMENT_TYPE)) {
            defaults.put(ENVIRONMENT_TYPE, TASKFLOW);
        }
        if (!defaults.isEmpty()) {
            environment
                    .getPropertySources()
                    .addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
        }
    }
}
