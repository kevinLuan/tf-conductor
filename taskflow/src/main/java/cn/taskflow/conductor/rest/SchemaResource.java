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
package cn.taskflow.conductor.rest;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.conductor.common.metadata.SchemaDef;

import cn.taskflow.conductor.core.schema.SchemaRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 对齐 Orkes {@code /api/schema} 与 SchemaClient。OSS 没有该资源（#1118），在 taskflow 模块补上。
 *
 * <p>{@code POST} 同时收单个 {@code SchemaDef}（Orkes 文档）和数组（Java SDK {@code saveSchemas}）。
 */
@RestController
@RequestMapping("/api/schema")
@ConditionalOnBean(SchemaRegistry.class)
public class SchemaResource {

    private final SchemaRegistry registry;
    private final ObjectMapper mapper;

    public SchemaResource(SchemaRegistry registry, ObjectMapper mapper) {
        this.registry = registry;
        this.mapper = mapper;
    }

    /** 列出全部 schema；{@code short=true} 省略 data。 */
    @GetMapping
    public List<SchemaDef> getAllSchemas(
            @RequestParam(name = "short", defaultValue = "false") boolean shortFormat) {
        return registry.list(shortFormat);
    }

    /** 保存单个或数组 schema；{@code newVersion=true} 时自动 max+1。 */
    @PostMapping
    public void saveSchemas(
            @RequestParam(name = "newVersion", defaultValue = "false") boolean newVersion,
            @RequestBody JsonNode body) {
        if (body == null || body.isNull() || body.isMissingNode()) {
            throw new IllegalArgumentException("schema body is required");
        }
        if (body.isArray()) {
            for (JsonNode node : body) {
                registry.save(mapper.convertValue(node, SchemaDef.class), newVersion);
            }
            return;
        }
        registry.save(mapper.convertValue(body, SchemaDef.class), newVersion);
    }

    /** 取该 name 最新版本。 */
    @GetMapping("/{name}")
    public SchemaDef getLatest(@PathVariable("name") String name) {
        return registry.getLatest(name);
    }

    /** 取指定版本。 */
    @GetMapping("/{name}/{version}")
    public SchemaDef getByVersion(
            @PathVariable("name") String name, @PathVariable("version") int version) {
        return registry.get(name, version);
    }

    /** 删除该 name 全部版本。 */
    @DeleteMapping("/{name}")
    public void deleteAll(@PathVariable("name") String name) {
        registry.deleteAll(name);
    }

    /** 删除指定版本。 */
    @DeleteMapping("/{name}/{version}")
    public void deleteVersion(
            @PathVariable("name") String name, @PathVariable("version") int version) {
        registry.delete(name, version);
    }
}
