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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.netflix.conductor.common.metadata.EnvironmentVariable;
import com.netflix.conductor.core.env.EnvVarLookup;
import com.netflix.conductor.core.exception.NotFoundException;
import com.netflix.conductor.dao.EnvironmentDAO;

import cn.taskflow.conductor.core.model.env.EnvironmentVariableView;
import cn.taskflow.conductor.core.model.env.Tag;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 可写 EnvironmentDAO，合同对齐 Orkes EnvironmentClient：key + text/plain value，以及 tags。
 */
public class TaskflowEnvironmentDAO implements EnvironmentDAO {

    private static final TypeReference<List<Tag>> TAGS_TYPE = new TypeReference<>() {
    };

    private final EnvironmentStore store;
    private final boolean envFallback;
    private final String envPrefix;
    private final ObjectMapper mapper;

    public TaskflowEnvironmentDAO(EnvironmentStore store, boolean envFallback, String envPrefix) {
        this(store, envFallback, envPrefix, new ObjectMapper());
    }

    public TaskflowEnvironmentDAO(
            EnvironmentStore store, boolean envFallback, String envPrefix, ObjectMapper mapper) {
        this.store = store;
        this.envFallback = envFallback;
        this.envPrefix = envPrefix == null ? "CONDUCTOR_ENV_" : envPrefix;
        this.mapper = mapper;
    }

    /**
     * 先读 store，未命中且开启回落时再读 {@code CONDUCTOR_ENV_*}。
     */
    @Override
    public String getEnvVariable(String key) {
        requireKey(key);
        return store.get(key)
                .map(EnvironmentRecord::value)
                .orElseGet(() -> envFallback ? EnvVarLookup.lookup(envPrefix, key) : null);
    }

    /**
     * 只写入 store；value 不可为 null。
     */
    @Override
    public void setEnvVariable(String key, String value) {
        requireKey(key);
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
        store.put(key.trim(), value);
    }

    /**
     * 只删 store 中的记录。
     */
    @Override
    public void delete(String key) {
        requireKey(key);
        store.delete(key);
    }

    /**
     * DELETE /environment/{key}：先取旧值再删，与 SDK 返回值对齐。
     */
    public String deleteAndReturn(String key) {
        String previous = getEnvVariable(key);
        delete(key);
        return previous == null ? "" : previous;
    }

    /**
     * OSS {@link EnvironmentDAO} 合同：仅 name / value，不含 tags。
     */
    @Override
    public List<EnvironmentVariable> getAll() {
        return listAll().stream()
                .map(item -> EnvironmentVariable.of(item.getName(), item.getValue()))
                .toList();
    }

    /**
     * GET /environment：name / value / tags。
     */
    public List<EnvironmentVariableView> listAll() {
        Map<String, EnvironmentVariableView> byName = new LinkedHashMap<>();
        for (EnvironmentRecord record : store.list()) {
            byName.put(
                    record.name(),
                    new EnvironmentVariableView(
                            record.name(), record.value(), readTags(record.tagsJson())));
        }
        if (envFallback) {
            EnvVarLookup.allWithPrefix(envPrefix)
                    .forEach(
                            (name, value) ->
                                    byName.putIfAbsent(
                                            name,
                                            new EnvironmentVariableView(name, value, List.of())));
        }
        return new ArrayList<>(byName.values());
    }

    /**
     * 读取指定变量的 tags；变量不存在时返回空列表。
     */
    public List<Tag> getTags(String name) {
        requireKey(name);
        return store.get(name).map(record -> readTags(record.tagsJson())).orElse(List.of());
    }

    /**
     * 整表替换 tags；变量不存在则 404。
     */
    public void setTags(String name, List<Tag> tags) {
        requireKey(name);
        if (tags == null) {
            throw new IllegalArgumentException("tags cannot be null");
        }
        if (store.get(name).isEmpty()) {
            throw new NotFoundException("Environment variable not found: %s", name);
        }
        store.setTagsJson(name, writeTags(tags));
    }

    /**
     * 从现有 tags 中移除给定项后写回。
     */
    public void deleteTags(String name, List<Tag> tags) {
        requireKey(name);
        if (tags == null) {
            throw new IllegalArgumentException("tags cannot be null");
        }
        List<Tag> current = new ArrayList<>(getTags(name));
        current.removeAll(tags);
        if (store.get(name).isPresent()) {
            store.setTagsJson(name, writeTags(current));
        }
    }

    /**
     * 解析 store 里的 tags JSON；坏数据当作空列表。
     */
    private List<Tag> readTags(String tagsJson) {
        if (StringUtils.isBlank(tagsJson)) {
            return new ArrayList<>();
        }
        try {
            List<Tag> tags = mapper.readValue(tagsJson, TAGS_TYPE);
            return tags == null ? new ArrayList<>() : tags;
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    /**
     * 把 tags 序列化进 store。
     */
    private String writeTags(List<Tag> tags) {
        try {
            return mapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("tags are not valid JSON", e);
        }
    }

    /**
     * key 不能为 null 或空白。
     */
    private static void requireKey(String key) {
        if (key == null) {
            throw new NullPointerException("key cannot be null");
        }
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("key cannot be blank");
        }
    }
}
