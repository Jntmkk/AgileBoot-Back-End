-- 社交媒体模块（SocialMedia-Hub 接入）
-- 导入：mysql --default-character-set=utf8mb4 -h <host> -u root -p <db> < sql/social_media_20260721.sql

SET NAMES utf8mb4;

-- ----------------------------
-- 社交媒体账号表
-- 端口号规则：容器端口 = 18060 + id，stcp 隧道名 = social-acc-{id}
-- ----------------------------
DROP TABLE IF EXISTS `social_account`;
CREATE TABLE `social_account` (
    `id`           bigint auto_increment comment '账号ID' primary key,
    `platform`     varchar(16)  default 'xhs' not null comment '平台（xhs小红书 预留douyin）',
    `account_name` varchar(64)              not null comment '账号备注名',
    `xhs_user_id`  varchar(64)              null comment '平台侧用户ID（登录后回写）',
    `node_name`    varchar(64)              null comment '所在住宅节点名（运维参考）',
    `proxy_url`    varchar(255)             null comment '代理地址（IP池预留，住宅IP阶段为空）',
    `status`       smallint     default 1   not null comment '状态（1启用 0停用）',
    `remark`       varchar(255)             null comment '备注',
    `creator_id`   bigint                   null comment '创建者ID',
    `create_time`  datetime                 null comment '创建时间',
    `updater_id`   bigint                   null comment '更新者ID',
    `update_time`  datetime                 null comment '更新时间',
    `deleted`      tinyint      default 0   not null comment '删除标志（0存在 1删除）'
) engine = innodb
  default charset = utf8mb4 comment ='社交媒体账号表';

-- ----------------------------
-- 住宅节点表（节点 agent 心跳自动 upsert）
-- ----------------------------
DROP TABLE IF EXISTS `social_node`;
CREATE TABLE `social_node` (
    `id`             bigint auto_increment comment '节点ID' primary key,
    `node_name`      varchar(64)               not null comment '节点标识',
    `egress_ip`      varchar(64)               null comment '住宅出口IP',
    `ip_type`        varchar(16) default 'residential' not null comment 'IP类型（residential住宅 pool代理池）',
    `last_heartbeat` datetime                  null comment '最后心跳时间',
    `status`         smallint    default 1     not null comment '状态（1启用 0停用）',
    `remark`         varchar(255)              null comment '备注',
    `creator_id`     bigint                    null comment '创建者ID',
    `create_time`    datetime                  null comment '创建时间',
    `updater_id`     bigint                    null comment '更新者ID',
    `update_time`    datetime                  null comment '更新时间',
    `deleted`        tinyint     default 0     not null comment '删除标志（0存在 1删除）',
    UNIQUE KEY `uk_node_name` (`node_name`)
) engine = innodb
  default charset = utf8mb4 comment ='住宅节点表';

-- ----------------------------
-- 菜单：社交媒体目录 + 账号管理 + 节点管理 + 按钮权限
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, menu_type, router_name, parent_id, path, is_button, permission, meta_info, status, remark, creator_id, create_time, updater_id, update_time, deleted)
VALUES (600, '社交媒体', 2, '', 0, '/social', 0, '', '{"title":"社交媒体","icon":"ep:share","showParent":true,"rank":4}', 1, '社交媒体目录', 0, now(), 0, now(), 0);

INSERT INTO sys_menu (menu_id, menu_name, menu_type, router_name, parent_id, path, is_button, permission, meta_info, status, remark, creator_id, create_time, updater_id, update_time, deleted)
VALUES (601, '账号管理', 1, 'SocialAccount', 600, '/social/account/index', 0, 'social:account:list', '{"title":"账号管理","icon":"ep:user","showParent":true}', 1, '社交账号管理菜单', 0, now(), 0, now(), 0);

INSERT INTO sys_menu (menu_id, menu_name, menu_type, router_name, parent_id, path, is_button, permission, meta_info, status, remark, creator_id, create_time, updater_id, update_time, deleted)
VALUES (602, '节点管理', 1, 'SocialNode', 600, '/social/node/index', 0, 'social:node:list', '{"title":"节点管理","icon":"ep:connection","showParent":true}', 1, '住宅节点管理菜单', 0, now(), 0, now(), 0);

-- 账号管理按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_type, router_name, parent_id, path, is_button, permission, meta_info, status, remark, creator_id, create_time, updater_id, update_time, deleted)
VALUES (610, '账号查询', 0, ' ', 601, '', 1, 'social:account:query', '{"title":"账号查询"}', 1, '', 0, now(), null, null, 0);
INSERT INTO sys_menu (menu_id, menu_name, menu_type, router_name, parent_id, path, is_button, permission, meta_info, status, remark, creator_id, create_time, updater_id, update_time, deleted)
VALUES (611, '账号新增', 0, ' ', 601, '', 1, 'social:account:add', '{"title":"账号新增"}', 1, '', 0, now(), null, null, 0);
INSERT INTO sys_menu (menu_id, menu_name, menu_type, router_name, parent_id, path, is_button, permission, meta_info, status, remark, creator_id, create_time, updater_id, update_time, deleted)
VALUES (612, '账号修改', 0, ' ', 601, '', 1, 'social:account:edit', '{"title":"账号修改"}', 1, '', 0, now(), null, null, 0);
INSERT INTO sys_menu (menu_id, menu_name, menu_type, router_name, parent_id, path, is_button, permission, meta_info, status, remark, creator_id, create_time, updater_id, update_time, deleted)
VALUES (613, '账号删除', 0, ' ', 601, '', 1, 'social:account:remove', '{"title":"账号删除"}', 1, '', 0, now(), null, null, 0);
INSERT INTO sys_menu (menu_id, menu_name, menu_type, router_name, parent_id, path, is_button, permission, meta_info, status, remark, creator_id, create_time, updater_id, update_time, deleted)
VALUES (614, '扫码登录', 0, ' ', 601, '', 1, 'social:account:login', '{"title":"扫码登录"}', 1, '', 0, now(), null, null, 0);

-- 节点管理按钮
INSERT INTO sys_menu (menu_id, menu_name, menu_type, router_name, parent_id, path, is_button, permission, meta_info, status, remark, creator_id, create_time, updater_id, update_time, deleted)
VALUES (620, '节点查询', 0, ' ', 602, '', 1, 'social:node:query', '{"title":"节点查询"}', 1, '', 0, now(), null, null, 0);
INSERT INTO sys_menu (menu_id, menu_name, menu_type, router_name, parent_id, path, is_button, permission, meta_info, status, remark, creator_id, create_time, updater_id, update_time, deleted)
VALUES (621, '节点修改', 0, ' ', 602, '', 1, 'social:node:edit', '{"title":"节点修改"}', 1, '', 0, now(), null, null, 0);
