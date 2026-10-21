-- 拾光家庭记忆应用数据库设计
-- MySQL 8.0 / utf8mb4 / 时区 Asia/Shanghai

CREATE DATABASE IF NOT EXISTS `family_memory_new`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE `family_memory_new`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `fm_universe_index`;
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
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应表：fm_user.id',
  `username` VARCHAR(30) NOT NULL COMMENT '登录用户名，家庭记忆应用内唯一',
  `password` VARCHAR(100) NOT NULL COMMENT '登录密码的BCrypt摘要，不保存明文',
  `nickname` VARCHAR(30) NOT NULL COMMENT '用户显示昵称',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像图片地址',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号码',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '电子邮箱地址',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态，1正常，0停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志，0有效，1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_phone` (`phone`)
) ENGINE=InnoDB COMMENT='用户账号';

CREATE TABLE `fm_family` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应表：fm_family.id',
  `name` VARCHAR(60) NOT NULL COMMENT '家庭名称',
  `description` VARCHAR(300) DEFAULT NULL COMMENT '家庭介绍',
  `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图片地址',
  `creator_id` BIGINT NOT NULL COMMENT '创建者ID，来源表：fm_user.id',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志，0有效，1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_family_creator` (`creator_id`),
  CONSTRAINT `fk_family_creator` FOREIGN KEY (`creator_id`) REFERENCES `fm_user` (`id`)
) ENGINE=InnoDB COMMENT='家庭空间';

CREATE TABLE `fm_family_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应表：fm_family_member.id',
  `family_id` BIGINT NOT NULL COMMENT '家庭ID，来源表：fm_family.id',
  `user_id` BIGINT NOT NULL COMMENT '用户ID，来源表：fm_user.id',
  `role` VARCHAR(30) NOT NULL DEFAULT '家庭成员' COMMENT '家庭角色名称，自由文本，例如家庭成员、家庭管理员',
  `display_name` VARCHAR(30) NOT NULL COMMENT '家庭内称呼',
  `color` VARCHAR(20) NOT NULL DEFAULT 'peach' COMMENT '成员主题色，peach桃色，sky天蓝，amber琥珀，emerald翠绿，可扩展',
  `is_admin` TINYINT NOT NULL DEFAULT 0 COMMENT '管理员标志，0普通成员，1管理员',
  `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入家庭时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志，0有效，1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_family_member` (`family_id`,`user_id`),
  KEY `idx_member_user` (`user_id`),
  CONSTRAINT `fk_member_family` FOREIGN KEY (`family_id`) REFERENCES `fm_family` (`id`),
  CONSTRAINT `fk_member_user` FOREIGN KEY (`user_id`) REFERENCES `fm_user` (`id`)
) ENGINE=InnoDB COMMENT='家庭成员关系';

CREATE TABLE `fm_invite_code` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应表：fm_invite_code.id',
  `family_id` BIGINT NOT NULL COMMENT '家庭ID，来源表：fm_family.id',
  `creator_id` BIGINT NOT NULL COMMENT '创建者ID，来源表：fm_user.id',
  `code` CHAR(8) NOT NULL COMMENT '家庭邀请码，8位且唯一',
  `expire_at` DATETIME NOT NULL COMMENT '邀请码到期时间',
  `max_uses` INT NOT NULL DEFAULT 1 COMMENT '允许使用的最大次数',
  `used_count` INT NOT NULL DEFAULT 0 COMMENT '已使用次数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '邀请码状态，1可用，0停用或次数已用尽',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invite_code` (`code`),
  KEY `idx_invite_family` (`family_id`),
  KEY `idx_invite_expire` (`expire_at`),
  CONSTRAINT `fk_invite_family` FOREIGN KEY (`family_id`) REFERENCES `fm_family` (`id`),
  CONSTRAINT `fk_invite_creator` FOREIGN KEY (`creator_id`) REFERENCES `fm_user` (`id`)
) ENGINE=InnoDB COMMENT='家庭邀请码';

CREATE TABLE `fm_family_milestone` (
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
) ENGINE=InnoDB COMMENT='家庭大事记提醒';

