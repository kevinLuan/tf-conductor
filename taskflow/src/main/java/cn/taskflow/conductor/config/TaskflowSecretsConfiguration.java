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

import org.conductoross.conductor.dao.SecretsDAO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import cn.taskflow.conductor.core.secrets.JdbcSecretStore;
import cn.taskflow.conductor.core.secrets.SecretStore;
import cn.taskflow.conductor.core.secrets.TaskflowSecretsDAO;

/**
 * {@code conductor.secrets.type=taskflow} 且 {@code conductor.db.type} 为 sqlite / postgres / mysql
 * 时替换默认只读 secrets DAO。模块在 classpath 上时由 {@link TaskflowDefaultsEnvironmentPostProcessor} 默认打开。
 */
@Configuration
@ConditionalOnProperty(name = "conductor.secrets.type", havingValue = "taskflow")
@Conditional(OnConductorJdbcDbType.class)
public class TaskflowSecretsConfiguration {

    /** 复用进程 DataSource 的隐私配置表。 */
    @Bean
    public SecretStore jdbcSecretStore(DataSource dataSource) {
        return new JdbcSecretStore(dataSource);
    }

    /** 可写 SecretsDAO，默认允许环境变量回落。 */
    @Bean
    public SecretsDAO taskflowSecretsDAO(
            SecretStore secretStore,
            @Value("${conductor.taskflow.secrets.env-fallback:true}") boolean envFallback,
            @Value("${conductor.secrets.env.prefix:CONDUCTOR_SECRET_}") String envPrefix) {
        return new TaskflowSecretsDAO(secretStore, envFallback, envPrefix);
    }
}
