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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * 扫描 {@code cn.taskflow.conductor} 下的评估器、AI 服务、FOR 循环壳、HUMAN 审批单、独立 SecretStore / EnvironmentStore
 * 与 Schema 登记。
 *
 * <p>通过 Spring Boot AutoConfiguration 注册，避免修改 Conductor 主类的 {@code @ComponentScan}。
 */
@AutoConfiguration
@ComponentScan(
        basePackages = "cn.taskflow.conductor",
        excludeFilters = {
            @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = TaskflowAutoConfiguration.class)
        })
public class TaskflowAutoConfiguration {}
