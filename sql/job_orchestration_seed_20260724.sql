-- 通用任务编排层 — seed data（最小闭环：fetch_space → download_audio）
-- 导入前确保已先执行 job_orchestration_20260723.sql 建表
-- 导入：mysql --default-character-set=utf8mb4 -h <host> -u root -p <db> < sql/job_orchestration_seed_20260724.sql

SET NAMES utf8mb4;

-- ----------------------------
-- 任务模板：B站跟踪UP主 → 下载音频
-- ----------------------------
INSERT INTO `job_template` (`template_code`, `template_name`, `biz_type`, `description`, `status`, `version`, `create_time`, `update_time`, `deleted`)
VALUES ('bili_track_asr_summary', 'B站UP主跟踪→下载音频', 'BILI_TRACK', '获取UP主空间投稿→下载音频（后续扩展：转写+总结）', 1, 1, NOW(), NOW(), 0);

SET @template_id = LAST_INSERT_ID();

-- ----------------------------
-- 步骤模板
-- ----------------------------
-- Step 1: fetch_space（Java 后端执行）
INSERT INTO `job_step_template` (`job_template_id`, `step_code`, `step_name`, `step_type`, `order_index`, `executor_capability`, `timeout_seconds`, `retry_times`, `retry_interval_seconds`, `dependency_step_codes`, `description`, `status`, `create_time`, `update_time`, `deleted`)
VALUES (@template_id, 'fetch_space', '获取空间投稿', 'AUTO', 0, 'java', 30, 3, 60, '[]', '调用B站space/wbi/arc/search拉取UP主视频列表', 1, NOW(), NOW(), 0);

-- Step 2: download_audio（下载Worker执行）
INSERT INTO `job_step_template` (`job_template_id`, `step_code`, `step_name`, `step_type`, `order_index`, `executor_capability`, `timeout_seconds`, `retry_times`, `retry_interval_seconds`, `dependency_step_codes`, `description`, `status`, `create_time`, `update_time`, `deleted`)
VALUES (@template_id, 'download_audio', '下载音频', 'AUTO', 1, 'bili:download', 300, 2, 120, '["fetch_space"]', '用yt-dlp下载B站视频音频', 1, NOW(), NOW(), 0);

-- ----------------------------
-- 节点注册
-- ----------------------------
-- Java 后端自身（可执行 java、bili:api 能力）
INSERT INTO `job_node` (`node_id`, `node_name`, `node_type`, `capabilities`, `labels`, `protocol`, `endpoint`, `token`, `max_concurrent`, `current_load`, `version`, `status`, `heartbeat_interval_seconds`, `offline_threshold_seconds`, `create_time`, `update_time`, `deleted`)
VALUES ('java-server', 'Java后端', 'worker', '["java","bili:api"]', '{"zone":"internal"}', 'push', NULL, 'java-server-token-dev-001', 5, 0, '1.0.0', 'ONLINE', 30, 120, NOW(), NOW(), 0);

-- B站下载 Worker（独立部署，用 yt-dlp）
INSERT INTO `job_node` (`node_id`, `node_name`, `node_type`, `capabilities`, `labels`, `protocol`, `endpoint`, `token`, `max_concurrent`, `current_load`, `version`, `status`, `heartbeat_interval_seconds`, `offline_threshold_seconds`, `create_time`, `update_time`, `deleted`)
VALUES ('bili-download-worker-01', 'B站下载Worker-01', 'worker', '["bili:download"]', '{"zone":"worker"}', 'poll', NULL, 'bili-worker-token-dev-001', 2, 0, '1.0.0', 'ONLINE', 30, 120, NOW(), NOW(), 0);
