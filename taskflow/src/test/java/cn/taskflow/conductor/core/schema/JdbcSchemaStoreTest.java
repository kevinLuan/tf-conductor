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
package cn.taskflow.conductor.core.schema;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JdbcSchemaStoreTest {

    private JdbcSchemaStore store;

    @Before
    public void setUp() {
        SingleConnectionDataSource dataSource =
                new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        dataSource.setDriverClassName("org.sqlite.JDBC");
        store = new JdbcSchemaStore(dataSource);
    }

    @Test
    public void insertUpdateAndDelete() {
        long now = System.currentTimeMillis();
        store.put(record("itemSchema", 1, "{\"type\":\"object\"}", now, now));
        assertEquals("{\"type\":\"object\"}", store.get("itemSchema", 1).orElseThrow().dataJson());
        assertEquals(1, store.maxVersion("itemSchema"));

        store.put(record("itemSchema", 1, "{\"type\":\"string\"}", now, now + 1));
        assertEquals("{\"type\":\"string\"}", store.get("itemSchema", 1).orElseThrow().dataJson());
        assertEquals(1, store.listAll().size());

        store.put(record("itemSchema", 2, "{\"type\":\"array\"}", now + 2, now + 2));
        assertEquals(2, store.maxVersion("itemSchema"));

        store.delete("itemSchema", 1);
        assertTrue(store.get("itemSchema", 1).isEmpty());
        store.deleteAll("itemSchema");
        assertEquals(0, store.maxVersion("itemSchema"));
    }

    private static SchemaRecord record(
            String name, int version, String dataJson, long createdAt, long updatedAt) {
        return new SchemaRecord(
                name, version, "JSON", dataJson, null, createdAt, updatedAt, null, null);
    }
}