CREATE TABLE `fm_family_milestone_viewer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应表：fm_family_milestone_viewer.id',
  `milestone_id` BIGINT NOT NULL COMMENT '大事记ID，来源表：fm_family_milestone.id',
  `user_id` BIGINT NOT NULL COMMENT '用户ID，来源表：fm_user.id',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_milestone_viewer` (`milestone_id`,`user_id`),
  KEY `idx_milestone_viewer_user` (`user_id`),
  CONSTRAINT `fk_milestone_viewer_item` FOREIGN KEY (`milestone_id`) REFERENCES `fm_family_milestone` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_milestone_viewer_user` FOREIGN KEY (`user_id`) REFERENCES `fm_user` (`id`)
) ENGINE=InnoDB COMMENT='家庭大事记指定可见成员';

CREATE TABLE `fm_memory` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应表：fm_memory.id',
  `family_id` BIGINT NOT NULL COMMENT '家庭ID，来源表：fm_family.id',
  `author_id` BIGINT NOT NULL COMMENT '发布者ID，来源表：fm_user.id',
  `type` VARCHAR(20) NOT NULL DEFAULT 'photo' COMMENT '记忆类型，photo照片，video视频，note文字，audio声音',
  `title` VARCHAR(120) NOT NULL COMMENT '标题，最多120字',
  `content` TEXT DEFAULT NULL COMMENT '正文内容',
  `memory_date` DATE NOT NULL COMMENT '事情实际发生日期，发布后固定，也是星海归档日期',
  `memory_year` SMALLINT NOT NULL COMMENT '记忆年份，取memory_date的年份，用于传统时间轴筛选',
  `location` VARCHAR(200) DEFAULT NULL COMMENT '记忆发生地点',
  `audio_url` VARCHAR(500) DEFAULT NULL COMMENT '音频资源地址',
  `audio_duration` INT DEFAULT NULL COMMENT '音频时长，单位秒',
  `likes_count` INT NOT NULL DEFAULT 0 COMMENT '累计点赞数',
  `comments_count` INT NOT NULL DEFAULT 0 COMMENT '累计评论数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志，0有效，1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_memory_family_date` (`family_id`,`memory_date` DESC),
  KEY `idx_memory_family_year` (`family_id`,`memory_year`),
  KEY `idx_memory_author` (`author_id`),
  FULLTEXT KEY `ft_memory_text` (`title`,`content`),
  CONSTRAINT `fk_memory_family` FOREIGN KEY (`family_id`) REFERENCES `fm_family` (`id`),
  CONSTRAINT `fk_memory_author` FOREIGN KEY (`author_id`) REFERENCES `fm_user` (`id`)
) ENGINE=InnoDB COMMENT='家庭记忆';

CREATE TABLE `fm_memory_media` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应表：fm_memory_media.id',
  `memory_id` BIGINT NOT NULL COMMENT '记忆ID，来源表：fm_memory.id',
  `media_type` VARCHAR(20) NOT NULL DEFAULT 'image' COMMENT '媒体类型，image图片，video视频',
  `url` VARCHAR(500) NOT NULL COMMENT '原始媒体资源地址',
  `thumbnail_url` VARCHAR(500) DEFAULT NULL COMMENT '媒体缩略图地址，未生成时为空',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '媒体展示顺序，按数值升序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_media_memory_order` (`memory_id`,`sort_order`),
  CONSTRAINT `fk_media_memory` FOREIGN KEY (`memory_id`) REFERENCES `fm_memory` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='记忆图片与视频';

CREATE TABLE `fm_memory_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应表：fm_memory_tag.id',
  `memory_id` BIGINT NOT NULL COMMENT '记忆ID，来源表：fm_memory.id',
  `tag_name` VARCHAR(30) NOT NULL COMMENT '标签名称，同一记忆下唯一',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_memory_tag` (`memory_id`,`tag_name`),
  KEY `idx_tag_name` (`tag_name`),
  CONSTRAINT `fk_tag_memory` FOREIGN KEY (`memory_id`) REFERENCES `fm_memory` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='记忆标签';

