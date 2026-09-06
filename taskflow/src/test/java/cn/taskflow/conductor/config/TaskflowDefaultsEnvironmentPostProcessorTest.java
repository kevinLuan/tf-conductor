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
package cn.taskflow.conductor.config;

import java.util.Map;

import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TaskflowDefaultsEnvironmentPostProcessorTest {

    @Test
    public void fillsTypesWhenJdbcDb() {
        StandardEnvironment env = environment(Map.of("conductor.db.type", "sqlite"));
        new TaskflowDefaultsEnvironmentPostProcessor()
                .postProcessEnvironment(env, new SpringApplication());
        assertEquals("taskflow", env.getProperty("conductor.secrets.type"));
        assertEquals("taskflow", env.getProperty("conductor.environment.type"));
    }

    @Test
    public void skipsWhenNotJdbcDb() {
        StandardEnvironment env = environment(Map.of("conductor.db.type", "memory"));
        new TaskflowDefaultsEnvironmentPostProcessor()
                .postProcessEnvironment(env, new SpringApplication());
        assertNull(env.getProperty("conductor.secrets.type"));
        assertNull(env.getProperty("conductor.environment.type"));
    }

    @Test
    public void doesNotOverrideExplicitTypes() {
        StandardEnvironment env =
                environment(
                        Map.of(
                                "conductor.db.type",
                                "postgres",
                                "conductor.secrets.type",
                                "env",
                                "conductor.environment.type",
                                "noop"));
        new TaskflowDefaultsEnvironmentPostProcessor()
                .postProcessEnvironment(env, new SpringApplication());
        assertEquals("env", env.getProperty("conductor.secrets.type"));
        assertEquals("noop", env.getProperty("conductor.environment.type"));
    }

    private static StandardEnvironment environment(Map<String, Object> values) {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("user", values));
        return env;
    }
}
