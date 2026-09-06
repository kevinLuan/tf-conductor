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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.conductoross.conductor.dao.SecretsDAO;
import org.conductoross.conductor.model.secret.CredentialMeta;

import com.netflix.conductor.core.env.EnvVarLookup;

/** 可写 SecretsDAO。写只进 {@link SecretStore}；读可回落到 {@code CONDUCTOR_SECRET_*}。 */
public class TaskflowSecretsDAO implements SecretsDAO {

    private final SecretStore store;
    private final boolean envFallback;
    private final String envPrefix;

    public TaskflowSecretsDAO(SecretStore store, boolean envFallback, String envPrefix) {
        this.store = store;
        this.envFallback = envFallback;
        this.envPrefix = envPrefix == null ? "CONDUCTOR_SECRET_" : envPrefix;
    }

    /** 先读 store，未命中且开启回落时再读 {@code CONDUCTOR_SECRET_*}。 */
    @Override
    public String getSecret(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        return store.get(name)
                .map(SecretRecord::value)
                .orElseGet(() -> envFallback ? EnvVarLookup.lookup(envPrefix, name) : null);
    }

    /** 与 {@link #getSecret} 同一套查找规则。 */
    @Override
    public boolean secretExists(String name) {
        return getSecret(name) != null;
    }

    /** 返回 store 与（可选）环境回落的名称列表。 */
    @Override
    public List<String> listSecretNames() {
        return listWithMeta().stream().map(CredentialMeta::getName).toList();
    }

    /** 只写入 store，不写进程环境变量。 */
    @Override
    public void putSecret(String name, String value) {
        if (StringUtils.isBlank(name) || value == null || value.isEmpty()) {
            throw new IllegalArgumentException("secret name and value are required");
        }
        store.put(name.trim(), value);
    }

    /** 只删 store 中的记录。 */
    @Override
    public void deleteSecret(String name) {
        if (StringUtils.isBlank(name)) {
            return;
        }
        store.delete(name);
    }

    /** store 优先；同名环境变量不覆盖 store。值以 partial 脱敏。 */
    @Override
    public List<CredentialMeta> listWithMeta() {
        Map<String, CredentialMeta> byName = new LinkedHashMap<>();
        for (SecretRecord record : store.list()) {
            byName.put(record.name(), SecretMask.toMeta(record));
        }
        if (envFallback) {
            EnvVarLookup.allWithPrefix(envPrefix)
                    .forEach(
                            (name, value) ->
                                    byName.putIfAbsent(name, SecretMask.toMeta(name, value)));
        }
        return new ArrayList<>(byName.values());
    }
}
