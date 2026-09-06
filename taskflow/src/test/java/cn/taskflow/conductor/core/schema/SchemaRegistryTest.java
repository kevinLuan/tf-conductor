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

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.netflix.conductor.common.metadata.SchemaDef;
import com.netflix.conductor.core.exception.NotFoundException;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SchemaRegistryTest {

    private SchemaRegistry registry;

    @Before
    public void setUp() {
        SingleConnectionDataSource dataSource =
                new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        dataSource.setDriverClassName("org.sqlite.JDBC");
        registry = new SchemaRegistry(new JdbcSchemaStore(dataSource), new ObjectMapper());
    }

    @Test
    public void saveGetLatestAndVersion() {
        registry.save(schema("itemSchema", 1, Map.of("type", "object")), false);
        SchemaDef latest = registry.getLatest("itemSchema");
        assertEquals(1, latest.getVersion());
        assertEquals("object", latest.getData().get("type"));
        assertEquals(SchemaDef.Type.JSON, latest.getType());
        assertEquals(1, registry.get("itemSchema", 1).getVersion());
    }

    @Test
    public void newVersionIncrements() {
        registry.save(schema("itemSchema", 1, Map.of("v", "1")), false);
        registry.save(schema("itemSchema", 1, Map.of("v", "2")), true);
        assertEquals(2, registry.getLatest("itemSchema").getVersion());
        assertEquals("1", registry.get("itemSchema", 1).getData().get("v"));
        assertEquals("2", registry.get("itemSchema", 2).getData().get("v"));
    }

    @Test
    public void upsertKeepsCreatedTime() {
        registry.save(schema("itemSchema", 1, Map.of("type", "object")), false);
        long created = registry.get("itemSchema", 1).getCreateTime();
        registry.save(schema("itemSchema", 1, Map.of("type", "string")), false);
        SchemaDef updated = registry.get("itemSchema", 1);
        assertEquals("string", updated.getData().get("type"));
        assertEquals(created, updated.getCreateTime().longValue());
        assertTrue(updated.getUpdateTime() >= created);
    }

    @Test
    public void shortListOmitsPayload() {
        registry.save(schema("itemSchema", 1, Map.of("type", "object")), false);
        SchemaDef shortDef = registry.list(true).get(0);
        assertEquals("itemSchema", shortDef.getName());
        assertEquals(1, shortDef.getVersion());
        assertNull(shortDef.getData());
        assertNull(shortDef.getType());
        assertEquals(1, registry.list(false).get(0).getData().size());
    }

    @Test
    public void deleteVersionAndName() {
        registry.save(schema("itemSchema", 1, Map.of("v", "1")), false);
        registry.save(schema("itemSchema", 1, Map.of("v", "2")), true);
        registry.delete("itemSchema", 1);
        assertEquals(2, registry.getLatest("itemSchema").getVersion());
        registry.deleteAll("itemSchema");
        assertTrue(registry.list(false).isEmpty());
    }

    @Test(expected = NotFoundException.class)
    public void missingNameIsNotFound() {
        registry.getLatest("missing");
    }

    @Test(expected = IllegalArgumentException.class)
    public void blankNameRejected() {
        registry.save(new SchemaDef(), false);
    }

    private static SchemaDef schema(String name, int version, Map<String, Object> data) {
        SchemaDef def = new SchemaDef();
        def.setName(name);
        def.setVersion(version);
        def.setType(SchemaDef.Type.JSON);
        def.setData(data);
        return def;
    }
}
