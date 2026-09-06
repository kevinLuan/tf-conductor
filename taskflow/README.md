# conductor-taskflow

Taskflow 对 Conductor OSS 的扩展模块。包根路径 `cn.taskflow.conductor`。

本模块实现 SWITCH 的 `ai` / `if-else` 评估器、`type=FOR` 循环壳、HUMAN 到达后的轻量审批单（MOCK），以及 OSS 没有的 `/api/schema` 登记。对上游的钩子尽量薄，清单见仓库根目录 [`TF_CHANGE_LOG.md`](../TF_CHANGE_LOG.md)。扩展行为的单测只放在本模块。

## 依赖方向

评估器必须实现 `core` 的 `Evaluator` / `AdvancedEvaluator`，并使用 `WorkflowModel`、`ParametersUtils` 等 core 类型。Gradle 不允许循环依赖：

| 方向 | 说明 |
|---|---|
| `taskflow` → `core` / `common` | 实现 SPI、复用循环调度 |
| `server` → `taskflow` | 运行时把扩展放到 classpath |
| `core` 不依赖 `taskflow` | 通义千问 SDK 也只声明在本模块 |

启动时由 `TaskflowAutoConfiguration` 扫描 `cn.taskflow.conductor`（`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`），无需改 Conductor 主类的 `@ComponentScan`。

## SWITCH：`if-else`

Bean 名 / `evaluatorType`：`if-else`（`IfElseEvaluator`）。

`expression` 是 `ConditionRouter` 列表的 JSON。求值时用 `ParametersUtils.getTaskInputV2` 解析左右值里的 `${...}`，再按 AND/OR 与比较符判断，返回命中分支的 `action`（对应 `decisionCases` 的 key）。未命中返回 empty，`SwitchTaskMapper` 走 `defaultCase`。

无工作流上下文的 `evaluate(expression, input)` 会抛 `UnsupportedOperationException`。

### expression 形状

```json
[
  {
    "condition": {
      "operator": "AND",
      "children": [
        {
          "expression": "${workflow.input.status}",
          "operator": "EQUALS",
          "value": "approved"
        }
      ]
    },
    "action": "pass"
  }
]
```

`condition.operator`：`AND` / `OR`。  
谓词 `operator`：`EQUALS`、`NOT_EQUALS`、`GREATER_THAN`、`GREATER_THAN_OR_EQUAL`、`LESS_THAN`、`LESS_THAN_OR_EQUAL`、`EXISTS`（左侧包含右侧）、`NOT_EXISTS`（左侧非空）、`EMPTY`（左侧为空）。

数值比较把两侧 `trim` 后按 `double` 解析。求值异常记 error 日志并视为该谓词不匹配。

### SWITCH 任务示例

```json
{
  "type": "SWITCH",
  "name": "route",
  "taskReferenceName": "route",
  "evaluatorType": "if-else",
  "expression": "[{\"condition\":{\"operator\":\"AND\",\"children\":[{\"expression\":\"${workflow.input.status}\",\"operator\":\"EQUALS\",\"value\":\"approved\"}]},\"action\":\"pass\"}]",
  "decisionCases": {
    "pass": [{ "name": "ok", "taskReferenceName": "ok", "type": "SIMPLE" }]
  },
  "defaultCase": [{ "name": "reject", "taskReferenceName": "reject", "type": "SIMPLE" }]
}
```

## SWITCH：`ai`

Bean 名 / `evaluatorType`：`ai`（`AIEvaluator`）。

`expression` 是 `SwitchAiParams` JSON，目前只有 `instructions`。评估器取出 `decisionCases` 的 key，按插入顺序编号（从 1 起），拼分类提示词后调用 `LLMService`。模型须返回 `{"id": <n>}`；`id > 0` 且能对上某个 case 则选中该 key，否则 empty（走 `defaultCase`）。

当前 `LLMService` 实现是 `QwenService`（通义千问）。提示词模板：`src/main/resources/ai/prompt/classify.txt`。

### 配置

```properties
conductor.taskflow.qwen.api-key=<百炼 API Key>
conductor.taskflow.qwen.model=qwen-plus
```

未配置 API Key 时 `QwenService.query` 抛 `AICallerException`，SWITCH 任务会 FAILED。密钥不写进代码。

### SWITCH 任务示例

```json
{
  "type": "SWITCH",
  "name": "classify",
  "taskReferenceName": "classify",
  "evaluatorType": "ai",
  "expression": "{\"instructions\":\"按用户意图分类\"}",
  "decisionCases": {
    "refund": [{ "name": "refund", "taskReferenceName": "refund", "type": "SIMPLE" }],
    "consult": [{ "name": "consult", "taskReferenceName": "consult", "type": "SIMPLE" }]
  },
  "defaultCase": [{ "name": "other", "taskReferenceName": "other", "type": "SIMPLE" }]
}
```

