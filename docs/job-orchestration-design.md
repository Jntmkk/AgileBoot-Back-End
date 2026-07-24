# 通用任务编排层设计文档

> 生成时间：2026-07-23  
> 状态：设计稿，未实现  
> 范围：解决小红书、B 站、ASR 等多步骤任务需要跨节点执行的调度问题

---

## 1. 背景与目标

当前业务中已出现多种"多步骤、跨节点"场景：

- 小红书账号登录/发布：需要走到住宅 IP 节点上的 `xiaohongshu-mcp` 容器
- B 站 UP 主动态跟踪 → 下载音频 → 语音转写 → AI 总结：涉及后端 API、下载 Worker、ASR Worker、LLM
- 未来可能的小红书定时发布、批量互动、人工审核等

这些场景的共性：

1. 一个任务拆成多个步骤
2. 不同步骤需要在不同节点执行（Java 后端、住宅 IP 节点、GPU/M1 机器、Python Worker）
3. 步骤之间需要传递产物（音频、文本、JSON）
4. 需要重试、超时、死信、可观测性
5. 部分步骤需要人工介入（扫码、审核）

因此需要一套**通用的任务编排层**：控制面留在 Java/AgileBoot，执行面交给跨语言的 Node Worker。

---

## 2. 总体架构

```
┌─────────────────────────────────────┐
│         AgileBoot 后端 (Java)        │
│  任务模板 · 任务实例 · 调度器 · 回调处理  │
└─────────────────┬───────────────────┘
                  │ HTTP / Redis
        ┌─────────┼─────────┐
        ▼         ▼         ▼
   xhs 住宅节点   ASR Worker   B站下载 Worker
  (Docker sidecar) (M1/GPU)    (Docker)
```

### 核心原则

- **控制面在 Java**：调度、状态机、权限、后台管理、产物元数据
- **执行面无状态**：Worker 只负责领任务、执行、回调，不保存业务状态
- **语言无关**：Worker 可用 Python、Go、Node 任意实现
- **按需选择 push/poll**：NAT 后节点用 poll，有稳定地址的节点用 push

---

## 3. 数据模型

详见 SQL 文件：`sql/job_orchestration_20260723.sql`

### 3.1 表清单

| 表名 | 作用 |
|---|---|
| `job_template` | 任务模板 |
| `job_step_template` | 步骤模板（依赖、能力、重试策略） |
| `job_instance` | 任务的一次具体运行 |
| `job_step_instance` | 每个步骤的运行实例 |
| `job_step_log` | 步骤执行日志 |
| `job_artifact` | 中间产物 |
| `job_event` | 外部事件（人工扫码、回调等） |
| `job_node` | 节点注册表 |
| `job_node_heartbeat` | 节点心跳历史 |

### 3.2 关键设计点

- `job_step_template.dependency_step_codes`：JSON 数组，支持 DAG；早期线性链路放空数组即可
- `job_step_template.executor_capability`：例如 `xhs:login`、`asr:cpu`、`bili:download`、`llm:api`
- `job_instance.context_json`：全局上下文，各步骤输出汇总，后续步骤可读取任意前置结果
- `job_step_instance.input_artifact_ids / output_artifact_ids`：大文件引用，库里只存产物 ID
- `job_node.capabilities`：节点能力标签数组，调度器据此匹配步骤
- `job_node.protocol`：`poll` / `push` / `hybrid`

---

## 4. Node Worker HTTP 协议

### 4.1 认证

所有请求统一带令牌：

```http
X-Node-Token: <node_token>
```

- `node_token` 在 `job_node.token` 中预置，admin 注册节点后下发
- 回调可额外带一次性 `X-Callback-Token` 防重放（可选）

### 4.2 Worker → 调度器接口

#### 4.2.1 长轮询拉任务

```http
GET /api/v1/worker/tasks/next?capabilities=asr:cpu,asr:mps&timeoutSeconds=30
X-Node-Token: <token>
```

**200 OK**（已原子分配）：

```json
{
  "stepInstanceId": 10086,
  "jobInstanceId": 2024,
  "stepCode": "transcribe",
  "stepName": "语音转写",
  "jobTemplateCode": "bili_track_asr_summary",
  "input": {
    "audioArtifactId": 42,
    "language": "zh"
  },
  "inputArtifactUrls": {
    "audio": "https://scheduler.internal/api/v1/artifacts/42/download?token=xxx"
  },
  "callbackUrl": "https://scheduler.internal/api/v1/worker/callback",
  "deadlineAt": "2026-07-23T16:00:00Z"
}
```

