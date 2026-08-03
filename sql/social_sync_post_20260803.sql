-- 动态同步模块（B站UP主动态记录后台）
-- 顶级菜单：动态同步 + 动态列表菜单项 + 按钮权限
-- 导入：mysql --default-character-set=utf8mb4 -h <host> -u root -p <db> < sql/social_sync_post_20260803.sql

SET NAMES utf8mb4;

-- ----------------------------
-- audio_transcript 字段（本机 ASR worker 写入，若已存在则忽略报错）
-- ----------------------------
ALTER TABLE `social_sync_post`
    ADD COLUMN `audio_transcript` MEDIUMTEXT NULL COMMENT '音频转写文本（ASR）' AFTER `audio_url`;

-- ----------------------------
-- 菜单：动态同步（顶级目录） + 动态列表（菜单项）
-- menu_type: 2目录 1菜单 0按钮；is_button: 0否 1是
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, menu_type, router_name, parent_id, path, is_button, permission, meta_info, status, remark, creator_id, create_time, updater_id, update_time, deleted)
VALUES (630, '动态同步', 2, '', 0, '/sync', 0, '', '{"title":"动态同步","icon":"ep:video-camera","showParent":true,"rank":5}', 1, '动态同步目录（B站UP主动态）', 0, now(), 0, now(), 0);

INSERT INTO sys_menu (menu_id, menu_name, menu_type, router_name, parent_id, path, is_button, permission, meta_info, status, remark, creator_id, create_time, updater_id, update_time, deleted)
VALUES (631, '动态列表', 1, 'SocialSyncPost', 630, '/social/post/index', 0, 'social:post:list', '{"title":"动态列表","icon":"ep:document","showParent":true}', 1, '动态同步记录列表', 0, now(), 0, now(), 0);

-- 动态列表按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_type, router_name, parent_id, path, is_button, permission, meta_info, status, remark, creator_id, create_time, updater_id, update_time, deleted)
VALUES (632, '动态查询', 0, ' ', 631, '', 1, 'social:post:query', '{"title":"动态查询"}', 1, '', 0, now(), null, null, 0);
INSERT INTO sys_menu (menu_id, menu_name, menu_type, router_name, parent_id, path, is_button, permission, meta_info, status, remark, creator_id, create_time, updater_id, update_time, deleted)
VALUES (633, '重触发转写/总结', 0, ' ', 631, '', 1, 'social:post:retrigger', '{"title":"重触发转写/总结"}', 1, '', 0, now(), null, null, 0);

-- 说明：超级管理员（loginUser.isAdmin()）自动可见全部菜单，无需 sys_role_menu 授权。
-- 其它角色请在【系统管理 → 角色管理】中为角色勾选「动态同步」菜单。
