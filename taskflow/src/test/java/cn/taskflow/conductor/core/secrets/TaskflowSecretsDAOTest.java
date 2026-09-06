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

import org.conductoross.conductor.model.secret.CredentialMeta;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TaskflowSecretsDAOTest {

    private static final String PREFIX = "CONDUCTOR_SECRET_";
    private static final String FALLBACK_KEY = "TF_SECRETS_DAO_TEST_FALLBACK";

    private TaskflowSecretsDAO dao;

    @Before
    public void setUp() {
        dao = new TaskflowSecretsDAO(sqliteStore(), true, PREFIX);
        System.setProperty(PREFIX + FALLBACK_KEY, "env-value-1234");
    }

    @After
    public void tearDown() {
        System.clearProperty(PREFIX + FALLBACK_KEY);
    }

    @Test
    public void putGetDeleteRoundTrip() {
        dao.putSecret("DB_PASSWORD", "s3cr3t-value");
        assertEquals("s3cr3t-value", dao.getSecret("DB_PASSWORD"));
        assertTrue(dao.secretExists("DB_PASSWORD"));
        assertEquals(
                "s3cr...alue",
                dao.listWithMeta().stream()
                        .filter(item -> "DB_PASSWORD".equals(item.getName()))
                        .findFirst()
                        .orElseThrow()
                        .getPartial());
        dao.deleteSecret("DB_PASSWORD");
        assertFalse(dao.secretExists("DB_PASSWORD"));
    }

    @Test
    public void storeWinsOverEnvFallback() {
        dao.putSecret(FALLBACK_KEY, "store-value");
        assertEquals("store-value", dao.getSecret(FALLBACK_KEY));
    }

    @Test
    public void readsEnvWhenStoreMisses() {
        assertEquals("env-value-1234", dao.getSecret(FALLBACK_KEY));
        List<CredentialMeta> metas = dao.listWithMeta();
        assertTrue(metas.stream().anyMatch(item -> FALLBACK_KEY.equals(item.getName())));
    }

    @Test
    public void envFallbackCanBeDisabled() {
        TaskflowSecretsDAO noFallback = new TaskflowSecretsDAO(sqliteStore(), false, PREFIX);
        assertNull(noFallback.getSecret(FALLBACK_KEY));
    }

    private static JdbcSecretStore sqliteStore() {
        SingleConnectionDataSource dataSource =
                new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        dataSource.setDriverClassName("org.sqlite.JDBC");
        return new JdbcSecretStore(dataSource);
    }
}
