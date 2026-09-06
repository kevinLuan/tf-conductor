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

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/** 复用进程 DataSource。表 {@code taskflow_environment}，与隐私配置分表。 */
public final class JdbcEnvironmentStore implements EnvironmentStore {

    private static final String CREATE_TABLE =
            """
            CREATE TABLE IF NOT EXISTS taskflow_environment (
              name VARCHAR(100) NOT NULL PRIMARY KEY,
              value TEXT NOT NULL,
              tags TEXT,
              created_at INTEGER NOT NULL,
              updated_at INTEGER NOT NULL
            )
            """;

    private static final RowMapper<EnvironmentRecord> ROW_MAPPER =
            (rs, rowNum) ->
                    new EnvironmentRecord(
                            rs.getString("name"),
                            rs.getString("value"),
                            rs.getString("tags"),
                            rs.getLong("created_at"),
                            rs.getLong("updated_at"));

    private final JdbcTemplate jdbc;

    /** 启动时确保 {@code taskflow_environment} 表存在。 */
    public JdbcEnvironmentStore(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
        jdbc.execute(CREATE_TABLE);
    }

    @Override
    public Optional<EnvironmentRecord> get(String name) {
        List<EnvironmentRecord> rows =
                jdbc.query(
                        "SELECT name, value, tags, created_at, updated_at FROM taskflow_environment"
                                + " WHERE name = ?",
                        ROW_MAPPER,
                        name);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public List<EnvironmentRecord> list() {
        return jdbc.query(
                "SELECT name, value, tags, created_at, updated_at FROM taskflow_environment"
                        + " ORDER BY name",
                ROW_MAPPER);
    }

    /** 先 UPDATE 值，不影响 tags；无行则 INSERT。 */
    @Override
    public void put(String name, String value) {
        long now = System.currentTimeMillis();
        int updated =
                jdbc.update(
                        "UPDATE taskflow_environment SET value = ?, updated_at = ? WHERE name = ?",
                        value,
                        now,
                        name);
        if (updated == 0) {
            jdbc.update(
                    "INSERT INTO taskflow_environment (name, value, tags, created_at, updated_at)"
                            + " VALUES (?, ?, NULL, ?, ?)",
                    name,
                    value,
                    now,
                    now);
        }
    }

    @Override
    public void delete(String name) {
        jdbc.update("DELETE FROM taskflow_environment WHERE name = ?", name);
    }

    @Override
    public void setTagsJson(String name, String tagsJson) {
        jdbc.update(
                "UPDATE taskflow_environment SET tags = ?, updated_at = ? WHERE name = ?",
                tagsJson,
                System.currentTimeMillis(),
                name);
    }
}
