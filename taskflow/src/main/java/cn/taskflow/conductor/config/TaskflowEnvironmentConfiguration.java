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

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import cn.taskflow.conductor.core.env.EnvironmentStore;
import cn.taskflow.conductor.core.env.JdbcEnvironmentStore;
import cn.taskflow.conductor.core.env.TaskflowEnvironmentDAO;

/**
 * {@code conductor.environment.type=taskflow} 且 {@code conductor.db.type} 为 sqlite / postgres /
 * mysql 时替换默认只读 environment DAO。模块在 classpath 上时由 {@link TaskflowDefaultsEnvironmentPostProcessor}
 * 默认打开。
 */
@Configuration
@ConditionalOnProperty(name = "conductor.environment.type", havingValue = "taskflow")
@Conditional(OnConductorJdbcDbType.class)
public class TaskflowEnvironmentConfiguration {

    /** 复用进程 DataSource 的环境变量表。 */
    @Bean
    public EnvironmentStore jdbcEnvironmentStore(DataSource dataSource) {
        return new JdbcEnvironmentStore(dataSource);
    }

    /** 可写 EnvironmentDAO，默认允许环境变量回落。 */
    @Bean
    public TaskflowEnvironmentDAO taskflowEnvironmentDAO(
            EnvironmentStore environmentStore,
            @Value("${conductor.taskflow.environment.env-fallback:true}") boolean envFallback,
            @Value("${conductor.environment.env.prefix:CONDUCTOR_ENV_}") String envPrefix) {
        return new TaskflowEnvironmentDAO(environmentStore, envFallback, envPrefix);
    }
}
