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

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.netflix.conductor.common.metadata.SchemaDef;
import com.netflix.conductor.core.exception.NotFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Orkes / SchemaClient 兼容的 schema 登记。
 */
public class SchemaRegistry {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final SchemaStore store;
    private final ObjectMapper mapper;

    public SchemaRegistry(SchemaStore store, ObjectMapper mapper) {
        this.store = store;
        this.mapper = mapper;
    }

    /**
     * 按 name+version upsert。{@code newVersion=true} 时忽略请求里的 version，写成当前 max+1；更新时保留原 createdAt /
     * createdBy。
     */
    public void save(SchemaDef schemaDef, boolean newVersion) {
        if (schemaDef == null || StringUtils.isBlank(schemaDef.getName())) {
            throw new IllegalArgumentException("schema name is required");
        }
        String name = schemaDef.getName().trim();
        int version = newVersion ? store.maxVersion(name) + 1 : Math.max(schemaDef.getVersion(), 1);
        if (version < 1) {
            version = 1;
        }
        long now = System.currentTimeMillis();
        SchemaRecord existing = store.get(name, version).orElse(null);
        store.put(
                new SchemaRecord(
                        name,
                        version,
                        (schemaDef.getType() == null ? SchemaDef.Type.JSON : schemaDef.getType())
                                .name(),
                        writeData(schemaDef.getData()),
                        schemaDef.getExternalRef(),
                        existing == null ? now : existing.createdAt(),
                        now,
                        existing == null ? schemaDef.getCreatedBy() : existing.createdBy(),
                        schemaDef.getUpdatedBy()));
    }

    /**
     * 返回该 name 的最高版本；没有则 404。
     */
    public SchemaDef getLatest(String name) {
        requireName(name);
        int version = store.maxVersion(name);
        if (version < 1) {
            throw new NotFoundException("Schema not found: %s", name);
        }
        return get(name, version);
    }

    /**
     * 读取指定版本；没有则 404。
     */
    public SchemaDef get(String name, int version) {
        requireName(name);
        return store.get(name, version)
                .map(row -> toDef(row, false))
                .orElseThrow(() -> new NotFoundException("Schema not found: %s/%s", name, version));
    }

    /**
     * {@code shortFormat=true} 时只留 name / version / 时间，不带 data。
     */
    public List<SchemaDef> list(boolean shortFormat) {
        return store.listAll().stream().map(row -> toDef(row, shortFormat)).toList();
    }

    /**
     * 删除该 name 全部版本；不存在则 404。
     */
    public void deleteAll(String name) {
        requireName(name);
        if (store.maxVersion(name) < 1) {
            throw new NotFoundException("Schema not found: %s", name);
        }
        store.deleteAll(name);
    }

    /**
     * 删除指定版本；不存在则 404。
     */
    public void delete(String name, int version) {
        requireName(name);
        if (store.get(name, version).isEmpty()) {
            throw new NotFoundException("Schema not found: %s/%s", name, version);
        }
        store.delete(name, version);
    }

    /**
     * 行记录转 {@link SchemaDef}；短格式省略 payload。
     */
    private SchemaDef toDef(SchemaRecord row, boolean shortFormat) {
        SchemaDef def = new SchemaDef();
        def.setName(row.name());
        def.setVersion(row.version());
        def.setCreateTime(row.createdAt());
        def.setUpdateTime(row.updatedAt());
        if (!shortFormat) {
            def.setType(SchemaDef.Type.valueOf(row.type()));
            def.setData(readData(row.dataJson()));
            def.setExternalRef(row.externalRef());
            def.setCreatedBy(row.createdBy());
            def.setUpdatedBy(row.updatedBy());
        }
        return def;
    }

    private String writeData(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("schema data is not valid JSON", e);
        }
    }

    private Map<String, Object> readData(String dataJson) {
        if (StringUtils.isBlank(dataJson)) {
            return null;
        }
        try {
            return mapper.readValue(dataJson, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("stored schema data is not valid JSON", e);
        }
    }

    private static void requireName(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("schema name is required");
        }
    }
}