`decisionCases` 的 key 顺序即模型看到的 id 顺序（`LinkedHashMap`）。分类逻辑在 `AIFactory.classify`。

## FOR

`For` 继承 `DoWhile`，只换任务类型（`@Component("FOR")`），列表迭代完全复用 DoWhile。`ForTaskMapper` 只产出 IN_PROGRESS 循环头，第一轮循环体仍由 `For.execute()` 调度。

| 约定 | 说明 |
|---|---|
| 集合字段 | 画布统一写 `inputParameters.items`（也认 WorkflowTask 顶层 `items`） |
| 循环输出 | `loopItem` / `loopIndex`（DoWhile 原语义） |
| `loopCondition` | FOR 不要求 |
| 空列表 | 立即完成，不跑循环体（DoWhile 已有行为） |

注册期校验（`WorkflowTaskTypeConstraint`）：必须有 `items`（顶层字段或 `inputParameters.items`）以及非空 `loopOver`。

### 任务示例

```json
{
  "type": "FOR",
  "name": "for_each",
  "taskReferenceName": "for_each",
  "inputParameters": {
    "items": "${workflow.input.items}"
  },
  "loopOver": [
    {
      "name": "body",
      "taskReferenceName": "body",
      "type": "SIMPLE",
      "inputParameters": {
        "item": "${for_each.output.loopItem}",
        "index": "${for_each.output.loopIndex}"
      }
    }
  ]
}
```

图遍历、Decider、重试把 FOR 与 DO_WHILE 同等视为循环头，判断入口是 `TaskType.isLoopTask(String)`。

## HUMAN 轻量审批单

OSS 的 `HUMAN` 只停在 `IN_PROGRESS`。本模块在任务进入该状态且 input 含 `__humanTaskDefinition` 时，按 Orkes 字段的**子集**建一张单（无认领、无会签、无 SLA）。

| 使用 | 忽略 |
|---|---|
| `displayName`、`userFormTemplate.name/version`、`assignee` / `owner`（可选） | `assignments`、`autoClaim`、`assignmentCompletionStrategy` |

单一审批人顺序：`__humanTaskDefinition.assignee` → `owner` → input `_createdBy` → 工作流 `createdBy` → `unknown`。  
租户键取工作流 `ownerApp`（或定义上的 `ownerApp`），随建单请求传给 taskflow。  
表单数据为 input 中去掉 `__humanTaskDefinition` / `__humanTaskProcessContext` / `_createdBy` 后的字段。建单成功后把 `formId` 写回任务 output。

默认仍是内存 MOCK。切到 taskflow 服务：

```properties
conductor.taskflow.approval.enabled=true
conductor.taskflow.approval.client=http
conductor.taskflow.approval.base-url=http://localhost:8081
conductor.taskflow.approval.token=${TASKFLOW_INTERNAL_TOKEN:}
```

`http` 客户端 `POST {base-url}/api/internal/approval-forms`，请求体与 `ApprovalFormRequest` 一致，响应按 taskflow `DataResult` 解出 `data`。监听器不用改。

```properties
conductor.taskflow.approval.enabled=true
conductor.taskflow.approval.client=mock
```

核心只补了一处：HUMAN 同步启动后补发 `TaskStatusListener.onTaskInProgress`。

## 其它类

| 类 | 作用 |
|---|---|
| `LLMService` | LLM 问答接口 |
| `AIFactory` | 拼提示词、解析 `{"id": n}`、映射回 case 名 |
| `TemplateProcessor` | `${a.b.c}` 简单替换（工具类；if-else 走 `ParametersUtils`） |
| `AICallerException` / `AIResult` | AI 调用失败与结果模型 |
| `OperatorEnum` / `LogicOperatorEnum` | if-else 比较符与 AND/OR |

## Secrets / Environment

OSS 默认 secrets 与 environment DAO 都只读。本模块用**两套独立存储**。HTTP 和占位符仍是两套 Conductor 合同。

| 默认开关 | DAO | 存储 | jdbc 表 | HTTP | 占位符 | 进程环境回落 |
|---|---|---|---|---|---|---|
| `conductor.secrets.type=taskflow`（模块在 classpath 上即默认） | `TaskflowSecretsDAO` | 跟 `conductor.db.type` | `taskflow_secret` | 现有 `SecretController` | `${workflow.secrets.X}` | `CONDUCTOR_SECRET_*` |
| `conductor.environment.type=taskflow`（同上） | `TaskflowEnvironmentDAO` | 跟 `conductor.db.type` | `taskflow_environment` | 本模块 `/api/environment`（对齐 Orkes EnvironmentClient） | `${workflow.env.X}` | `CONDUCTOR_ENV_*` |