CREATE TABLE `fm_memory_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应表：fm_memory_like.id',
  `memory_id` BIGINT NOT NULL COMMENT '记忆ID，来源表：fm_memory.id',
  `user_id` BIGINT NOT NULL COMMENT '用户ID，来源表：fm_user.id',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_memory_like` (`memory_id`,`user_id`),
  KEY `idx_like_user` (`user_id`),
  CONSTRAINT `fk_like_memory` FOREIGN KEY (`memory_id`) REFERENCES `fm_memory` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_like_user` FOREIGN KEY (`user_id`) REFERENCES `fm_user` (`id`)
) ENGINE=InnoDB COMMENT='记忆点赞';

CREATE TABLE `fm_memory_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应表：fm_memory_comment.id',
  `memory_id` BIGINT NOT NULL COMMENT '记忆ID，来源表：fm_memory.id',
  `user_id` BIGINT NOT NULL COMMENT '用户ID，来源表：fm_user.id',
  `content` VARCHAR(500) NOT NULL COMMENT '评论正文，最多500字',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志，0有效，1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_comment_memory_time` (`memory_id`,`created_at`),
  KEY `idx_comment_user` (`user_id`),
  CONSTRAINT `fk_comment_memory` FOREIGN KEY (`memory_id`) REFERENCES `fm_memory` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `fm_user` (`id`)
) ENGINE=InnoDB COMMENT='记忆评论';

CREATE TABLE `fm_time_capsule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应表：fm_time_capsule.id',
  `family_id` BIGINT NOT NULL COMMENT '家庭ID，来源表：fm_family.id',
  `creator_id` BIGINT NOT NULL COMMENT '创建者ID，来源表：fm_user.id',
  `recipient` VARCHAR(60) DEFAULT NULL COMMENT '收信人称呼',
  `title` VARCHAR(120) NOT NULL COMMENT '标题，最多120字',
  `content` TEXT NOT NULL COMMENT '封存正文，仅达到开启条件后由详情接口返回',
  `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图片地址',
  `open_date` DATE NOT NULL COMMENT '约定开启日期，仅控制内容解锁，不作为星海归档日期',
  `is_opened` TINYINT NOT NULL DEFAULT 0 COMMENT '开启标志，0尚未记录开启，1已开启；接口还会结合open_date判断是否可读',
  `opened_at` DATETIME DEFAULT NULL COMMENT '首次实际开启时间，未开启时为空',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '封存时间，日期部分作为星海归档日期，发布后固定',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志，0有效，1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_capsule_family_open` (`family_id`,`open_date`),
  KEY `idx_capsule_creator` (`creator_id`),
  CONSTRAINT `fk_capsule_family` FOREIGN KEY (`family_id`) REFERENCES `fm_family` (`id`),
  CONSTRAINT `fk_capsule_creator` FOREIGN KEY (`creator_id`) REFERENCES `fm_user` (`id`)
) ENGINE=InnoDB COMMENT='珍藏时光胶囊';

CREATE TABLE `fm_universe_index` (
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

CREATE TABLE `fm_file_upload` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，对应表：fm_file_upload.id',
  `user_id` BIGINT NOT NULL COMMENT '用户ID，来源表：fm_user.id',
  `original_name` VARCHAR(255) NOT NULL COMMENT '用户上传时的原始文件名',
  `stored_name` VARCHAR(100) NOT NULL COMMENT '服务端生成的存储文件名',
  `file_path` VARCHAR(500) NOT NULL COMMENT '文件在服务端的存储路径',
  `file_url` VARCHAR(500) NOT NULL COMMENT '对外访问文件的资源地址',
  `file_size` BIGINT NOT NULL COMMENT '文件大小，单位字节',
  `file_type` VARCHAR(100) NOT NULL COMMENT '文件MIME类型，例如image/jpeg、video/mp4、audio/mpeg',
  `category` VARCHAR(30) NOT NULL DEFAULT 'memory' COMMENT '文件业务分类，默认memory记忆，可由调用方扩展',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_file_user` (`user_id`),
  KEY `idx_file_category` (`category`),
  CONSTRAINT `fk_file_user` FOREIGN KEY (`user_id`) REFERENCES `fm_user` (`id`)
) ENGINE=InnoDB COMMENT='文件上传记录';

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


-- 初始化演示星海索引；本文件仅供重新初始化空库，已有数据库不得执行本文件。
INSERT INTO fm_universe_index(family_id,source_type,source_id,node_date,title,deleted)
SELECT family_id,'MEMORY',id,memory_date,title,0 FROM fm_memory WHERE deleted=0;
INSERT INTO fm_universe_index(family_id,source_type,source_id,node_date,title,deleted)
SELECT family_id,'CAPSULE',id,DATE(created_at),title,0 FROM fm_time_capsule WHERE deleted=0;
