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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.taskflow.conductor.core.env.TaskflowEnvironmentDAO;
import cn.taskflow.conductor.core.model.env.EnvironmentVariableView;
import cn.taskflow.conductor.core.model.env.Tag;

/**
 * 对齐 Orkes {@code EnvironmentClient} / {@code EnvironmentResource}。
 *
 * <ul>
 *   <li>{@code GET /api/environment} — {@code [{name, value, tags}]}
 *   <li>{@code GET /api/environment/{key}} — text/plain
 *   <li>{@code PUT /api/environment/{key}} — text/plain body
 *   <li>{@code DELETE /api/environment/{key}} — 返回旧值 text/plain
 *   <li>{@code GET|PUT|DELETE /api/environment/{name}/tags}
 * </ul>
 */
@RestController
@RequestMapping("/api/environment")
@ConditionalOnProperty(name = "conductor.environment.type", havingValue = "taskflow")
public class EnvironmentResource {

    private final TaskflowEnvironmentDAO environmentDAO;

    public EnvironmentResource(TaskflowEnvironmentDAO environmentDAO) {
        this.environmentDAO = environmentDAO;
    }

    /** 列出全部环境变量（含 tags）。 */
    @GetMapping
    public List<EnvironmentVariableView> getAllEnvironmentVariables() {
        return environmentDAO.listAll();
    }

    /** 按 key 取值，不存在返回空串。 */
    @GetMapping(value = "/{key}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getEnvironmentVariable(@PathVariable("key") String key) {
        String value = environmentDAO.getEnvVariable(key);
        return value == null ? "" : value;
    }

    @PutMapping(
            value = "/{key}",
            consumes = {MediaType.TEXT_PLAIN_VALUE, MediaType.ALL_VALUE})
    /** 创建或覆盖环境变量值。 */
    public void createOrUpdateEnvironmentVariable(
            @PathVariable("key") String key, @RequestBody String value) {
        environmentDAO.setEnvVariable(key, value);
    }

    /** 删除并返回旧值，对齐 Orkes SDK。 */
    @DeleteMapping(value = "/{key}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String deleteEnvironmentVariable(@PathVariable("key") String key) {
        return environmentDAO.deleteAndReturn(key);
    }

    /** 读取指定变量的 tags。 */
    @GetMapping("/{name}/tags")
    public List<Tag> getEnvironmentVariableTags(@PathVariable("name") String name) {
        return environmentDAO.getTags(name);
    }

    /** 整表替换 tags。 */
    @PutMapping("/{name}/tags")
    public void setEnvironmentVariableTags(
            @PathVariable("name") String name, @RequestBody List<Tag> tags) {
        environmentDAO.setTags(name, tags);
    }

    /** 从现有 tags 中删除给定项。 */
    @DeleteMapping("/{name}/tags")
    public void deleteEnvironmentVariableTags(
            @PathVariable("name") String name, @RequestBody List<Tag> tags) {
        environmentDAO.deleteTags(name, tags);
    }
}