**204 No Content**：超时无任务。

#### 4.2.2 心跳上报

```http
POST /api/v1/worker/heartbeat
X-Node-Token: <token>
```

```json
{
  "nodeId": "asr-m1-dev",
  "status": "ONLINE",
  "currentLoad": 1,
  "capabilities": ["asr:cpu", "asr:mps"],
  "version": "1.0.0",
  "ipAddress": "192.168.1.10",
  "metrics": {
    "cpuPercent": 45,
    "memoryFreeMb": 2048,
    "diskFreeGb": 120
  }
}
```

#### 4.2.3 执行回调

```http
POST /api/v1/worker/callback
X-Node-Token: <token>
```

```json
{
  "stepInstanceId": 10086,
  "nodeId": "asr-m1-dev",
  "status": "COMPLETED",
  "output": {
    "text": "大家好，今天这期视频讲的是...",
    "durationSeconds": 600
  },
  "outputArtifactIds": [43],
  "metrics": {
    "durationMs": 3700
  }
}
```

`status` 取值：

| 值 | 含义 |
|---|---|
| `RUNNING` | 开始执行 |
| `PROGRESS` | 进度更新，需带 `progressPercent` |
| `COMPLETED` | 成功 |
| `FAILED` | 失败，需带 `error` |

#### 4.2.4 产物上传

```http
POST /api/v1/worker/artifacts
X-Node-Token: <token>
Content-Type: multipart/form-data
```

表单字段：`stepInstanceId`、`artifactType`、`file`

**201 Created**：

```json
{
  "artifactId": 43,
  "downloadUrl": "https://scheduler.internal/api/v1/artifacts/43/download?token=xxx",
  "storageType": "local"
}
```

### 4.3 调度器 → Worker 接口（push 模式）

#### 4.3.1 推送任务

```http
POST /execute
X-Node-Token: <token>
```

```json
{
  "stepInstanceId": 10086,
  "jobInstanceId": 2024,
  "stepCode": "transcribe",
  "stepName": "语音转写",
  "jobTemplateCode": "bili_track_asr_summary",
  "input": { ... },
  "inputArtifactUrls": { ... },
  "callbackUrl": "https://scheduler.internal/api/v1/worker/callback",
  "deadlineAt": "2026-07-23T16:00:00Z"
}
```

**202 Accepted**：

```json
{
  "accepted": true,
  "executionId": "10086-uuid",
  "message": "accepted"
}
```

**429 Too Many Requests**：Worker 已满负载。

#### 4.3.2 健康检查

```http
GET /health
X-Node-Token: <token>
```

```json
{
  "status": "UP",
  "nodeId": "asr-m1-dev",
  "currentLoad": 1,
  "maxConcurrent": 2
}
```

---

## 5. Java 调度器核心流程

### 5.1 组件职责

| 组件 | 职责 |
|---|---|
| `TaskScheduler` | 定时扫描 DB，找出可执行步骤 |
| `TaskAllocator` | 按能力 + 负载匹配节点 |
| `TaskDispatcher` | push 直接 HTTP 调 Worker；poll 写入 Redis 队列 |
| `CallbackHandler` | 处理 Worker 回调，驱动状态流转 |
| `HumanEventHandler` | 消费人工事件 |
| `Watchdog` | 扫描超时、离线、死信 |
| `NodeHeartbeatMonitor` | 根据心跳更新节点在线状态 |

### 5.2 调度循环

1. 查询 `status = PENDING` 且依赖已满足的步骤
2. `SELECT ... FOR UPDATE SKIP LOCKED` 抢占（防并发双发）
3. 按 `executor_capability` 匹配在线节点
4. 过滤 `current_load >= max_concurrent` 的节点
5. 选择 `current_load` 最小的节点
6. **push**：POST `/execute`；**poll**：`LPUSH redis:job:queue:{nodeId}`
7. 更新 `step_instance.status = RUNNING`、`assigned_node_id`、`timeout_at`
8. 节点 `current_load + 1`

### 5.3 回调处理

- `COMPLETED`：写 output/artifact → 合并到 `job_instance.context_json` → 创建后续 PENDING 步骤 → 无后续则 job 完成
- `FAILED`：`retry_count + 1` → 未超限则按策略重试 → 超限则 DEAD
- `PROGRESS`：只写日志，不推进状态
- 幂等：终态步骤丢弃重复回调

### 5.4 看门狗

