-- 拾光家庭记忆应用数据库设计
-- MySQL 8.0 / utf8mb4 / 时区 Asia/Shanghai

CREATE DATABASE IF NOT EXISTS `family_memory_new`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE `family_memory_new`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `fm_file_upload`;
DROP TABLE IF EXISTS `fm_family_milestone_viewer`;
DROP TABLE IF EXISTS `fm_family_milestone`;
DROP TABLE IF EXISTS `fm_time_capsule`;
DROP TABLE IF EXISTS `fm_memory_comment`;
DROP TABLE IF EXISTS `fm_memory_like`;
DROP TABLE IF EXISTS `fm_memory_tag`;
DROP TABLE IF EXISTS `fm_memory_media`;
DROP TABLE IF EXISTS `fm_memory`;
DROP TABLE IF EXISTS `fm_invite_code`;
DROP TABLE IF EXISTS `fm_family_member`;
DROP TABLE IF EXISTS `fm_family`;
DROP TABLE IF EXISTS `fm_user`;

CREATE TABLE `fm_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户主键',
  `username` VARCHAR(30) NOT NULL COMMENT '登录用户名',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码摘要',
  `nickname` VARCHAR(30) NOT NULL COMMENT '显示昵称',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像地址',
  `phone` VARCHAR(20) DEFAULT NULL,
  `email` VARCHAR(100) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 正常，0 停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_phone` (`phone`)
) ENGINE=InnoDB COMMENT='用户';

CREATE TABLE `fm_family` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(60) NOT NULL COMMENT '家庭名称',
  `description` VARCHAR(300) DEFAULT NULL COMMENT '家庭介绍',
  `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '家庭封面',
  `creator_id` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_family_creator` (`creator_id`),
  CONSTRAINT `fk_family_creator` FOREIGN KEY (`creator_id`) REFERENCES `fm_user` (`id`)
) ENGINE=InnoDB COMMENT='家庭空间';

CREATE TABLE `fm_family_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `family_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `role` VARCHAR(30) NOT NULL DEFAULT '家庭成员' COMMENT '家庭内角色',
  `display_name` VARCHAR(30) NOT NULL COMMENT '家庭内称呼',
  `color` VARCHAR(20) NOT NULL DEFAULT 'peach' COMMENT '成员主题色标识',
  `is_admin` TINYINT NOT NULL DEFAULT 0,
  `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_family_member` (`family_id`,`user_id`),
  KEY `idx_member_user` (`user_id`),
  CONSTRAINT `fk_member_family` FOREIGN KEY (`family_id`) REFERENCES `fm_family` (`id`),
  CONSTRAINT `fk_member_user` FOREIGN KEY (`user_id`) REFERENCES `fm_user` (`id`)
) ENGINE=InnoDB COMMENT='家庭成员关系';

CREATE TABLE `fm_invite_code` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `family_id` BIGINT NOT NULL,
  `creator_id` BIGINT NOT NULL,
  `code` CHAR(8) NOT NULL,
  `expire_at` DATETIME NOT NULL,
  `max_uses` INT NOT NULL DEFAULT 1,
  `used_count` INT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invite_code` (`code`),
  KEY `idx_invite_family` (`family_id`),
  KEY `idx_invite_expire` (`expire_at`),
  CONSTRAINT `fk_invite_family` FOREIGN KEY (`family_id`) REFERENCES `fm_family` (`id`),
  CONSTRAINT `fk_invite_creator` FOREIGN KEY (`creator_id`) REFERENCES `fm_user` (`id`)
) ENGINE=InnoDB COMMENT='家庭邀请码';

CREATE TABLE `fm_family_milestone` (
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
) ENGINE=InnoDB COMMENT='family milestone reminder';

CREATE TABLE `fm_family_milestone_viewer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `milestone_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_milestone_viewer` (`milestone_id`,`user_id`),
  KEY `idx_milestone_viewer_user` (`user_id`),
  CONSTRAINT `fk_milestone_viewer_item` FOREIGN KEY (`milestone_id`) REFERENCES `fm_family_milestone` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_milestone_viewer_user` FOREIGN KEY (`user_id`) REFERENCES `fm_user` (`id`)
) ENGINE=InnoDB COMMENT='milestone specified viewers';

CREATE TABLE `fm_memory` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `family_id` BIGINT NOT NULL,
  `author_id` BIGINT NOT NULL,
  `type` VARCHAR(20) NOT NULL DEFAULT 'photo' COMMENT 'photo/video/note/audio',
  `title` VARCHAR(120) NOT NULL,
  `content` TEXT DEFAULT NULL,
  `memory_date` DATE NOT NULL COMMENT '事情实际发生日期',
  `memory_year` SMALLINT NOT NULL COMMENT '冗余年份，用于时间轴筛选',
  `location` VARCHAR(200) DEFAULT NULL,
  `audio_url` VARCHAR(500) DEFAULT NULL,
  `audio_duration` INT DEFAULT NULL COMMENT '音频时长，秒',
  `likes_count` INT NOT NULL DEFAULT 0,
  `comments_count` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_memory_family_date` (`family_id`,`memory_date` DESC),
  KEY `idx_memory_family_year` (`family_id`,`memory_year`),
  KEY `idx_memory_author` (`author_id`),
  FULLTEXT KEY `ft_memory_text` (`title`,`content`),
  CONSTRAINT `fk_memory_family` FOREIGN KEY (`family_id`) REFERENCES `fm_family` (`id`),
  CONSTRAINT `fk_memory_author` FOREIGN KEY (`author_id`) REFERENCES `fm_user` (`id`)
) ENGINE=InnoDB COMMENT='家庭记忆';

