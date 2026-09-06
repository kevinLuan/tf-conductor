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

import java.util.List;
import java.util.Optional;

/** 隐私配置存储。与环境变量分表，memory / jdbc 都走这套。 */
public interface SecretStore {

    /** 按名称读取一条隐私配置。 */
    Optional<SecretRecord> get(String name);

    /** 列出全部隐私配置，按名称排序由实现决定。 */
    List<SecretRecord> list();

    /** 按名称写入；已存在则覆盖值并刷新更新时间。 */
    void put(String name, String value);

    /** 按名称删除；不存在则视为成功。 */
    void delete(String name);
}
