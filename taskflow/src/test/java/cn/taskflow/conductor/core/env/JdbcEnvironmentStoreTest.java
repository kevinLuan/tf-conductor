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
package cn.taskflow.conductor.core.env;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import cn.taskflow.conductor.core.secrets.JdbcSecretStore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JdbcEnvironmentStoreTest {

    private SingleConnectionDataSource dataSource;
    private JdbcEnvironmentStore store;

    @Before
    public void setUp() {
        dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        dataSource.setDriverClassName("org.sqlite.JDBC");
        store = new JdbcEnvironmentStore(dataSource);
    }

    @Test
    public void insertUpdateAndDelete() {
        store.put("REGION", "us-east-1");
        assertEquals("us-east-1", store.get("REGION").orElseThrow().value());
        assertEquals(1, store.list().size());

        store.put("REGION", "eu-west-1");
        EnvironmentRecord updated = store.get("REGION").orElseThrow();
        assertEquals("eu-west-1", updated.value());
        assertTrue(updated.updatedAt() >= updated.createdAt());

        store.delete("REGION");
        assertTrue(store.get("REGION").isEmpty());
    }

    @Test
    public void putKeepsTags() {
        store.put("REGION", "us-east-1");
        store.setTagsJson("REGION", "[{\"key\":\"k\",\"value\":\"v\"}]");
        store.put("REGION", "eu-west-1");
        assertEquals(
                "[{\"key\":\"k\",\"value\":\"v\"}]", store.get("REGION").orElseThrow().tagsJson());
        assertEquals("eu-west-1", store.get("REGION").orElseThrow().value());
    }

    @Test
    public void sameNameDoesNotCollideWithSecretStore() {
        JdbcSecretStore secrets = new JdbcSecretStore(dataSource);
        secrets.put("TOKEN", "secret-1");
        store.put("TOKEN", "env-1");
        assertEquals("secret-1", secrets.get("TOKEN").orElseThrow().value());
        assertEquals("env-1", store.get("TOKEN").orElseThrow().value());
        secrets.delete("TOKEN");
        assertTrue(secrets.get("TOKEN").isEmpty());
        assertEquals("env-1", store.get("TOKEN").orElseThrow().value());
    }
}
