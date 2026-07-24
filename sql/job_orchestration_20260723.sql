-- 通用任务编排层表结构（含节点注册表）
-- 导入：mysql --default-character-set=utf8mb4 -h <host> -u root -p <db> < sql/job_orchestration_20260723.sql

SET NAMES utf8mb4;

-- ----------------------------
-- 任务模板
-- ----------------------------
CREATE TABLE `job_template` (
    `id`           bigint auto_increment comment '模板ID' primary key,
    `template_code` varchar(64)             not null comment '模板编码（唯一，如 bili_track_asr_summary）',
    `template_name` varchar(128)            not null comment '模板名称',
    `biz_type`     varchar(32)             not null comment '业务类型（SOCIAL_XHS / BILI_TRACK / ASR_SUMMARY）',
    `description`  varchar(512)            null comment '描述',
    `status`       smallint     default 1  not null comment '状态（1启用 0停用）',
    `version`      int          default 1  not null comment '版本号',
    `remark`       varchar(255)            null comment '备注',
    `creator_id`   bigint                  null comment '创建者ID',
    `create_time`  datetime                null comment '创建时间',
    `updater_id`   bigint                  null comment '更新者ID',
    `update_time`  datetime                null comment '更新时间',
    `deleted`      tinyint      default 0  not null comment '删除标志（0存在 1删除）',
    UNIQUE KEY `uk_template_code` (`template_code`)
) engine = innodb
  default charset = utf8mb4 comment ='任务模板表';

-- ----------------------------
-- 任务步骤模板
-- ----------------------------
CREATE TABLE `job_step_template` (
    `id`           bigint auto_increment comment '步骤模板ID' primary key,
    `job_template_id` bigint               not null comment '所属任务模板ID',
    `step_code`    varchar(64)             not null comment '步骤编码（模板内唯一，如 fetch_space / download_audio / transcribe / summarize）',
    `step_name`    varchar(128)            not null comment '步骤名称',
    `step_type`    varchar(16)             not null comment '步骤类型（AUTO 自动 / HUMAN 人工 / GATEWAY 分支网关）',
    `order_index`  int          default 0  not null comment '排序（同层级展示/默认执行顺序）',
    `executor_capability` varchar(64)      null comment '执行器能力标签（xhs:login / asr:cpu / bili:api / java 等）',
    `timeout_seconds` int      default 300 not null comment '步骤超时时间（秒）',
    `retry_times`  int          default 3  not null comment '失败重试次数',
    `retry_interval_seconds` int default 60 not null comment '重试间隔（秒）',
    `dependency_step_codes` json            null comment '前置步骤 code 数组（JSON），空表示无依赖',
    `input_schema`  json                   null comment '输入参数 JSON Schema（校验用，可选）',
    `output_schema` json                   null comment '输出参数 JSON Schema（校验用，可选）',
    `node_selector` json                   null comment '节点选择策略（JSON：如 {capabilities:[asr], preferredNode:null}）',
    `description`  varchar(512)            null comment '描述',
    `status`       smallint     default 1  not null comment '状态（1启用 0停用）',
    `remark`       varchar(255)            null comment '备注',
    `creator_id`   bigint                  null comment '创建者ID',
    `create_time`  datetime                null comment '创建时间',
    `updater_id`   bigint                  null comment '更新者ID',
    `update_time`  datetime                null comment '更新时间',
    `deleted`      tinyint      default 0  not null comment '删除标志（0存在 1删除）',
    UNIQUE KEY `uk_job_template_step_code` (`job_template_id`, `step_code`),
    KEY `idx_job_template_id` (`job_template_id`)
) engine = innodb
  default charset = utf8mb4 comment ='任务步骤模板表';

-- ----------------------------
-- 任务实例
-- ----------------------------
CREATE TABLE `job_instance` (
    `id`           bigint auto_increment comment '实例ID' primary key,
    `job_template_id` bigint               not null comment '任务模板ID',
    `biz_type`     varchar(32)             not null comment '业务类型（冗余，方便查询）',
    `biz_key`      varchar(128)            not null comment '业务唯一键（如 xhs账号ID / up主mid / bvid）',
    `biz_sub_key`  varchar(128)            null comment '业务子键（如分P编号、重跑批次）',
    `params_json`  json                    null comment '任务级入参（JSON）',
    `status`       varchar(16)             not null comment '状态（PENDING / RUNNING / WAITING_HUMAN / COMPLETED / FAILED / DEAD / CANCELLED）',
    `current_step_code` varchar(64)        null comment '当前执行到的步骤 code',
    `context_json` json                    null comment '全局上下文（各步骤输出汇总，便于后续步骤读取）',
    `start_time`   datetime                null comment '开始时间',
    `end_time`     datetime                null comment '结束时间',
    `error_msg`    varchar(1024)           null comment '最终失败原因',
    `trigger_source` varchar(32) default 'manual' null comment '触发来源（manual 手动 / schedule 定时 / webhook 回调）',
    `remark`       varchar(255)            null comment '备注',
    `creator_id`   bigint                  null comment '创建者ID',
    `create_time`  datetime                null comment '创建时间',
    `updater_id`   bigint                  null comment '更新者ID',
    `update_time`  datetime                null comment '更新时间',
    `deleted`      tinyint      default 0  not null comment '删除标志（0存在 1删除）',
    UNIQUE KEY `uk_template_biz_key_sub` (`job_template_id`, `biz_key`, `biz_sub_key`),
    KEY `idx_status_create_time` (`status`, `create_time`),
    KEY `idx_biz_key` (`biz_key`)
) engine = innodb
  default charset = utf8mb4 comment ='任务实例表';

