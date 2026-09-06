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

import java.util.List;
import java.util.Optional;

/**
 * 环境变量存储。与隐私配置分表，memory / jdbc 都走这套。
 */
public interface EnvironmentStore {

    /**
     * 按名称读取一条环境变量。
     */
    Optional<EnvironmentRecord> get(String name);

    /**
     * 列出全部环境变量。
     */
    List<EnvironmentRecord> list();

    /**
     * 按名称写入值；已存在则覆盖，tags 保持不变。
     */
    void put(String name, String value);

    /**
     * 按名称删除；不存在则视为成功。
     */
    void delete(String name);

    /**
     * 只改 tags，不改 value。行不存在则 no-op。
     */
    void setTagsJson(String name, String tagsJson);
}
