-- ============================================
-- 家庭记忆应用 - 数据库设计文档
-- Family Memory Application - Database Design
-- 数据库名称: family_memory
-- 字符集: utf8mb4
-- 排序规则: utf8mb4_general_ci
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `family_memory`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;

USE `family_memory`;

-- ============================================
-- 1. 用户表 (fm_user)
-- 存储系统用户的基本信息
-- ============================================
DROP TABLE IF EXISTS `fm_user`;
CREATE TABLE `fm_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID，主键',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名，用于登录',
    `password` VARCHAR(255) NOT NULL COMMENT '密码，BCrypt加密存储',
    `nickname` VARCHAR(50) NOT NULL COMMENT '昵称，显示名称',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL或emoji',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_phone` (`phone`),
    KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- ============================================
-- 2. 家庭表 (fm_family)
-- 存储家庭/群组的基本信息
-- ============================================
DROP TABLE IF EXISTS `fm_family`;
CREATE TABLE `fm_family` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '家庭ID，主键',
    `name` VARCHAR(100) NOT NULL COMMENT '家庭名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '家庭描述',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '家庭头像URL',
    `creator_id` BIGINT NOT NULL COMMENT '创建者ID，逻辑外键 -> fm_user.id',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_creator_id` (`creator_id`) COMMENT '索引：创建者ID，关联fm_user.id'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='家庭表';

-- ============================================
-- 3. 家庭成员表 (fm_family_member)
-- 存储家庭成员关系，多对多关联用户和家庭
-- ============================================
DROP TABLE IF EXISTS `fm_family_member`;
CREATE TABLE `fm_family_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '成员关系ID，主键',
    `family_id` BIGINT NOT NULL COMMENT '家庭ID，逻辑外键 -> fm_family.id',
    `user_id` BIGINT NOT NULL COMMENT '用户ID，逻辑外键 -> fm_user.id',
    `role` VARCHAR(50) NOT NULL DEFAULT '成员' COMMENT '家庭角色，如：一家之主、超人妈妈、捣蛋鬼等',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '在家庭中的昵称，如：爸爸、妈妈、小宝',
    `color` VARCHAR(50) DEFAULT 'bg-gray-100' COMMENT '成员卡片背景色，Tailwind CSS类名',
    `is_admin` TINYINT NOT NULL DEFAULT 0 COMMENT '是否管理员：0-否，1-是',
    `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_family_user` (`family_id`, `user_id`) COMMENT '唯一索引：一个用户在一个家庭中只能有一条记录',
    KEY `idx_family_id` (`family_id`) COMMENT '索引：家庭ID，关联fm_family.id',
    KEY `idx_user_id` (`user_id`) COMMENT '索引：用户ID，关联fm_user.id'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='家庭成员表';

-- ============================================
-- 4. 记忆表 (fm_memory)
-- 存储家庭记忆/动态的主体信息
-- ============================================
DROP TABLE IF EXISTS `fm_memory`;
CREATE TABLE `fm_memory` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记忆ID，主键',
    `family_id` BIGINT NOT NULL COMMENT '所属家庭ID，逻辑外键 -> fm_family.id',
    `author_id` BIGINT NOT NULL COMMENT '作者用户ID，逻辑外键 -> fm_user.id',
    `type` VARCHAR(20) NOT NULL DEFAULT 'photo' COMMENT '记忆类型：photo-照片，video-视频，note-笔记',
    `title` VARCHAR(200) NOT NULL COMMENT '记忆标题',
    `description` TEXT COMMENT '记忆描述/内容',
    `memory_date` DATE NOT NULL COMMENT '记忆发生日期',
    `memory_year` VARCHAR(4) NOT NULL COMMENT '记忆年份，冗余字段便于筛选',
    `location` VARCHAR(200) DEFAULT NULL COMMENT '位置信息',
    `audio_url` VARCHAR(500) DEFAULT NULL COMMENT '语音文件URL',
    `audio_duration` INT DEFAULT NULL COMMENT '语音时长（秒）',
    `likes_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数量，冗余字段',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_family_id` (`family_id`) COMMENT '索引：家庭ID，关联fm_family.id',
    KEY `idx_author_id` (`author_id`) COMMENT '索引：作者ID，关联fm_user.id',
    KEY `idx_memory_date` (`memory_date`) COMMENT '索引：记忆日期，用于那年今日查询',
    KEY `idx_memory_year` (`memory_year`) COMMENT '索引：记忆年份，用于时光轴筛选',
    KEY `idx_created_at` (`created_at`) COMMENT '索引：创建时间，用于最新动态排序'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='记忆表';

