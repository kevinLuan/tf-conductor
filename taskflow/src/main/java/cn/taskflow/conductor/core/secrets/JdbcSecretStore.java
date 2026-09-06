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

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/** 复用进程 DataSource。表 {@code taskflow_secret}，与环境变量分表。 */
public final class JdbcSecretStore implements SecretStore {

    private static final String CREATE_TABLE =
            """
            CREATE TABLE IF NOT EXISTS taskflow_secret (
              name VARCHAR(100) NOT NULL PRIMARY KEY,
              value TEXT NOT NULL,
              created_at INTEGER NOT NULL,
              updated_at INTEGER NOT NULL
            )
            """;

    private static final RowMapper<SecretRecord> ROW_MAPPER =
            (rs, rowNum) ->
                    new SecretRecord(
                            rs.getString("name"),
                            rs.getString("value"),
                            rs.getLong("created_at"),
                            rs.getLong("updated_at"));

    private final JdbcTemplate jdbc;

    /** 启动时确保 {@code taskflow_secret} 表存在。 */
    public JdbcSecretStore(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
        jdbc.execute(CREATE_TABLE);
    }

    @Override
    public Optional<SecretRecord> get(String name) {
        List<SecretRecord> rows =
                jdbc.query(
                        "SELECT name, value, created_at, updated_at FROM taskflow_secret WHERE name = ?",
                        ROW_MAPPER,
                        name);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public List<SecretRecord> list() {
        return jdbc.query(
                "SELECT name, value, created_at, updated_at FROM taskflow_secret ORDER BY name",
                ROW_MAPPER);
    }

    /** 先 UPDATE，影响 0 行再 INSERT，兼容 sqlite / postgres / mysql。 */
    @Override
    public void put(String name, String value) {
        long now = System.currentTimeMillis();
        int updated =
                jdbc.update(
                        "UPDATE taskflow_secret SET value = ?, updated_at = ? WHERE name = ?",
                        value,
                        now,
                        name);
        if (updated == 0) {
            jdbc.update(
                    "INSERT INTO taskflow_secret (name, value, created_at, updated_at) VALUES (?, ?, ?, ?)",
                    name,
                    value,
                    now,
                    now);
        }
    }

    @Override
    public void delete(String name) {
        jdbc.update("DELETE FROM taskflow_secret WHERE name = ?", name);
    }
}
