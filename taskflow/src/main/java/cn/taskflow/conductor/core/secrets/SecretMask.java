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

import java.time.Instant;

import org.conductoross.conductor.model.secret.CredentialMeta;

/** 与 {@code EnvVariableSecretsDAO} 相同的 partial 规则。 */
final class SecretMask {

    private SecretMask() {}

    /** 与 OSS env secrets 相同：短于等于 8 位显示 {@code ...}，否则保留首尾各 4 位。 */
    static String partial(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.length() <= 8) {
            return "...";
        }
        return value.substring(0, 4) + "..." + value.substring(value.length() - 4);
    }

    /** store 记录转列表元数据，带创建/更新时间。 */
    static CredentialMeta toMeta(SecretRecord record) {
        return CredentialMeta.builder()
                .name(record.name())
                .partial(partial(record.value()))
                .createdAt(Instant.ofEpochMilli(record.createdAt()))
                .updatedAt(Instant.ofEpochMilli(record.updatedAt()))
                .build();
    }

    /** 环境回落项转列表元数据，无时间戳。 */
    static CredentialMeta toMeta(String name, String value) {
        return CredentialMeta.builder().name(name).partial(partial(value)).build();
    }
}