-- ============================================
-- 5. 记忆媒体表 (fm_memory_media)
-- 存储记忆关联的图片/视频文件
-- ============================================
DROP TABLE IF EXISTS `fm_memory_media`;
CREATE TABLE `fm_memory_media` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '媒体ID，主键',
    `memory_id` BIGINT NOT NULL COMMENT '记忆ID，逻辑外键 -> fm_memory.id',
    `media_type` VARCHAR(20) NOT NULL DEFAULT 'image' COMMENT '媒体类型：image-图片，video-视频',
    `url` VARCHAR(500) NOT NULL COMMENT '媒体文件URL',
    `thumbnail_url` VARCHAR(500) DEFAULT NULL COMMENT '缩略图URL',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序，升序排列',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_memory_id` (`memory_id`) COMMENT '索引：记忆ID，关联fm_memory.id'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='记忆媒体表';

-- ============================================
-- 6. 记忆标签表 (fm_memory_tag)
-- 存储记忆的标签信息
-- ============================================
DROP TABLE IF EXISTS `fm_memory_tag`;
CREATE TABLE `fm_memory_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '标签关联ID，主键',
    `memory_id` BIGINT NOT NULL COMMENT '记忆ID，逻辑外键 -> fm_memory.id',
    `tag_name` VARCHAR(50) NOT NULL COMMENT '标签名称',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_memory_id` (`memory_id`) COMMENT '索引：记忆ID，关联fm_memory.id',
    KEY `idx_tag_name` (`tag_name`) COMMENT '索引：标签名称，用于标签搜索'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='记忆标签表';

-- ============================================
-- 7. 记忆点赞表 (fm_memory_like)
-- 存储用户对记忆的点赞记录
-- ============================================
DROP TABLE IF EXISTS `fm_memory_like`;
CREATE TABLE `fm_memory_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '点赞ID，主键',
    `memory_id` BIGINT NOT NULL COMMENT '记忆ID，逻辑外键 -> fm_memory.id',
    `user_id` BIGINT NOT NULL COMMENT '点赞用户ID，逻辑外键 -> fm_user.id',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_memory_user` (`memory_id`, `user_id`) COMMENT '唯一索引：一个用户对一条记忆只能点赞一次',
    KEY `idx_memory_id` (`memory_id`) COMMENT '索引：记忆ID，关联fm_memory.id',
    KEY `idx_user_id` (`user_id`) COMMENT '索引：用户ID，关联fm_user.id'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='记忆点赞表';

-- ============================================
-- 8. 时光胶囊表 (fm_time_capsule)
-- 存储时光胶囊信息
-- ============================================
DROP TABLE IF EXISTS `fm_time_capsule`;
CREATE TABLE `fm_time_capsule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '胶囊ID，主键',
    `family_id` BIGINT NOT NULL COMMENT '所属家庭ID，逻辑外键 -> fm_family.id',
    `creator_id` BIGINT NOT NULL COMMENT '创建者用户ID，逻辑外键 -> fm_user.id',
    `title` VARCHAR(200) NOT NULL COMMENT '胶囊标题',
    `content` TEXT NOT NULL COMMENT '胶囊内容',
    `open_date` DATE NOT NULL COMMENT '开启日期',
    `is_opened` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已开启：0-未开启，1-已开启',
    `opened_at` DATETIME DEFAULT NULL COMMENT '实际开启时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_family_id` (`family_id`) COMMENT '索引：家庭ID，关联fm_family.id',
    KEY `idx_creator_id` (`creator_id`) COMMENT '索引：创建者ID，关联fm_user.id',
    KEY `idx_open_date` (`open_date`) COMMENT '索引：开启日期，用于定时任务查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='时光胶囊表';

-- ============================================
-- 9. 邀请码表 (fm_invite_code)
-- 存储家庭邀请码信息
-- ============================================
DROP TABLE IF EXISTS `fm_invite_code`;
CREATE TABLE `fm_invite_code` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '邀请码ID，主键',
    `family_id` BIGINT NOT NULL COMMENT '所属家庭ID，逻辑外键 -> fm_family.id',
    `creator_id` BIGINT NOT NULL COMMENT '创建者用户ID，逻辑外键 -> fm_user.id',
    `code` VARCHAR(20) NOT NULL COMMENT '邀请码，8位随机字符',
    `expire_at` DATETIME NOT NULL COMMENT '过期时间',
    `max_uses` INT NOT NULL DEFAULT 1 COMMENT '最大使用次数',
    `used_count` INT NOT NULL DEFAULT 0 COMMENT '已使用次数',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-已失效，1-有效',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`) COMMENT '唯一索引：邀请码唯一',
    KEY `idx_family_id` (`family_id`) COMMENT '索引：家庭ID，关联fm_family.id',
    KEY `idx_expire_at` (`expire_at`) COMMENT '索引：过期时间，用于清理过期邀请码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='邀请码表';