-- ----------------------------
-- 任务步骤实例
-- ----------------------------
CREATE TABLE `job_step_instance` (
    `id`           bigint auto_increment comment '步骤实例ID' primary key,
    `job_instance_id` bigint               not null comment '任务实例ID',
    `job_step_template_id` bigint          not null comment '步骤模板ID',
    `step_code`    varchar(64)             not null comment '步骤编码',
    `step_name`    varchar(128)            not null comment '步骤名称',
    `step_type`    varchar(16)             not null comment '步骤类型（AUTO / HUMAN / GATEWAY）',
    `status`       varchar(16)             not null comment '状态（PENDING / RUNNING / WAITING_HUMAN / COMPLETED / FAILED / DEAD / TIMEOUT）',
    `assigned_node_id` varchar(64)         null comment '分配到的节点ID',
    `node_type`    varchar(64)             null comment '实际执行节点类型',
    `input_json`   json                    null comment '输入参数（继承模板+上一步输出）',
    `output_json`  json                    null comment '输出结果（小数据直接存）',
    `input_artifact_ids` json              null comment '输入产物ID数组',
    `output_artifact_ids` json             null comment '输出产物ID数组',
    `started_at`   datetime                null comment '开始执行时间',
    `ended_at`     datetime                null comment '结束时间',
    `timeout_at`   datetime                null comment '超时截止时间',
    `retry_count`  int          default 0  not null comment '已重试次数',
    `error_msg`    varchar(1024)           null comment '失败原因',
    `previous_step_id` bigint              null comment '前序步骤实例ID（线性链路用）',
    `remark`       varchar(255)            null comment '备注',
    `creator_id`   bigint                  null comment '创建者ID',
    `create_time`  datetime                null comment '创建时间',
    `updater_id`   bigint                  null comment '更新者ID',
    `update_time`  datetime                null comment '更新时间',
    `deleted`      tinyint      default 0  not null comment '删除标志（0存在 1删除）',
    KEY `idx_job_instance_id` (`job_instance_id`),
    KEY `idx_status_timeout` (`status`, `timeout_at`),
    KEY `idx_assigned_node` (`assigned_node_id`, `status`)
) engine = innodb
  default charset = utf8mb4 comment ='任务步骤实例表';

-- ----------------------------
-- 步骤执行日志
-- ----------------------------
CREATE TABLE `job_step_log` (
    `id`           bigint auto_increment comment '日志ID' primary key,
    `job_step_instance_id` bigint          not null comment '步骤实例ID',
    `log_level`    varchar(16)  default 'INFO' not null comment '日志级别（DEBUG / INFO / WARN / ERROR）',
    `stage`        varchar(64)             null comment '阶段标识',
    `content`      text                    null comment '日志内容',
    `ext_json`     json                    null comment '扩展信息（JSON）',
    `create_time`  datetime                null comment '创建时间',
    KEY `idx_step_instance_id` (`job_step_instance_id`),
    KEY `idx_create_time` (`create_time`)
) engine = innodb
  default charset = utf8mb4 comment ='任务步骤执行日志表';

-- ----------------------------
-- 任务产物
-- ----------------------------
CREATE TABLE `job_artifact` (
    `id`           bigint auto_increment comment '产物ID' primary key,
    `job_instance_id` bigint               not null comment '任务实例ID',
    `job_step_instance_id` bigint          null comment '产生该产物的步骤实例ID',
    `artifact_type` varchar(32)            not null comment '产物类型（raw_json / audio / transcript / summary / log / image）',
    `storage_type` varchar(16)             not null comment '存储类型（db / local / oss / minio）',
    `content`      longtext                null comment '文本内容（storage_type=db 时使用）',
    `file_path`    varchar(512)            null comment '文件路径或URL',
    `file_size`    bigint                  null comment '文件大小（字节）',
    `file_hash`    varchar(128)            null comment '文件哈希（sha256）',
    `status`       smallint     default 1  not null comment '状态（1有效 0已清理）',
    `expired_at`   datetime                null comment '过期清理时间',
    `remark`       varchar(255)            null comment '备注',
    `creator_id`   bigint                  null comment '创建者ID',
    `create_time`  datetime                null comment '创建时间',
    `updater_id`   bigint                  null comment '更新者ID',
    `update_time`  datetime                null comment '更新时间',
    `deleted`      tinyint      default 0  not null comment '删除标志（0存在 1删除）',
    KEY `idx_job_instance_id` (`job_instance_id`),
    KEY `idx_step_instance_id` (`job_step_instance_id`),
    KEY `idx_artifact_type` (`artifact_type`)
) engine = innodb
  default charset = utf8mb4 comment ='任务产物表';

