-- 动态同步记录去重：按 platform_post_id 唯一
-- 导入：mysql --default-character-set=utf8mb4 -h <host> -u root -p <db> < sql/social_sync_post_unique_20260805.sql

SET NAMES utf8mb4;

ALTER TABLE `social_sync_post`
    ADD UNIQUE KEY `uk_platform_post_id` (`platform_post_id`);
