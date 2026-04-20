CREATE TABLE `ml_user`
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
