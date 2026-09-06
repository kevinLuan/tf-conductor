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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import cn.taskflow.conductor.core.schema.JdbcSchemaStore;
import cn.taskflow.conductor.core.schema.SchemaRegistry;
import cn.taskflow.conductor.core.schema.SchemaStore;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * OSS 没有 schema 登记。{@code conductor.db.type} 为 sqlite / postgres / mysql 时默认打开，复用进程 {@code
 * DataSource}。
 */
@Configuration
@ConditionalOnProperty(
        name = "conductor.taskflow.schema.enabled",
        havingValue = "true",
        matchIfMissing = true)
@Conditional(OnConductorJdbcDbType.class)
public class TaskflowSchemaConfiguration {

    /** 复用进程 DataSource 的 schema 登记表。 */
    @Bean
    public SchemaStore jdbcSchemaStore(DataSource dataSource) {
        return new JdbcSchemaStore(dataSource);
    }

    /** Orkes SchemaClient 兼容的登记服务。 */
    @Bean
    public SchemaRegistry schemaRegistry(SchemaStore schemaStore, ObjectMapper objectMapper) {
        return new SchemaRegistry(schemaStore, objectMapper);
    }
}
