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
import java.util.Optional;

/** Schema 登记存储。memory / jdbc 都走这套。 */
public interface SchemaStore {

    /** 按 name + version 读取一条 schema。 */
    Optional<SchemaRecord> get(String name, int version);

    /** 列出全部 schema 版本。 */
    List<SchemaRecord> listAll();

    /** 该 name 的最大版本号；没有记录时返回 0。 */
    int maxVersion(String name);

    /** 按 name + version upsert。 */
    void put(SchemaRecord record);

    /** 删除指定版本。 */
    void delete(String name, int version);

    /** 删除该 name 的全部版本。 */
    void deleteAll(String name);
}