| 扫描对象 | 触发条件 | 动作 |
|---|---|---|
| `RUNNING` 步骤 | `timeout_at < now()` | 标记 `TIMEOUT`，按失败重试 |
| `RUNNING` 步骤 | 所属节点离线 | 标记失败，换节点重试 |
| `WAITING_HUMAN` 步骤 | 超过人工超时 | 标记失败 |
| 节点 | 心跳超时 | `status = OFFLINE` |

---

## 6. 人工步骤与事件驱动

### 6.1 HUMAN 步骤状态

```
PENDING
  │
  ▼（调度器发现是 HUMAN，不分配节点）
WAITING_HUMAN
  │
  ▼（外部事件到达）
COMPLETED / FAILED
```

### 6.2 事件来源

| 场景 | 事件类型 | 来源 |
|---|---|---|
| 小红书/B 站扫码完成 | `qr_scanned` | 前端轮询成功后写入 |
| 人工审核通过 | `manual_approved` | 管理后台按钮 |
| 定时触发 | `schedule_tick` | 定时任务 |
| 外部 webhook | `webhook` | 第三方系统 |

### 6.3 小红书登录是否纳入编排？

**建议不纳入**。现有 xhs 登录是用户在前端弹窗等待的同步交互，已经有成熟的轮询闭环。强行改成 HUMAN 步骤会降低用户体验。

通用编排层优先用于**异步流水线**：

- B 站跟踪 → 下载 → 转写 → 总结
- 小红书定时发布、批量互动
- 需要人工审核的内容处理

---

## 7. 产物存储策略

### 7.1 存储类型

| 类型 | 适用内容 | 实现 |
|---|---|---|
| `db` | 转写文本、总结 JSON、原始 API 响应 | `job_artifact.content` |
| `local` | 音频、视频、截图 | 服务器本地磁盘 |
| `oss/minio` | 同上，多机共享 | 对象存储 |

### 7.2 推荐路径（早期）

**local + db 混合**：

- 文本/JSON 直接落 DB
- 音频/视频落本地盘，路径规则：
  ```
  /data/artifacts/{bizType}/{jobInstanceId}/{stepCode}/{artifactId}-{filename}
  ```

### 7.3 上传流程

1. 调度器创建 `job_artifact` 记录，`status = PENDING`
2. Worker 从 `inputArtifactUrls` 下载输入
3. Worker POST multipart 到 `/api/v1/worker/artifacts`
4. 调度器写盘，更新 `status = VALID`
5. Worker 回调里引用 `outputArtifactIds`

### 7.4 清理策略

- 音频/视频：转写完成后 7 天清理
- 原始 API 响应：保留 30 天
- 总结结果：长期保留

---

## 8. 重试与死信

### 8.1 重试策略

`job_step_template` 已定义：

- `retry_times`：最大重试次数
- `retry_interval_seconds`：基础间隔
- 可扩展 `retry_backoff_multiplier`（默认 1，指数退避时设为 2）

### 8.2 失败流转

```
FAILED (retry_count < max)
  │
  ▼（设置 next_retry_at = now + interval * multiplier^retry_count）
PENDING
```

### 8.3 死信处理

- 重试耗尽 → `step_instance.status = DEAD`，`job_instance.status = FAILED`
- 管理台提供"重试死信"按钮：复制步骤或重置为 PENDING
- `job_step_log` 记录每次失败原因

---

## 9. 可观测性

### 9.1 数据

- `job_step_log`：每步全生命周期日志
- `job_node_heartbeat`：节点负载曲线
- `job_artifact`：产物血缘

### 9.2 管理台页面

- 任务实例列表：bizKey、状态、当前步骤、起止时间
- 步骤实例列表：节点、状态、重试次数、耗时
- 步骤日志：时间轴
- 产物管理：下载、查看、清理
- 死信队列：一键重试
- 节点管理：在线状态、负载、能力标签

### 9.3 可选指标

- `job_step_total`（按 step_code、status）
- `job_step_duration_seconds`
- `job_node_current_load`
- `job_dead_letter_total`

---

## 10. 安全与隔离

### 10.1 认证

- `X-Node-Token`：节点级长期令牌
- `X-Callback-Token`：单次回调令牌（可选）
- 管理台操作需 `social:node:*` 权限

### 10.2 隔离

- **能力隔离**：xhs 节点只能领到 xhs 任务
- **网络隔离**：住宅节点不访问内网其他服务
- **资源隔离**：Docker 限制 CPU/内存

### 10.3 产物安全

- 下载 URL 带短期 token
- Worker 不直接连 DB
- 敏感音频不长期外置

---

## 11. 与现有流程的关系

