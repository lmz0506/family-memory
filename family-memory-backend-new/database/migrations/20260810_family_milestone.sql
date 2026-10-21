USE `family_memory_new`;

CREATE TABLE IF NOT EXISTS `fm_family_milestone` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应表：fm_family_milestone.id',
  `family_id` BIGINT NOT NULL COMMENT '家庭ID，来源表：fm_family.id',
  `creator_id` BIGINT NOT NULL COMMENT '创建者ID，来源表：fm_user.id',
  `title` VARCHAR(120) NOT NULL COMMENT '标题，最多120字',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '提醒说明',
  `event_date` DATE NOT NULL COMMENT '提醒对应的原始日期',
  `recurring_yearly` TINYINT NOT NULL DEFAULT 0 COMMENT '每年重复标志，0一次性提醒，1每年重复',
  `visibility_scope` VARCHAR(16) NOT NULL DEFAULT 'PRIVATE' COMMENT '可见范围，PRIVATE仅创建者，FAMILY全体家人，SPECIFIC指定成员',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志，0有效，1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_milestone_family_date` (`family_id`,`event_date`),
  KEY `idx_milestone_creator` (`creator_id`),
  CONSTRAINT `fk_milestone_family` FOREIGN KEY (`family_id`) REFERENCES `fm_family` (`id`),
  CONSTRAINT `fk_milestone_creator` FOREIGN KEY (`creator_id`) REFERENCES `fm_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家庭大事记提醒';

CREATE TABLE IF NOT EXISTS `fm_family_milestone_viewer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应表：fm_family_milestone_viewer.id',
  `milestone_id` BIGINT NOT NULL COMMENT '大事记ID，来源表：fm_family_milestone.id',
  `user_id` BIGINT NOT NULL COMMENT '用户ID，来源表：fm_user.id',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_milestone_viewer` (`milestone_id`,`user_id`),
  KEY `idx_milestone_viewer_user` (`user_id`),
  CONSTRAINT `fk_milestone_viewer_item` FOREIGN KEY (`milestone_id`) REFERENCES `fm_family_milestone` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_milestone_viewer_user` FOREIGN KEY (`user_id`) REFERENCES `fm_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家庭大事记指定可见成员';
