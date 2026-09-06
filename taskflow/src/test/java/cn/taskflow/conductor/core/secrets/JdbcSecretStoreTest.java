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
package cn.taskflow.conductor.core.secrets;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JdbcSecretStoreTest {

    private SingleConnectionDataSource dataSource;
    private JdbcSecretStore store;

    @Before
    public void setUp() {
        dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        dataSource.setDriverClassName("org.sqlite.JDBC");
        store = new JdbcSecretStore(dataSource);
    }

    @Test
    public void insertUpdateAndDelete() {
        store.put("TOKEN", "secret-1");
        assertEquals("secret-1", store.get("TOKEN").orElseThrow().value());
        assertEquals(1, store.list().size());

        store.put("TOKEN", "secret-2");
        SecretRecord updated = store.get("TOKEN").orElseThrow();
        assertEquals("secret-2", updated.value());
        assertTrue(updated.updatedAt() >= updated.createdAt());

        store.delete("TOKEN");
        assertTrue(store.get("TOKEN").isEmpty());
    }
}
