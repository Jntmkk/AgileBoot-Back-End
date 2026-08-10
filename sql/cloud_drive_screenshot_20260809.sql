-- 阿里云盘视频总结：截图状态字段
-- 导入：mysql --default-character-set=utf8mb4 -h <host> -u root -p <db> < sql/cloud_drive_screenshot_20260809.sql

SET NAMES utf8mb4;

ALTER TABLE `social_sync_post`
    ADD COLUMN `screenshot_status` TINYINT NOT NULL DEFAULT 0 COMMENT '截图状态：0无需截图 1待截图 2已截图' AFTER `audio_summarized_at`;

-- 菜单：云盘文件浏览
INSERT INTO sys_menu (menu_id, menu_name, menu_type, router_name, parent_id, path, is_button, permission, meta_info, status, remark, creator_id, create_time, updater_id, update_time, deleted)
VALUES (640, '云盘文件', 1, 'CloudDrive', 630, '/social/cloud-drive/index', 0, 'social:cloud-drive:query', '{"title":"云盘文件","icon":"ep:folder-opened","showParent":true}', 1, '阿里云盘文件浏览与同步', 0, now(), 0, now(), 0);

INSERT INTO sys_menu (menu_id, menu_name, menu_type, router_name, parent_id, path, is_button, permission, meta_info, status, remark, creator_id, create_time, updater_id, update_time, deleted)
VALUES (641, '云盘同步', 0, ' ', 640, '', 1, 'social:cloud-drive:sync', '{"title":"云盘同步"}', 1, '触发云盘视频同步', 0, now(), null, null, 0);
