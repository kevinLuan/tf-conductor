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
package cn.taskflow.conductor.core.approval;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cn.taskflow.conductor.core.model.approval.ApprovalForm;
import cn.taskflow.conductor.core.model.approval.ApprovalFormRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 调用 taskflow {@code POST /api/internal/approval-forms} 建单。
 *
 * <p>开启：{@code conductor.taskflow.approval.client=http}，并配置 {@code
 * conductor.taskflow.approval.base-url}。
 */
@Component
@ConditionalOnProperty(name = "conductor.taskflow.approval.client", havingValue = "http")
public class HttpApprovalFormClient implements ApprovalFormClient {

    static final String TOKEN_HEADER = "X-Taskflow-Internal-Token";
    static final String CREATE_PATH = "/api/internal/approval-forms";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String token;

    @Autowired
    public HttpApprovalFormClient(
            ObjectMapper objectMapper,
            @Value("${conductor.taskflow.approval.base-url}") String baseUrl,
            @Value("${conductor.taskflow.approval.token:}") String token) {
        this(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                objectMapper,
                baseUrl,
                token);
    }

    HttpApprovalFormClient(
            HttpClient httpClient, ObjectMapper objectMapper, String baseUrl, String token) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.token = token;
    }

    /** POST taskflow 内部建单接口，非 2xx 或业务 code 非成功则抛错。 */
    @Override
    public ApprovalForm create(ApprovalFormRequest request) {
        try {
            HttpRequest.Builder builder =
                    HttpRequest.newBuilder()
                            .uri(URI.create(baseUrl + CREATE_PATH))
                            .timeout(Duration.ofSeconds(15))
                            .header("Content-Type", "application/json")
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            objectMapper.writeValueAsString(request)));
            if (StringUtils.isNotBlank(token)) {
                builder.header(TOKEN_HEADER, token);
            }
            HttpResponse<String> response =
                    httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "taskflow 建单失败 HTTP "
                                + response.statusCode()
                                + ": "
                                + truncate(response.body()));
            }
            return parseBody(response.body());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("调用 taskflow 建单失败", e);
        }
    }

    /** 兼容直接对象或 {@code {code, data}} 包装；从 data 解 formId / 审批人。 */
    ApprovalForm parseBody(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode data = root.has("data") ? root.get("data") : root;
        int code = root.has("code") ? root.get("code").asInt(200) : 200;
        if (code != 200 && code != 0) {
            String message = root.has("message") ? root.get("message").asText() : body;
            throw new IllegalStateException("taskflow 建单失败 code=" + code + ": " + message);
        }
        return ApprovalForm.builder()
                .formId(text(data, "formId"))
                .workflowId(text(data, "workflowId"))
                .taskId(text(data, "taskId"))
                .approver(firstText(data, "approver", "assignee"))
                .build();
    }

    private static String text(JsonNode node, String field) {
        return node != null && node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    /** 按字段顺序取第一个非空文本，兼容 approver / assignee。 */
    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static String stripTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > 500 ? body.substring(0, 500) : body;
    }
}
