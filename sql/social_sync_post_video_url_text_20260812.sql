SET NAMES utf8mb4;

-- 2026-08-12: alist 直链 URL 过长（含 token），varchar(1024) 不够
ALTER TABLE `social_sync_post`
    MODIFY COLUMN `video_url` TEXT NULL COMMENT '视频地址';
