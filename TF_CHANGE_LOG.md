# Taskflow 变更记录

只记录对上游 Conductor（`common` / `core` / `server` / `settings`）的侵入点，便于对齐升级。  
功能说明、配置与用法见 [`taskflow/README.md`](taskflow/README.md)。  
扩展行为的单测只放在 `taskflow`，不写入 `common` / `core`。

## 模块接入

| 位置 | 改动 |
|---|---|
| `settings.gradle` | `include 'taskflow'` |
| `server/build.gradle` | `implementation project(':conductor-taskflow')` |
| `core/build.gradle` | 注释：`core` 不反向依赖 `taskflow`（避免循环依赖） |

运行时由 AutoConfiguration 扫描 `cn.taskflow.conductor`，不改 `Conductor.java` 的 `@ComponentScan`。

## SWITCH：上下文评估

| 位置 | 改动 |
|---|---|
| `core/.../evaluators/AdvancedEvaluator.java` | **新增**接口：评估时传入 `WorkflowModel` / `WorkflowTask` |
| `core/.../mapper/SwitchTaskMapper.java` | `instanceof AdvancedEvaluator` 则走带上下文的 `evaluate`；否则保持原逻辑 |

供 `taskflow` 的 `evaluatorType=ai` / `if-else` 使用。javascript / value-param 等原评估器不变。

## FOR：当作循环头

| 位置 | 改动 |
|---|---|
| `common/.../TaskType.java` | 枚举末尾加 `FOR`（避免 Proto 序号前移）；`TASK_TYPE_FOR`、`BUILT_IN_TASKS`、`isLoopTask()` |
| `core/.../tasks/DoWhile.java` | 保护构造器供循环壳换类型（公开构造器加 `@Autowired`，避免双构造器时 Spring 走无参）；列表迭代同时认 `inputParameters.items` |
| `common/.../WorkflowTask.java` | `children` / `has` / `next` 把 `FOR` 当循环（体末回自身） |
| `common/.../WorkflowDef.java` | `getNextTask` 用 `isLoopTask` |
| `core/.../DeciderService.java` | 两处循环判断改为 `isLoopTask` |
| `core/.../WorkflowExecutorOps.java` | 重试 / 重跑 / 取消用 `isLoopTask` |
| `core/.../validations/WorkflowTaskTypeConstraint.java` | `FOR` 校验 `items`（字段或 `inputParameters.items`）+ `loopOver`；不要求 `loopCondition` |
| `core/.../service/WorkflowTestService.java` | 测试算子集合加入 `FOR` |

## HUMAN：到达时补发状态事件

| 位置 | 改动 |
|---|---|
| `core/.../WorkflowExecutorOps.java` | 同步启动 HUMAN 后调用 `taskStatusListener.onTaskInProgressIfEnabled`（HUMAN 不进队列，原先不会通知监听器） |

`taskflow` 订阅该事件建审批单。无 `__humanTaskDefinition` 的 HUMAN 不建单。表单与审批单落在 taskflow 服务，引擎只 HTTP 建单（或 MOCK）。

## Secrets / Environment：可写 DAO 默认接管

| 位置 | 改动 |
|---|---|
| `taskflow/.../TaskflowDefaultsEnvironmentPostProcessor` | classpath 上默认 `conductor.secrets.type=taskflow`、`conductor.environment.type=taskflow`，不写 `application.properties` |
| `taskflow/.../OnConductorJdbcDbType` | 只认 `conductor.db.type=sqlite|postgres|mysql`，走 jdbc；不实现 memory 存储 |

`SecretController` / `SecretsDAO` / `EnvironmentDAO` 接口未改。默认关掉 OSS 只读 `EnvironmentResource`，由子模块按 Orkes EnvironmentClient 提供 GET/PUT/DELETE 与 tags。隐私配置和环境变量分表：`taskflow_secret` / `taskflow_environment`。显式 `conductor.secrets.type=env` / `conductor.environment.type=env` 可退回 OSS。

## Environment：OSS 只读 Controller 让位

| 位置 | 改动 |
|---|---|
| `rest/.../EnvironmentResource.java` | `conductor.environment.type=env` 才启用，避免与 taskflow 全量 `/api/environment` 抢映射 |

## Schema：登记接口

| 位置 | 改动 |
|---|---|
| `taskflow/.../TaskflowSchemaConfiguration` | `conductor.db.type` 为 sqlite / postgres / mysql 时默认打开 `/api/schema` |

OSS / agentspan 没有 Schema Controller。实现只在子模块，不改 `rest`。
