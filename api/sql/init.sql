-- 数据库
CREATE DATABASE my_life DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 用户表
CREATE TABLE IF NOT EXISTS `ml_user`
(
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `phone`             VARCHAR(20)  NOT NULL COMMENT '手机号（唯一）',
    `password`          VARCHAR(60)  NOT NULL COMMENT 'BCrypt加密后的密码',
    `nick_name`         VARCHAR(32)  DEFAULT NULL COMMENT '昵称',
    `avatar`            VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    `status`            VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态：ACTIVE-正常, DISABLED-禁用',
    `login_fail_count`  INT          NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    `last_login_at`     DATETIME     DEFAULT NULL COMMENT '最后登录时间',
    `is_deleted`        CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '是否删除：Y-是，N-否',
    `gmt_created`       DATETIME     NOT NULL COMMENT '创建时间',
    `creator`           VARCHAR(64)  NOT NULL DEFAULT 'system' COMMENT '创建人',
    `gmt_modified`      DATETIME     NOT NULL COMMENT '更新时间',
    `modifier`          VARCHAR(64)  NOT NULL DEFAULT 'system' COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uniq_phone` (`phone`),
    INDEX `idx_status` (`status`),
    INDEX `idx_gmt_created` (`gmt_created`),
    INDEX `idx_gmt_modified` (`gmt_modified`)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COMMENT = '用户表';

-- Agent表
CREATE TABLE IF NOT EXISTS `ml_agent` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `uuid` VARCHAR(36) NOT NULL COMMENT '对外唯一标识，替代自增ID',
    `user_id` BIGINT NOT NULL,
    `name` VARCHAR(64) NOT NULL,
    `description` VARCHAR(256) DEFAULT NULL,
    `icon_index` INT DEFAULT 0,
    `color` VARCHAR(32) DEFAULT '#6366f1',
    `system_prompt` TEXT DEFAULT NULL,
    `knowledge_base_id` BIGINT DEFAULT NULL COMMENT '关联知识库表ID',
    `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    `is_deleted` CHAR(1) NOT NULL DEFAULT 'N',
    `gmt_created` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `creator` VARCHAR(64) DEFAULT '',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `modifier` VARCHAR(64) DEFAULT '',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uniq_uuid` (`uuid`),
    INDEX `idx_user_id` (`user_id`),
    UNIQUE INDEX `uniq_user_name` (`user_id`, `name`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 聊天消息表
CREATE TABLE IF NOT EXISTS `ml_chat_message` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT       DEFAULT NULL COMMENT '用户ID，游客为NULL',
    `agent_uuid`    VARCHAR(36)  NOT NULL COMMENT '智能体UUID',
    `role`          VARCHAR(16)  NOT NULL COMMENT '角色：USER,ASSISTANT,SYSTEM,TOOL_RESULT',
    `content`       TEXT         NOT NULL COMMENT '消息内容',
    `tool_name`     VARCHAR(64)  DEFAULT NULL COMMENT '工具名称（TOOL_RESULT时使用）',
    `scene`         VARCHAR(16)  NOT NULL DEFAULT 'PUBLISHED' COMMENT '场景：EDIT-编辑预览, PUBLISHED-发布后对话',
    `is_deleted`    CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '是否删除：Y-是，N-否',
    `gmt_created`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `creator`       VARCHAR(64)  NOT NULL DEFAULT 'system' COMMENT '创建人',
    `gmt_modified`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `modifier`      VARCHAR(64)  NOT NULL DEFAULT 'system' COMMENT '更新人',
    PRIMARY KEY (`id`),
    INDEX `idx_user_agent_created` (`user_id`, `agent_uuid`, `gmt_created`),
    INDEX `idx_user_agent_scene` (`user_id`, `agent_uuid`, `scene`, `gmt_created`),
    INDEX `idx_agent_uuid` (`agent_uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

-- 上下文记忆表
CREATE TABLE IF NOT EXISTS `ml_context_memory` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`         BIGINT       NOT NULL COMMENT '用户ID',
    `agent_uuid`      VARCHAR(36)  NOT NULL COMMENT '智能体UUID',
    `content`         TEXT         NOT NULL COMMENT 'LLM生成的上下文记忆',
    `message_count`   INT          NOT NULL DEFAULT 0 COMMENT '被压缩的消息数',
    `is_deleted`      CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '是否删除：Y-是，N-否',
    `gmt_created`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `creator`         VARCHAR(64)  NOT NULL DEFAULT 'system' COMMENT '创建人',
    `gmt_modified`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `modifier`        VARCHAR(64)  NOT NULL DEFAULT 'system' COMMENT '更新人',
    PRIMARY KEY (`id`),
    INDEX `idx_user_agent` (`user_id`, `agent_uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='上下文记忆表';

-- 知识库表
CREATE TABLE IF NOT EXISTS `ml_knowledge_base` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `uuid`          VARCHAR(36)  NOT NULL COMMENT '对外唯一标识',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `name`          VARCHAR(64)  NOT NULL COMMENT '知识库名称',
    `source`        VARCHAR(16)  NOT NULL DEFAULT 'BAILIAN' COMMENT '来源：BAILIAN-阿里百炼',
    `external_id`   VARCHAR(64)  NOT NULL COMMENT '外部知识库ID（如百炼知识库ID）',
    `is_deleted`    CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '是否删除：Y-是，N-否',
    `gmt_created`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `creator`       VARCHAR(64)  NOT NULL DEFAULT 'system' COMMENT '创建人',
    `gmt_modified`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `modifier`      VARCHAR(64)  NOT NULL DEFAULT 'system' COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uniq_uuid` (`uuid`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';
