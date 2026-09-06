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

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/** 复用进程 DataSource。DDL / DML 用 sqlite 与 postgres 都能跑的子集。 */
public final class JdbcSchemaStore implements SchemaStore {

    private static final String CREATE_TABLE =
            """
            CREATE TABLE IF NOT EXISTS taskflow_schema (
              name VARCHAR(100) NOT NULL,
              version INTEGER NOT NULL,
              type VARCHAR(50) NOT NULL,
              data TEXT,
              external_ref TEXT,
              created_at INTEGER NOT NULL,
              updated_at INTEGER NOT NULL,
              created_by VARCHAR(50),
              updated_by VARCHAR(50),
              PRIMARY KEY (name, version)
            )
            """;

    private static final RowMapper<SchemaRecord> ROW_MAPPER =
            (rs, rowNum) ->
                    new SchemaRecord(
                            rs.getString("name"),
                            rs.getInt("version"),
                            rs.getString("type"),
                            rs.getString("data"),
                            rs.getString("external_ref"),
                            rs.getLong("created_at"),
                            rs.getLong("updated_at"),
                            rs.getString("created_by"),
                            rs.getString("updated_by"));

    private final JdbcTemplate jdbc;

    /** 启动时确保 {@code taskflow_schema} 表存在。 */
    public JdbcSchemaStore(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
        jdbc.execute(CREATE_TABLE);
    }

    @Override
    public Optional<SchemaRecord> get(String name, int version) {
        List<SchemaRecord> rows =
                jdbc.query(
                        "SELECT name, version, type, data, external_ref, created_at, updated_at, created_by, updated_by"
                                + " FROM taskflow_schema WHERE name = ? AND version = ?",
                        ROW_MAPPER,
                        name,
                        version);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public List<SchemaRecord> listAll() {
        return jdbc.query(
                "SELECT name, version, type, data, external_ref, created_at, updated_at, created_by, updated_by"
                        + " FROM taskflow_schema ORDER BY name, version",
                ROW_MAPPER);
    }

    @Override
    public int maxVersion(String name) {
        Integer max =
                jdbc.queryForObject(
                        "SELECT MAX(version) FROM taskflow_schema WHERE name = ?",
                        Integer.class,
                        name);
        return max == null ? 0 : max;
    }

    /** 先 UPDATE，影响 0 行再 INSERT。 */
    @Override
    public void put(SchemaRecord record) {
        int updated =
                jdbc.update(
                        "UPDATE taskflow_schema SET type = ?, data = ?, external_ref = ?, updated_at = ?,"
                                + " updated_by = ? WHERE name = ? AND version = ?",
                        record.type(),
                        record.dataJson(),
                        record.externalRef(),
                        record.updatedAt(),
                        record.updatedBy(),
                        record.name(),
                        record.version());
        if (updated == 0) {
            jdbc.update(
                    "INSERT INTO taskflow_schema (name, version, type, data, external_ref, created_at, updated_at,"
                            + " created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    record.name(),
                    record.version(),
                    record.type(),
                    record.dataJson(),
                    record.externalRef(),
                    record.createdAt(),
                    record.updatedAt(),
                    record.createdBy(),
                    record.updatedBy());
        }
    }

    @Override
    public void delete(String name, int version) {
        jdbc.update("DELETE FROM taskflow_schema WHERE name = ? AND version = ?", name, version);
    }

    @Override
    public void deleteAll(String name) {
        jdbc.update("DELETE FROM taskflow_schema WHERE name = ?", name);
    }
}
