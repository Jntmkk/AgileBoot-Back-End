-- 关注UP主模块 + 动态同步触发按钮
-- 导入：mysql --default-character-set=utf8mb4 -h <host> -u root -p <db> < sql/social_follow_up_20260805.sql

SET NAMES utf8mb4;

-- ----------------------------
-- 关注UP主表（动态同步抓取目标，按 platform 可扩展）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `social_follow_up` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `platform` VARCHAR(32) NOT NULL DEFAULT 'bili' COMMENT '平台（bili哔哩 xhs小红书 douyin抖音）',
    `up_id` VARCHAR(64) NOT NULL COMMENT 'UP主平台ID（B站mid/小红书号/抖音号）',
    `up_name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'UP主昵称（冗余展示）',
    `up_avatar` VARCHAR(512) NULL DEFAULT '' COMMENT '头像链接',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态（1启用 0停用）',
    `sync_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否参与自动同步（1是 0否）',
    `last_sync_at` DATETIME NULL DEFAULT NULL COMMENT '最近同步时间',
    `remark` VARCHAR(500) NULL DEFAULT '' COMMENT '备注',
    `creator_id` BIGINT NOT NULL DEFAULT 0 COMMENT '创建者ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater_id` BIGINT NULL DEFAULT NULL COMMENT '更新者ID',
    `update_time` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志（0存在 1删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_platform_up_id` (`platform`, `up_id`),
    KEY `idx_status_sync_enabled` (`status`, `sync_enabled`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注UP主列表（动态同步抓取目标）';

-- 初始化：把之前硬编码的 B站 UP 322005137 录入
INSERT IGNORE INTO `social_follow_up` (`platform`, `up_id`, `up_name`, `status`, `sync_enabled`, `remark`, `creator_id`, `create_time`, `deleted`)
VALUES ('bili', '322005137', '硬编码迁移UP', 1, 1, '从旧工作流迁移的默认关注UP', 0, NOW(), 0);

-- ----------------------------
-- 菜单：关注UP管理（动态同步目录下） + 动态同步触发按钮（动态列表下）
-- menu_type: 2目录 1菜单 0按钮；is_button: 0否 1是
-- 父菜单：630 动态同步；631 动态列表
-- ----------------------------
INSERT IGNORE INTO `sys_menu` (`menu_id`, `menu_name`, `menu_type`, `router_name`, `parent_id`, `path`, `is_button`, `permission`, `meta_info`, `status`, `remark`, `creator_id`, `create_time`, `updater_id`, `update_time`, `deleted`)
VALUES (634, '关注UP管理', 1, 'SocialFollowUp', 630, '/social/follow/index', 0, 'social:follow:list', '{"title":"关注UP管理","icon":"ep:user","showParent":true}', 1, '关注UP主列表', 0, now(), 0, now(), 0);

INSERT IGNORE INTO `sys_menu` (`menu_id`, `menu_name`, `menu_type`, `router_name`, `parent_id`, `path`, `is_button`, `permission`, `meta_info`, `status`, `remark`, `creator_id`, `create_time`, `updater_id`, `update_time`, `deleted`)
VALUES (635, '关注UP查询', 0, ' ', 634, '', 1, 'social:follow:query', '{"title":"关注UP查询"}', 1, '', 0, now(), null, null, 0);

INSERT IGNORE INTO `sys_menu` (`menu_id`, `menu_name`, `menu_type`, `router_name`, `parent_id`, `path`, `is_button`, `permission`, `meta_info`, `status`, `remark`, `creator_id`, `create_time`, `updater_id`, `update_time`, `deleted`)
VALUES (636, '关注UP新增', 0, ' ', 634, '', 1, 'social:follow:add', '{"title":"关注UP新增"}', 1, '', 0, now(), null, null, 0);

INSERT IGNORE INTO `sys_menu` (`menu_id`, `menu_name`, `menu_type`, `router_name`, `parent_id`, `path`, `is_button`, `permission`, `meta_info`, `status`, `remark`, `creator_id`, `create_time`, `updater_id`, `update_time`, `deleted`)
VALUES (637, '关注UP修改', 0, ' ', 634, '', 1, 'social:follow:edit', '{"title":"关注UP修改"}', 1, '', 0, now(), null, null, 0);

INSERT IGNORE INTO `sys_menu` (`menu_id`, `menu_name`, `menu_type`, `router_name`, `parent_id`, `path`, `is_button`, `permission`, `meta_info`, `status`, `remark`, `creator_id`, `create_time`, `updater_id`, `update_time`, `deleted`)
VALUES (638, '关注UP删除', 0, ' ', 634, '', 1, 'social:follow:remove', '{"title":"关注UP删除"}', 1, '', 0, now(), null, null, 0);

INSERT IGNORE INTO `sys_menu` (`menu_id`, `menu_name`, `menu_type`, `router_name`, `parent_id`, `path`, `is_button`, `permission`, `meta_info`, `status`, `remark`, `creator_id`, `create_time`, `updater_id`, `update_time`, `deleted`)
VALUES (639, '动态同步触发', 0, ' ', 631, '', 1, 'social:follow:sync', '{"title":"动态同步触发"}', 1, '', 0, now(), null, null, 0);