-- ============================================
-- 10. 文件上传记录表 (fm_file_upload)
-- 存储文件上传记录，便于管理和清理
-- ============================================
DROP TABLE IF EXISTS `fm_file_upload`;
CREATE TABLE `fm_file_upload` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件ID，主键',
    `user_id` BIGINT NOT NULL COMMENT '上传用户ID，逻辑外键 -> fm_user.id',
    `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `stored_name` VARCHAR(255) NOT NULL COMMENT '存储文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    `file_url` VARCHAR(500) NOT NULL COMMENT '文件访问URL',
    `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
    `file_type` VARCHAR(50) NOT NULL COMMENT '文件MIME类型',
    `category` VARCHAR(50) NOT NULL DEFAULT 'memory' COMMENT '文件分类：memory-记忆，avatar-头像，audio-语音',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`) COMMENT '索引：用户ID，关联fm_user.id',
    KEY `idx_category` (`category`) COMMENT '索引：文件分类'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文件上传记录表';

-- ============================================
-- 初始化测试数据
-- ============================================

-- 插入测试用户
INSERT INTO `fm_user` (`id`, `username`, `password`, `nickname`, `avatar`, `phone`) VALUES
(1, 'dad', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '爸爸', '👨', '13800138001'),
(2, 'mom', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '妈妈', '👩', '13800138002'),
(3, 'baby', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '小宝', '👶', '13800138003'),
(4, 'grandpa', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '爷爷', '👴', '13800138004');
-- 密码都是: 123456

-- 插入测试家庭
INSERT INTO `fm_family` (`id`, `name`, `description`, `creator_id`) VALUES
(1, '温馨小家', '我们幸福的一家人', 1);

-- 插入家庭成员关系
INSERT INTO `fm_family_member` (`family_id`, `user_id`, `role`, `nickname`, `color`, `is_admin`) VALUES
(1, 1, '一家之主', '爸爸', 'bg-blue-100', 1),
(1, 2, '超人妈妈', '妈妈', 'bg-pink-100', 1),
(1, 3, '捣蛋鬼', '小宝', 'bg-yellow-100', 0),
(1, 4, '故事大王', '爷爷', 'bg-gray-100', 0);

-- 插入测试记忆
INSERT INTO `fm_memory` (`id`, `family_id`, `author_id`, `type`, `title`, `description`, `memory_date`, `memory_year`, `location`, `likes_count`) VALUES
(1, 1, 2, 'photo', '宝宝第一次叫爸爸', '今天晚饭后，小宝突然看着爸爸清晰地喊了一声"爸爸"，大家都惊呆了！赶紧记录下来。', '2023-10-15', '2023', '温馨的家', 4),
(2, 1, 1, 'video', '全家海边露营', '趁着周末，带上帐篷去了海边。晚上的星星特别亮，海浪声很治愈。孩子们玩疯了，捡了好多贝壳。', '2023-09-01', '2023', '那琴半岛', 6),
(3, 1, 4, 'note', '致未来的孙儿', '今天是你出生的日子，爷爷想写几句话给你。无论未来世界变成什么样，希望你永远保持善良和勇敢...', '2015-05-20', '2015', '老家书房', 12),
(4, 1, 2, 'photo', '六一儿童节快乐', '公园野餐，今天天气真好！', '2023-06-01', '2023', '中心公园', 8);

-- 插入记忆媒体
INSERT INTO `fm_memory_media` (`memory_id`, `media_type`, `url`, `sort_order`) VALUES
(1, 'image', 'https://images.unsplash.com/photo-1519689680058-324335c77eba?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80', 0),
(2, 'image', 'https://images.unsplash.com/photo-1478131143081-80f7f84ca84d?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80', 0),
(2, 'image', 'https://images.unsplash.com/photo-1496950866446-325b842a9986?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80', 1),
(2, 'image', 'https://images.unsplash.com/photo-1523987355523-c7b5b0dd90a7?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80', 2),
(2, 'image', 'https://images.unsplash.com/photo-1504280390367-361c6d9f38f4?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80', 3),
(4, 'image', 'https://images.unsplash.com/photo-1606092195730-5d7b9af1ef4d?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80', 0),
(4, 'image', 'https://images.unsplash.com/photo-1566004100631-35d015d6a491?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80', 1);

-- 插入记忆标签
INSERT INTO `fm_memory_tag` (`memory_id`, `tag_name`) VALUES
(1, '成长'), (1, '第一次'),
(2, '旅行'), (2, '风景'),
(3, '家书'), (3, '传承'),
(4, '节日'), (4, '快乐');

-- 插入测试时光胶囊
INSERT INTO `fm_time_capsule` (`family_id`, `creator_id`, `title`, `content`, `open_date`) VALUES
(1, 2, '给十岁的小宝', '亲爱的小宝，当你打开这封信的时候，你已经十岁了...', '2025-05-20'),
(1, 4, '爷爷的心愿', '希望我们的家庭永远幸福美满...', '2030-01-01'),
(1, 1, '我们的约定', '等疫情结束，我们一起去看大海...', '2024-06-01');
