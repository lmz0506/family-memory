USE `family_memory_new`;

CREATE TABLE IF NOT EXISTS `fm_family_milestone` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `family_id` BIGINT NOT NULL,
  `creator_id` BIGINT NOT NULL,
  `title` VARCHAR(120) NOT NULL,
  `description` VARCHAR(500) DEFAULT NULL,
  `event_date` DATE NOT NULL,
  `recurring_yearly` TINYINT NOT NULL DEFAULT 0,
  `visibility_scope` VARCHAR(16) NOT NULL DEFAULT 'PRIVATE' COMMENT 'PRIVATE/FAMILY/SPECIFIC',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_milestone_family_date` (`family_id`,`event_date`),
  KEY `idx_milestone_creator` (`creator_id`),
  CONSTRAINT `fk_milestone_family` FOREIGN KEY (`family_id`) REFERENCES `fm_family` (`id`),
  CONSTRAINT `fk_milestone_creator` FOREIGN KEY (`creator_id`) REFERENCES `fm_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家庭大事记提醒';

CREATE TABLE IF NOT EXISTS `fm_family_milestone_viewer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `milestone_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_milestone_viewer` (`milestone_id`,`user_id`),
  KEY `idx_milestone_viewer_user` (`user_id`),
  CONSTRAINT `fk_milestone_viewer_item` FOREIGN KEY (`milestone_id`) REFERENCES `fm_family_milestone` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_milestone_viewer_user` FOREIGN KEY (`user_id`) REFERENCES `fm_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家庭大事记提醒参与人';