-- ----------------------------
-- 任务事件（人工触发/回调通知）
-- 高频场景可降级到 Redis，但 DB 表保留用于持久化与审计
-- ----------------------------
CREATE TABLE `job_event` (
    `id`           bigint auto_increment comment '事件ID' primary key,
    `job_instance_id` bigint               not null comment '任务实例ID',
    `job_step_instance_id` bigint          null comment '目标步骤实例ID',
    `event_type`   varchar(64)             not null comment '事件类型（qr_scanned / webhook / manual_confirm / retry 等）',
    `payload_json` json                    null comment '事件负载（JSON）',
    `status`       varchar(16)  default 'PENDING' not null comment '状态（PENDING / PROCESSED / IGNORED）',
    `processed_at` datetime                null comment '处理时间',
    `create_time`  datetime                null comment '创建时间',
    KEY `idx_job_instance_id` (`job_instance_id`),
    KEY `idx_status_create_time` (`status`, `create_time`)
) engine = innodb
  default charset = utf8mb4 comment ='任务事件表';

-- ----------------------------
-- 节点注册表
-- ----------------------------
CREATE TABLE `job_node` (
    `id`           bigint auto_increment comment '节点ID' primary key,
    `node_id`      varchar(64)             not null comment '节点唯一标识（机器/容器hostname或UUID）',
    `node_name`    varchar(128)            null comment '节点可读名称（如 mac-m1-dev / gpu-worker-01 / xhs-node-home）',
    `node_type`    varchar(32)             not null comment '节点大类（worker / residential / local_dev）',
    `capabilities` json                    not null comment '能力标签数组（JSON，如 ["xhs:login","xhs:publish"] / ["asr:cpu","asr:mps"] / ["bili:download"]）',
    `labels`       json                    null comment '自定义标签（JSON，如 {zone: home, gpu: none, owner: ywb}）',
    `protocol`     varchar(16)  default 'poll' not null comment '通信协议（poll 拉取 / push 推送 / hybrid 混合）',
    `endpoint`     varchar(256)            null comment '推送地址（protocol=push 时必填，如 http://192.168.1.10:8080）',
    `token`        varchar(255)            not null comment '节点认证令牌（注册时生成，调度和回调均校验）',
    `max_concurrent` int       default 1   not null comment '最大并发任务数',
    `current_load` int         default 0   not null comment '当前正在执行的任务数',
    `version`      varchar(32)             null comment 'Worker 版本号',
    `ip_address`   varchar(64)             null comment '节点出口IP（风控排查用）',
    `status`       varchar(16)  default 'ONLINE' not null comment '状态（ONLINE / OFFLINE / DISABLED / BUSY）',
    `last_heartbeat` datetime              null comment '最后心跳时间',
    `heartbeat_interval_seconds` int default 30 not null comment '预期心跳间隔（秒）',
    `offline_threshold_seconds` int default 120 not null comment '离线判定阈值（秒）',
    `remark`       varchar(255)            null comment '备注',
    `creator_id`   bigint                  null comment '创建者ID',
    `create_time`  datetime                null comment '创建时间',
    `updater_id`   bigint                  null comment '更新者ID',
    `update_time`  datetime                null comment '更新时间',
    `deleted`      tinyint      default 0  not null comment '删除标志（0存在 1删除）',
    UNIQUE KEY `uk_node_id` (`node_id`),
    KEY `idx_status` (`status`),
    KEY `idx_last_heartbeat` (`last_heartbeat`)
) engine = innodb
  default charset = utf8mb4 comment ='任务执行节点注册表';

-- ----------------------------
-- 节点心跳历史（可选，用于排查节点抖动/负载曲线）
-- 高频心跳数据量大，可单独存时序库；此处提供基础 MySQL 归档表
-- ----------------------------
CREATE TABLE `job_node_heartbeat` (
    `id`           bigint auto_increment comment '心跳ID' primary key,
    `node_id`      varchar(64)             not null comment '节点ID',
    `status`       varchar(16)             not null comment '上报状态',
    `current_load` int         default 0   not null comment '当前并发数',
    `payload_json` json                    null comment '扩展负载信息（JSON：内存/CPU/磁盘/队列长度）',
    `create_time`  datetime                null comment '上报时间',
    KEY `idx_node_id_time` (`node_id`, `create_time`)
) engine = innodb
  default charset = utf8mb4 comment ='节点心跳历史表';