只在 `conductor.db.type` 为 `sqlite` / `postgres` / `mysql` 时启用，复用进程 `DataSource`。其它库类型不接管，保持 OSS 只读实现。同名可以同时存在两边，互不影响。写只进各自 store。`conductor.secrets.type=env` 或 `conductor.environment.type=env` 可退回 OSS 只读实现。

环境变量 HTTP 对齐 SDK：

| 方法 | 路径 | 参数 / 响应 |
|---|---|---|
| `GET` | `/api/environment` | `[{ name, value, tags }]` |
| `GET` | `/api/environment/{key}` | text/plain 值 |
| `PUT` | `/api/environment/{key}` | body：text/plain 值（不可 null） |
| `DELETE` | `/api/environment/{key}` | text/plain 旧值 |
| `GET` | `/api/environment/{name}/tags` | `[{ key, value }]` |
| `PUT` | `/api/environment/{name}/tags` | body：`[{ key, value }]`，整表替换 |
| `DELETE` | `/api/environment/{name}/tags` | body：要删的 tags |

不必在 `application.properties` 里再写上述开关；`conductor.db.type` 沿用服务器已有配置即可。

## Schema

OSS 没有 `/api/schema`（#1118）。本模块默认挂上 `SchemaResource`，合同对齐 Orkes / SchemaClient：

| 方法 | 路径 | 行为 |
|---|---|---|
| `GET` | `/api/schema?short=` | 全部版本；`short=true` 只留 name / version / 时间 |
| `POST` | `/api/schema?newVersion=` | 单个 `SchemaDef` 或数组；按 name+version upsert，`newVersion=true` 则 max+1 |
| `GET` | `/api/schema/{name}` | 最新版本，没有则 404 |
| `GET` | `/api/schema/{name}/{version}` | 指定版本 |
| `DELETE` | `/api/schema/{name}` | 删该 name 全部版本 |
| `DELETE` | `/api/schema/{name}/{version}` | 删一个版本 |

只在 `conductor.db.type` 为 sqlite / postgres / mysql 时挂上，表 `taskflow_schema`。`conductor.taskflow.schema.enabled=false` 可关掉。core / rest 不改。

## 测试

全部在 `taskflow/src/test/java`：

| 测试 | 覆盖 |
|---|---|
| `IfElseEvaluatorTest` | 占位符解析、AND/OR、未命中 |
| `AIFactoryTest` | 分类 id → case、提示词清洗 |
| `TemplateProcessorTest` | 嵌套路径替换 |
| `SwitchTaskMapperTest` | 真实 `if-else` / `ai` 评估器走 `SwitchTaskMapper` |
| `ForTest` | 执行、`isLoopTask`、图上 `has` / `next` |
| `ForTaskMapperTest` | 只映射循环头 |
| `ForTaskConstraintTest` | `items` + `loopOver` 校验 |
| `HumanApprovalServiceTest` | 建单字段、审批人回落、幂等、formId 回写 |
| `HumanApprovalArrivalListenerTest` | 只处理 HUMAN |
| `HttpApprovalFormClientTest` | 调 taskflow 内部建单接口 |
| `TaskflowSecretsDAOTest` | 隐私配置读写、env 回落 |
| `TaskflowEnvironmentDAOTest` | 环境变量读写、env 回落、tags |
| `JdbcSecretStoreTest` | `taskflow_secret` upsert / 删除 |
| `JdbcEnvironmentStoreTest` | `taskflow_environment` upsert / tags / 与 secret 分表隔离 |
| `SchemaRegistryTest` | 版本递增、short 列表、删除、404 |
| `JdbcSchemaStoreTest` | sqlite 内存库 upsert / 删除 |
| `TaskflowDefaultsEnvironmentPostProcessorTest` | 默认打开 type=taskflow，不覆盖显式配置 |
| `TaskflowStoreAutoConfigurationTest` | `sqlite` / `postgres` / `mysql` 才挂 jdbc 存储 |

```bash
./gradlew :conductor-taskflow:test
```

## 相对 feiliu-conductor 的刻意差异

- 未迁入 `WorkflowExecutionLog` 事件发布；if-else 求值只打 debug 日志。
- 未迁入集成应用 / DAO 体系；AI 分类固定走通义千问。
- FOR 不提供 `element` / `index` 别名，也不写 `[$index]`；与 DoWhile 对齐为 `loopItem` / `loopIndex`。
- 画布集合字段用 `items`，不用 feiliu 的 `elements`。
