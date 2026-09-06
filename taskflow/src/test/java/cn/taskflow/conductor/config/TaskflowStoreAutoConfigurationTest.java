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

import org.junit.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import cn.taskflow.conductor.core.env.EnvironmentStore;
import cn.taskflow.conductor.core.env.JdbcEnvironmentStore;
import cn.taskflow.conductor.core.schema.JdbcSchemaStore;
import cn.taskflow.conductor.core.schema.SchemaRegistry;
import cn.taskflow.conductor.core.schema.SchemaStore;
import cn.taskflow.conductor.core.secrets.JdbcSecretStore;
import cn.taskflow.conductor.core.secrets.SecretStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TaskflowStoreAutoConfigurationTest {

    @Test
    public void sqliteUsesJdbcStores() {
        runner("sqlite")
                .withBean(DataSource.class, this::sqliteDataSource)
                .run(
                        ctx -> {
                            assertTrue(ctx.getBean(SchemaStore.class) instanceof JdbcSchemaStore);
                            assertTrue(ctx.getBean(SecretStore.class) instanceof JdbcSecretStore);
                            assertTrue(
                                    ctx.getBean(EnvironmentStore.class)
                                            instanceof JdbcEnvironmentStore);
                        });
    }

    @Test
    public void postgresUsesJdbcStores() {
        runner("postgres")
                .withBean(DataSource.class, this::sqliteDataSource)
                .run(ctx -> assertTrue(ctx.getBean(SchemaStore.class) instanceof JdbcSchemaStore));
    }

    @Test
    public void mysqlUsesJdbcStores() {
        runner("mysql")
                .withBean(DataSource.class, this::sqliteDataSource)
                .run(ctx -> assertTrue(ctx.getBean(SchemaStore.class) instanceof JdbcSchemaStore));
    }

    @Test
    public void nonJdbcDbTypeSkipsStores() {
        runner("memory")
                .run(
                        ctx -> {
                            assertFalse(ctx.containsBean("jdbcSchemaStore"));
                            assertFalse(ctx.containsBean("jdbcSecretStore"));
                            assertFalse(ctx.containsBean("jdbcEnvironmentStore"));
                            assertFalse(ctx.containsBean("schemaRegistry"));
                        });
        runner("redis_standalone")
                .run(ctx -> assertFalse(ctx.getBeanNamesForType(SchemaRegistry.class).length > 0));
    }

    private ApplicationContextRunner runner(String dbType) {
        return new ApplicationContextRunner()
                .withUserConfiguration(
                        TaskflowSchemaConfiguration.class,
                        TaskflowSecretsConfiguration.class,
                        TaskflowEnvironmentConfiguration.class)
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withPropertyValues(
                        "conductor.db.type=" + dbType,
                        "conductor.secrets.type=taskflow",
                        "conductor.environment.type=taskflow");
    }

    private DataSource sqliteDataSource() {
        SingleConnectionDataSource dataSource =
                new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        dataSource.setDriverClassName("org.sqlite.JDBC");
        return dataSource;
    }
}
