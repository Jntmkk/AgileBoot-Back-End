-- B站账号体系接入：登录凭据表 + 账号列通用化
-- 导入：mysql --default-character-set=utf8mb4 -h <host> -u root -p <db> < sql/social_bilibili_20260722.sql

SET NAMES utf8mb4;

-- ----------------------------
-- 平台用户ID列通用化（xhs_user_id -> platform_user_id，同时承载小红书号/B站mid）
-- ----------------------------
ALTER TABLE `social_account`
    CHANGE COLUMN `xhs_user_id` `platform_user_id` varchar(64) NULL COMMENT '平台侧用户ID（登录后回写）';

-- ----------------------------
-- 社交平台登录凭据表（cookie会话DB持久化，与账号一对一）
-- ----------------------------
CREATE TABLE `social_credential` (
    `id`              bigint auto_increment comment '凭据ID' primary key,
    `account_id`      bigint                  not null comment 'social_account.id',
    `platform`        varchar(16)             not null comment '平台（bili）',
    `cookie`          text                    null comment '完整Cookie串（SESSDATA/bili_jct/DedeUserID/buvid3...）',
    `refresh_token`   varchar(128)            null comment 'B站刷新令牌（cookie刷新流程预留）',
    `expires_at`      datetime                null comment '凭据过期时间（取自SESSDATA的Expires，未知可空）',
    `last_login_time` datetime                null comment '最近一次扫码登录成功时间',
    `status`          smallint     default 1  not null comment '凭据状态（1有效 0失效）',
    `remark`          varchar(255)            null comment '备注',
    `creator_id`      bigint                  null comment '创建者ID',
    `create_time`     datetime                null comment '创建时间',
    `updater_id`      bigint                  null comment '更新者ID',
    `update_time`     datetime                null comment '更新时间',
    `deleted`         tinyint      default 0  not null comment '删除标志（0存在 1删除）',
    UNIQUE KEY `uk_account_id` (`account_id`)
) engine = innodb
  default charset = utf8mb4 comment ='社交平台登录凭据表';
