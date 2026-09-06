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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.netflix.conductor.common.metadata.EnvironmentVariable;

import cn.taskflow.conductor.core.model.env.Tag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TaskflowEnvironmentDAOTest {

    private static final String PREFIX = "CONDUCTOR_ENV_";
    private static final String FALLBACK_KEY = "TF_ENV_DAO_TEST_FALLBACK";

    private TaskflowEnvironmentDAO dao;

    @Before
    public void setUp() {
        dao = new TaskflowEnvironmentDAO(sqliteStore(), true, PREFIX);
        System.setProperty(PREFIX + FALLBACK_KEY, "env-region");
    }

    @After
    public void tearDown() {
        System.clearProperty(PREFIX + FALLBACK_KEY);
    }

    @Test
    public void putGetDeleteRoundTrip() {
        dao.setEnvVariable("REGION", "us-east-1");
        assertEquals("us-east-1", dao.getEnvVariable("REGION"));
        assertTrue(dao.getAll().stream().anyMatch(item -> "REGION".equals(item.getName())));
        dao.delete("REGION");
        assertNull(dao.getEnvVariable("REGION"));
    }

    @Test
    public void storeWinsOverEnvFallback() {
        dao.setEnvVariable(FALLBACK_KEY, "store-region");
        assertEquals("store-region", dao.getEnvVariable(FALLBACK_KEY));
    }

    @Test
    public void readsEnvWhenStoreMisses() {
        assertEquals("env-region", dao.getEnvVariable(FALLBACK_KEY));
        assertTrue(
                dao.getAll().stream()
                        .map(EnvironmentVariable::getName)
                        .anyMatch(FALLBACK_KEY::equals));
    }

    @Test
    public void putGetDeleteAndTagsMatchSdk() {
        dao.setEnvVariable("REGION", "us-east-1");
        assertEquals("us-east-1", dao.getEnvVariable("REGION"));
        assertEquals("us-east-1", dao.listAll().get(0).getValue());
        assertTrue(dao.listAll().get(0).getTags().isEmpty());

        Tag tag = new Tag("department", "accounts");
        dao.setTags("REGION", List.of(tag));
        assertEquals(1, dao.getTags("REGION").size());
        assertEquals("department", dao.getTags("REGION").get(0).getKey());
        assertEquals(1, dao.listAll().get(0).getTags().size());

        dao.deleteTags("REGION", List.of(tag));
        assertTrue(dao.getTags("REGION").isEmpty());
        assertEquals("us-east-1", dao.deleteAndReturn("REGION"));
        assertNull(dao.getEnvVariable("REGION"));
    }

    @Test
    public void envFallbackCanBeDisabled() {
        TaskflowEnvironmentDAO noFallback =
                new TaskflowEnvironmentDAO(sqliteStore(), false, PREFIX);
        assertNull(noFallback.getEnvVariable(FALLBACK_KEY));
    }

    private static JdbcEnvironmentStore sqliteStore() {
        SingleConnectionDataSource dataSource =
                new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        dataSource.setDriverClassName("org.sqlite.JDBC");
        return new JdbcEnvironmentStore(dataSource);
    }
}