CREATE TABLE `fm_memory_media` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `memory_id` BIGINT NOT NULL,
  `media_type` VARCHAR(20) NOT NULL DEFAULT 'image',
  `url` VARCHAR(500) NOT NULL,
  `thumbnail_url` VARCHAR(500) DEFAULT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_media_memory_order` (`memory_id`,`sort_order`),
  CONSTRAINT `fk_media_memory` FOREIGN KEY (`memory_id`) REFERENCES `fm_memory` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='记忆图片和视频';

CREATE TABLE `fm_memory_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `memory_id` BIGINT NOT NULL,
  `tag_name` VARCHAR(30) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_memory_tag` (`memory_id`,`tag_name`),
  KEY `idx_tag_name` (`tag_name`),
  CONSTRAINT `fk_tag_memory` FOREIGN KEY (`memory_id`) REFERENCES `fm_memory` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='记忆标签';

CREATE TABLE `fm_memory_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `memory_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_memory_like` (`memory_id`,`user_id`),
  KEY `idx_like_user` (`user_id`),
  CONSTRAINT `fk_like_memory` FOREIGN KEY (`memory_id`) REFERENCES `fm_memory` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_like_user` FOREIGN KEY (`user_id`) REFERENCES `fm_user` (`id`)
) ENGINE=InnoDB COMMENT='记忆点赞';

CREATE TABLE `fm_memory_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `memory_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `content` VARCHAR(500) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_comment_memory_time` (`memory_id`,`created_at`),
  KEY `idx_comment_user` (`user_id`),
  CONSTRAINT `fk_comment_memory` FOREIGN KEY (`memory_id`) REFERENCES `fm_memory` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `fm_user` (`id`)
) ENGINE=InnoDB COMMENT='家人对记忆的回应';

CREATE TABLE `fm_time_capsule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `family_id` BIGINT NOT NULL,
  `creator_id` BIGINT NOT NULL,
  `recipient` VARCHAR(60) DEFAULT NULL COMMENT '收件人称呼',
  `title` VARCHAR(120) NOT NULL,
  `content` TEXT NOT NULL,
  `cover_url` VARCHAR(500) DEFAULT NULL,
  `open_date` DATE NOT NULL,
  `is_opened` TINYINT NOT NULL DEFAULT 0,
  `opened_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_capsule_family_open` (`family_id`,`open_date`),
  KEY `idx_capsule_creator` (`creator_id`),
  CONSTRAINT `fk_capsule_family` FOREIGN KEY (`family_id`) REFERENCES `fm_family` (`id`),
  CONSTRAINT `fk_capsule_creator` FOREIGN KEY (`creator_id`) REFERENCES `fm_user` (`id`)
) ENGINE=InnoDB COMMENT='时光胶囊';

CREATE TABLE `fm_file_upload` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `original_name` VARCHAR(255) NOT NULL,
  `stored_name` VARCHAR(100) NOT NULL,
  `file_path` VARCHAR(500) NOT NULL,
  `file_url` VARCHAR(500) NOT NULL,
  `file_size` BIGINT NOT NULL,
  `file_type` VARCHAR(100) NOT NULL,
  `category` VARCHAR(30) NOT NULL DEFAULT 'memory',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_file_user` (`user_id`),
  KEY `idx_file_category` (`category`),
  CONSTRAINT `fk_file_user` FOREIGN KEY (`user_id`) REFERENCES `fm_user` (`id`)
) ENGINE=InnoDB COMMENT='上传文件审计记录';