| 现有能力 | 处理方式 |
|---|---|
| 小红书扫码登录 | **保持现状**，不纳入编排层 |
| 小红书账号容器（xiaohongshu-mcp） | 被 Node Worker sidecar 包装，未来可编排"发布/评论"任务 |
| B 站登录态（social_credential） | 新流水线直接复用 |
| B 站 API 客户端（BiliApiClient） | 作为 `java-executor` 能力执行 fetch_space 等步骤 |

**迁移原则**：新场景用新架构，旧场景逐步迁移，不一次性重构。

---

## 12. 示例工作流模板

### 12.1 B 站跟踪 UP 主 → 转写 → 总结

```json
{
  "templateCode": "bili_track_asr_summary",
  "bizType": "BILI_TRACK",
  "steps": [
    {
      "stepCode": "fetch_space",
      "stepName": "获取空间投稿",
      "stepType": "AUTO",
      "executorCapability": "bili:api",
      "timeoutSeconds": 30,
      "retryTimes": 3
    },
    {
      "stepCode": "download_audio",
      "stepName": "下载音频",
      "stepType": "AUTO",
      "executorCapability": "bili:download",
      "dependencyStepCodes": ["fetch_space"],
      "timeoutSeconds": 300,
      "retryTimes": 2
    },
    {
      "stepCode": "transcribe",
      "stepName": "语音转写",
      "stepType": "AUTO",
      "executorCapability": "asr:cpu",
      "dependencyStepCodes": ["download_audio"],
      "timeoutSeconds": 600,
      "retryTimes": 2
    },
    {
      "stepCode": "summarize",
      "stepName": "AI总结",
      "stepType": "AUTO",
      "executorCapability": "llm:api",
      "dependencyStepCodes": ["transcribe"],
      "timeoutSeconds": 60,
      "retryTimes": 3
    }
  ]
}
```

### 12.2 小红书定时发布（未来）

```json
{
  "templateCode": "xhs_scheduled_publish",
  "bizType": "SOCIAL_XHS",
  "steps": [
    {
      "stepCode": "prepare_content",
      "stepType": "AUTO",
      "executorCapability": "java"
    },
    {
      "stepCode": "human_review",
      "stepType": "HUMAN",
      "executorCapability": null
    },
    {
      "stepCode": "publish_note",
      "stepType": "AUTO",
      "executorCapability": "xhs:publish"
    }
  ]
}
```

---

## 13. Worker 部署形态

| 节点 | 存在形式 | 运行位置 |
|---|---|---|
| xhs 住宅节点 Worker | **Docker sidecar 容器** | 和 `xiaohongshu-mcp` 同机同 compose |
| B 站下载 Worker | **Docker 容器** | 后端服务器或独立轻量云主机 |
| ASR M1 开发机 | **本机 Python 进程** | macOS，用 MPS 加速 |
| 未来 ASR GPU 服务器 | **Docker + nvidia-runtime** | 独立 GPU 机器 |
| Java 直连执行器 | **AgileBoot 后端自身** | 生产后端容器 |

### 13.1 xhs 住宅节点 sidecar 示例

在 `SocialMedia-Hub/node/docker-compose.yml` 中新增：

```yaml
smh-node-agent:
  image: registry.cn-hangzhou.aliyuncs.com/u-rep/smh-node-agent:latest
  container_name: smh-node-agent
  restart: unless-stopped
  environment:
    - NODE_ID=xhs-node-mac
    - SCHEDULER_URL=https://agile.frxxz.top/prod-api
    - NODE_TOKEN=xxx
    - CAPABILITIES=xhs:login,xhs:publish
  networks:
    - smh
```

它和 `xiaohongshu-mcp` 容器同网络，可直接通过容器名调用。

### 13.2 ASR M1 开发机

不建议用 Docker，直接用本机 Python 进程：

```bash
conda activate funasr
python asr_worker.py
```

用 `launchd` 或 `tmux` 保持常驻。`device="mps"` 在 macOS 本机进程里最稳定。

---

## 14. 落地建议顺序

1. **最小闭环**：B 站 `fetch_space → download_audio`（Java 调度器 + 下载 Worker）
2. **接入 ASR**：M1 本机起 FastAPI Worker，实现 `/execute` + callback
3. **补齐总结**：LLM 总结步骤 + 看门狗
4. **扩展 UI**：管理台任务列表、死信队列、节点管理
5. **迁移 xhs**：有需要时把发布/评论类任务纳入编排

---

## 15. 相关文件

- SQL：`sql/job_orchestration_20260723.sql`
- 本设计稿：`docs/job-orchestration-design.md`
