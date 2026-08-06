-- 总结提示词配置表 + 菜单 + 权限
CREATE TABLE `social_summary_prompt` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `up_id`         VARCHAR(64)  NOT NULL COMMENT 'UP主平台ID（* 表示兜底默认）',
    `keyword`       VARCHAR(255) NOT NULL COMMENT '标题匹配关键词',
    `system_prompt` TEXT         NOT NULL COMMENT '系统提示词',
    `sort_order`    INT          NOT NULL DEFAULT 0 COMMENT '排序（越小越优先）',
    `status`        TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '状态（1启用 0停用）',
    `creator_id`    BIGINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater_id`    BIGINT       NULL,
    `update_time`   DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_up_id` (`up_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI总结提示词配置';

-- 菜单（父目录 630=动态同步）
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `menu_type`, `router_name`, `parent_id`, `path`,
    `is_button`, `permission`, `meta_info`, `status`, `remark`,
    `creator_id`, `create_time`, `updater_id`, `update_time`, `deleted`)
VALUES
(640, '总结提示词', 1, 'SocialSummaryPrompt', 630, '/social/prompt/index', 0,
    'social:prompt:list', '{"title":"总结提示词","icon":"ep:edit-pen","showParent":true}',
    1, '', 0, NOW(), 0, NOW(), 0),
(641, '总结提示词新增', 0, NULL, 640, NULL, 1,
    'social:prompt:add', '{}', 1, '', 0, NOW(), 0, NOW(), 0),
(642, '总结提示词修改', 0, NULL, 640, NULL, 1,
    'social:prompt:edit', '{}', 1, '', 0, NOW(), 0, NOW(), 0),
(643, '总结提示词删除', 0, NULL, 640, NULL, 1,
    'social:prompt:delete', '{}', 1, '', 0, NOW(), 0, NOW(), 0);
