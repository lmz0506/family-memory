-- 星海统一索引：MySQL 8.0 / utf8mb4。
-- 请先核对目标数据库；本文件只新增索引表，不修改、删除原业务表。
-- 日期规则：MEMORY = fm_memory.memory_date；CAPSULE = DATE(fm_time_capsule.created_at)。
-- 每份业务内容只有一条索引，珍藏开启日期不参与星海归档。
USE `family_memory_new`;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `fm_universe_index` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应表：fm_universe_index.id',
  `family_id` BIGINT NOT NULL COMMENT '家庭ID，来源表：fm_family.id',
  `source_type` VARCHAR(16) NOT NULL COMMENT '来源类型，MEMORY记忆，CAPSULE珍藏',
  `source_id` BIGINT NOT NULL COMMENT '来源ID，MEMORY来源表：fm_memory.id；CAPSULE来源表：fm_time_capsule.id',
  `node_date` DATE NOT NULL COMMENT '归档日期，MEMORY来源表：fm_memory.memory_date；CAPSULE来源表：fm_time_capsule.created_at的日期部分，发布后固定',
  `title` VARCHAR(120) NOT NULL COMMENT '标题副本，来源表：fm_memory.title或fm_time_capsule.title，由对应业务同步',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '索引行创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '索引行最近同步更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志，0有效，1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_universe_source` (`source_type`, `source_id`),
  KEY `idx_universe_family_date` (`family_id`, `deleted`, `node_date`, `source_type`),
  CONSTRAINT `fk_universe_family` FOREIGN KEY (`family_id`) REFERENCES `fm_family` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='星海统一内容索引';

-- 不单独存储年月日三个字段；node_date 已完整表达日期，避免冗余不一致。
-- 不复制正文、媒体、收信人、开启日期或开启状态；这些信息仍由原详情接口提供。
-- source_id 是多态引用，不对两个业务表建立外键；一致性由对应业务同事务维护。
-- IF NOT EXISTS 只防止重复建表，不会自动修正一张已经存在但结构不同的表。
