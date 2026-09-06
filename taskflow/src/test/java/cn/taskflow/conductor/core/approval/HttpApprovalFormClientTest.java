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
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.taskflow.conductor.core.model.approval.ApprovalForm;
import cn.taskflow.conductor.core.model.approval.ApprovalFormRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HttpApprovalFormClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private final AtomicReference<String> lastToken = new AtomicReference<>();
    private final AtomicReference<String> lastBody = new AtomicReference<>();

    @Before
    public void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(
                HttpApprovalFormClient.CREATE_PATH,
                exchange -> {
                    lastToken.set(
                            exchange.getRequestHeaders()
                                    .getFirst(HttpApprovalFormClient.TOKEN_HEADER));
                    lastBody.set(
                            new String(
                                    exchange.getRequestBody().readAllBytes(),
                                    StandardCharsets.UTF_8));
                    byte[] resp =
                            "{\"code\":200,\"data\":{\"formId\":\"f-1\",\"workflowId\":\"wf-1\",\"taskId\":\"t-1\",\"approver\":\"u-1\"}}"
                                    .getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, resp.length);
                    exchange.getResponseBody().write(resp);
                    exchange.close();
                });
        server.start();
    }

    @After
    public void tearDown() {
        server.stop(0);
    }

    @Test
    public void postsWrappedDataResultAndToken() throws Exception {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        HttpApprovalFormClient client =
                new HttpApprovalFormClient(
                        java.net.http.HttpClient.newHttpClient(), objectMapper, baseUrl, "secret");
        ApprovalForm form =
                client.create(
                        ApprovalFormRequest.builder()
                                .tenantId("app-9")
                                .workflowId("wf-1")
                                .taskId("t-1")
                                .approver("u-1")
                                .formData(Map.of("days", 3))
                                .build());
        assertEquals("f-1", form.getFormId());
        assertEquals("secret", lastToken.get());
        JsonNode body = objectMapper.readTree(lastBody.get());
        assertEquals("app-9", body.get("tenantId").asText());
        assertEquals(3, body.get("formData").get("days").asInt());
        assertTrue(body.has("workflowId"));
    }
}
