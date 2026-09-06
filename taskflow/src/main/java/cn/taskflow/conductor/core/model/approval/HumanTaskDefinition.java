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
package cn.taskflow.conductor.core.model.approval;

import lombok.Data;

/**
 * Orkes {@code __humanTaskDefinition} 的轻量子集。
 *
 * <p>本模块只使用 {@code displayName}、{@code owner}、{@code userFormTemplate}。 {@code assignments} /
 * {@code autoClaim} / {@code assignmentCompletionStrategy} 不解释。
 */
@Data
public class HumanTaskDefinition {
    private String displayName;

    /** 单一审批人；taskflow 画布写入的是 {@code assignee}。 */
    private String assignee;

    /** Orkes 字段；缺省时回落到 {@code assignee}。 */
    private String owner;

    private UserFormTemplate userFormTemplate;
}