SET FOREIGN_KEY_CHECKS = 1;

-- 演示用户密码均为 123456
INSERT INTO `fm_user` (`id`,`username`,`password`,`nickname`,`status`) VALUES
  (1,'dad','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi','爸爸',1),
  (2,'mom','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi','妈妈',1),
  (3,'xiaoman','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi','小满',1),
  (4,'grandma','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi','奶奶',1);

INSERT INTO `fm_family` (`id`,`name`,`description`,`cover_url`,`creator_id`,`created_at`) VALUES
  (1,'林家小院','平凡日子里的相亲相爱','/assets/images/family-sunset.jpg',2,'2023-01-01 09:00:00');

INSERT INTO `fm_family_member` (`family_id`,`user_id`,`role`,`display_name`,`color`,`is_admin`,`joined_at`) VALUES
  (1,1,'记录者','爸爸','sky',1,'2023-01-01 09:00:00'),
  (1,2,'家庭管理员','妈妈','peach',1,'2023-01-01 09:00:00'),
  (1,3,'故事主角','小满','amber',0,'2023-01-01 09:10:00'),
  (1,4,'家庭成员','奶奶','emerald',0,'2023-01-01 09:20:00');

INSERT INTO `fm_memory` (`id`,`family_id`,`author_id`,`type`,`title`,`content`,`memory_date`,`memory_year`,`location`,`likes_count`,`comments_count`) VALUES
  (1,1,2,'photo','湖边的晚风','临时决定绕远路回家，结果遇见了今年最漂亮的一场晚霞。小满一路追着风跑，爸爸在后面喊着慢一点。','2026-08-07',2026,'东湖绿道',2,1),
  (2,1,1,'photo','第一次自己系鞋带','认真了十分钟，终于成功。小小的事情，也值得好好记住。','2026-06-18',2026,'家',3,0),
  (3,1,4,'photo','奶奶的六十八岁','没有隆重的仪式，只有一家人围坐在一起分享蛋糕。','2026-06-03',2026,'林家小院',4,1),
  (4,1,2,'photo','回到外婆家的那个夏天','旧院子里的桂花还没开，我们在门口拍下了这张有点模糊、却很珍贵的合照。','2023-08-07',2023,'外婆家',4,0);

INSERT INTO `fm_memory_media` (`memory_id`,`media_type`,`url`,`sort_order`) VALUES
  (1,'image','/assets/images/lake-sunset.jpg',0),
  (2,'image','/assets/images/child-growing.jpg',0),
  (3,'image','/assets/images/birthday-cake.jpg',0),
  (4,'image','/assets/images/family-sunset.jpg',0);

INSERT INTO `fm_memory_tag` (`memory_id`,`tag_name`) VALUES
  (1,'生活碎片'),(1,'晚霞'),(2,'小满成长'),(3,'生日'),(4,'旧照片');

INSERT INTO `fm_memory_like` (`memory_id`,`user_id`) VALUES (1,1),(1,4),(2,2),(2,3),(2,4),(3,1),(3,2),(3,3),(3,4),(4,1),(4,2),(4,3),(4,4);
INSERT INTO `fm_memory_comment` (`memory_id`,`user_id`,`content`) VALUES (1,1,'下次还走这条路，记得带上我。'),(3,2,'愿奶奶一直健康快乐。');

INSERT INTO `fm_time_capsule` (`family_id`,`creator_id`,`recipient`,`title`,`content`,`open_date`) VALUES
  (1,2,'小满','小满的十八岁生日','亲爱的小满，当你打开这封信的时候，你已经十八岁了。愿你仍然善良、勇敢，也永远知道家是可以回来的地方。','2031-08-07'),
  (1,1,'爸爸和妈妈','结婚十周年的约定','希望十周年这一天，我们还能一起散步、一起吃饭，也还记得当初写下这封信时的心情。','2028-05-20');
