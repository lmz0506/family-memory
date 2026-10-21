-- 全后端数据库中文注释更新：14张表、123个字段。
-- 根据当前 family_memory_new 的 information_schema 只读元数据生成；不执行回填，不修改业务数据。
-- MODIFY COLUMN 保留本次读取的类型、字符集、排序规则、空值、默认值、自增及更新时间行为。
-- 如执行前又修改了字段结构，请先核对定义；DDL会自动提交，请按数据库变更流程执行。
-- 不删除表、不删除索引、不重置数据，不涉及珍藏开启状态。
USE `family_memory_new`;
SET NAMES utf8mb4;

-- 家庭空间
ALTER TABLE `fm_family`
  COMMENT = '家庭空间',
  MODIFY COLUMN `id` bigint NOT NULL auto_increment COMMENT '主键ID，对应表：fm_family.id',
  MODIFY COLUMN `name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '家庭名称',
  MODIFY COLUMN `description` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '家庭介绍',
  MODIFY COLUMN `cover_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '封面图片地址',
  MODIFY COLUMN `creator_id` bigint NOT NULL COMMENT '创建者ID，来源表：fm_user.id',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP COMMENT '最近更新时间',
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志，0有效，1已删除';

-- 家庭成员关系
ALTER TABLE `fm_family_member`
  COMMENT = '家庭成员关系',
  MODIFY COLUMN `id` bigint NOT NULL auto_increment COMMENT '主键ID，对应表：fm_family_member.id',
  MODIFY COLUMN `family_id` bigint NOT NULL COMMENT '家庭ID，来源表：fm_family.id',
  MODIFY COLUMN `user_id` bigint NOT NULL COMMENT '用户ID，来源表：fm_user.id',
  MODIFY COLUMN `role` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '家庭成员' COMMENT '家庭角色名称，自由文本，例如家庭成员、家庭管理员',
  MODIFY COLUMN `display_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '家庭内称呼',
  MODIFY COLUMN `color` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'peach' COMMENT '成员主题色，peach桃色，sky天蓝，amber琥珀，emerald翠绿，可扩展',
  MODIFY COLUMN `is_admin` tinyint NOT NULL DEFAULT 0 COMMENT '管理员标志，0普通成员，1管理员',
  MODIFY COLUMN `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入家庭时间',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP COMMENT '最近更新时间',
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志，0有效，1已删除';

-- 家庭大事记提醒
ALTER TABLE `fm_family_milestone`
  COMMENT = '家庭大事记提醒',
  MODIFY COLUMN `id` bigint NOT NULL auto_increment COMMENT '主键ID，对应表：fm_family_milestone.id',
  MODIFY COLUMN `family_id` bigint NOT NULL COMMENT '家庭ID，来源表：fm_family.id',
  MODIFY COLUMN `creator_id` bigint NOT NULL COMMENT '创建者ID，来源表：fm_user.id',
  MODIFY COLUMN `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题，最多120字',
  MODIFY COLUMN `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '提醒说明',
  MODIFY COLUMN `event_date` date NOT NULL COMMENT '提醒对应的原始日期',
  MODIFY COLUMN `recurring_yearly` tinyint NOT NULL DEFAULT 0 COMMENT '每年重复标志，0一次性提醒，1每年重复',
  MODIFY COLUMN `visibility_scope` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PRIVATE' COMMENT '可见范围，PRIVATE仅创建者，FAMILY全体家人，SPECIFIC指定成员',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP COMMENT '最近更新时间',
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志，0有效，1已删除';

-- 家庭大事记指定可见成员
ALTER TABLE `fm_family_milestone_viewer`
  COMMENT = '家庭大事记指定可见成员',
  MODIFY COLUMN `id` bigint NOT NULL auto_increment COMMENT '主键ID，对应表：fm_family_milestone_viewer.id',
  MODIFY COLUMN `milestone_id` bigint NOT NULL COMMENT '大事记ID，来源表：fm_family_milestone.id',
  MODIFY COLUMN `user_id` bigint NOT NULL COMMENT '用户ID，来源表：fm_user.id',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 文件上传记录
ALTER TABLE `fm_file_upload`
  COMMENT = '文件上传记录',
  MODIFY COLUMN `id` bigint NOT NULL auto_increment COMMENT '主键ID，对应表：fm_file_upload.id',
  MODIFY COLUMN `user_id` bigint NOT NULL COMMENT '用户ID，来源表：fm_user.id',
  MODIFY COLUMN `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户上传时的原始文件名',
  MODIFY COLUMN `stored_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '服务端生成的存储文件名',
  MODIFY COLUMN `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件在服务端的存储路径',
  MODIFY COLUMN `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '对外访问文件的资源地址',
  MODIFY COLUMN `file_size` bigint NOT NULL COMMENT '文件大小，单位字节',
  MODIFY COLUMN `file_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件MIME类型，例如image/jpeg、video/mp4、audio/mpeg',
  MODIFY COLUMN `category` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'memory' COMMENT '文件业务分类，默认memory记忆，可由调用方扩展',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 家庭邀请码
ALTER TABLE `fm_invite_code`
  COMMENT = '家庭邀请码',
  MODIFY COLUMN `id` bigint NOT NULL auto_increment COMMENT '主键ID，对应表：fm_invite_code.id',
  MODIFY COLUMN `family_id` bigint NOT NULL COMMENT '家庭ID，来源表：fm_family.id',
  MODIFY COLUMN `creator_id` bigint NOT NULL COMMENT '创建者ID，来源表：fm_user.id',
  MODIFY COLUMN `code` char(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '家庭邀请码，8位且唯一',
  MODIFY COLUMN `expire_at` datetime NOT NULL COMMENT '邀请码到期时间',
  MODIFY COLUMN `max_uses` int NOT NULL DEFAULT 1 COMMENT '允许使用的最大次数',
  MODIFY COLUMN `used_count` int NOT NULL DEFAULT 0 COMMENT '已使用次数',
  MODIFY COLUMN `status` tinyint NOT NULL DEFAULT 1 COMMENT '邀请码状态，1可用，0停用或次数已用尽',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 家庭记忆
ALTER TABLE `fm_memory`
  COMMENT = '家庭记忆',
  MODIFY COLUMN `id` bigint NOT NULL auto_increment COMMENT '主键ID，对应表：fm_memory.id',
  MODIFY COLUMN `family_id` bigint NOT NULL COMMENT '家庭ID，来源表：fm_family.id',
  MODIFY COLUMN `author_id` bigint NOT NULL COMMENT '发布者ID，来源表：fm_user.id',
  MODIFY COLUMN `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'photo' COMMENT '记忆类型，photo照片，video视频，note文字，audio声音',
  MODIFY COLUMN `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标题，最多120字',
  MODIFY COLUMN `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '正文内容',
  MODIFY COLUMN `memory_date` date NOT NULL COMMENT '事情实际发生日期，发布后固定，也是星海归档日期',
  MODIFY COLUMN `memory_year` smallint NOT NULL COMMENT '记忆年份，取memory_date的年份，用于传统时间轴筛选',
  MODIFY COLUMN `location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '记忆发生地点',
  MODIFY COLUMN `audio_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '音频资源地址',
  MODIFY COLUMN `audio_duration` int NULL DEFAULT NULL COMMENT '音频时长，单位秒',
  MODIFY COLUMN `likes_count` int NOT NULL DEFAULT 0 COMMENT '累计点赞数',
  MODIFY COLUMN `comments_count` int NOT NULL DEFAULT 0 COMMENT '累计评论数',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP COMMENT '最近更新时间',
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志，0有效，1已删除';

-- 记忆评论
ALTER TABLE `fm_memory_comment`
  COMMENT = '记忆评论',
  MODIFY COLUMN `id` bigint NOT NULL auto_increment COMMENT '主键ID，对应表：fm_memory_comment.id',
  MODIFY COLUMN `memory_id` bigint NOT NULL COMMENT '记忆ID，来源表：fm_memory.id',
  MODIFY COLUMN `user_id` bigint NOT NULL COMMENT '用户ID，来源表：fm_user.id',
  MODIFY COLUMN `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '评论正文，最多500字',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志，0有效，1已删除';

-- 记忆点赞
ALTER TABLE `fm_memory_like`
  COMMENT = '记忆点赞',
  MODIFY COLUMN `id` bigint NOT NULL auto_increment COMMENT '主键ID，对应表：fm_memory_like.id',
  MODIFY COLUMN `memory_id` bigint NOT NULL COMMENT '记忆ID，来源表：fm_memory.id',
  MODIFY COLUMN `user_id` bigint NOT NULL COMMENT '用户ID，来源表：fm_user.id',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 记忆图片与视频
ALTER TABLE `fm_memory_media`
  COMMENT = '记忆图片与视频',
  MODIFY COLUMN `id` bigint NOT NULL auto_increment COMMENT '主键ID，对应表：fm_memory_media.id',
  MODIFY COLUMN `memory_id` bigint NOT NULL COMMENT '记忆ID，来源表：fm_memory.id',
  MODIFY COLUMN `media_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'image' COMMENT '媒体类型，image图片，video视频',
  MODIFY COLUMN `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '原始媒体资源地址',
  MODIFY COLUMN `thumbnail_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '媒体缩略图地址，未生成时为空',
  MODIFY COLUMN `sort_order` int NOT NULL DEFAULT 0 COMMENT '媒体展示顺序，按数值升序',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 记忆标签
ALTER TABLE `fm_memory_tag`
  COMMENT = '记忆标签',
  MODIFY COLUMN `id` bigint NOT NULL auto_increment COMMENT '主键ID，对应表：fm_memory_tag.id',
  MODIFY COLUMN `memory_id` bigint NOT NULL COMMENT '记忆ID，来源表：fm_memory.id',
  MODIFY COLUMN `tag_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标签名称，同一记忆下唯一',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 珍藏时光胶囊
ALTER TABLE `fm_time_capsule`
  COMMENT = '珍藏时光胶囊',
  MODIFY COLUMN `id` bigint NOT NULL auto_increment COMMENT '主键ID，对应表：fm_time_capsule.id',
  MODIFY COLUMN `family_id` bigint NOT NULL COMMENT '家庭ID，来源表：fm_family.id',
  MODIFY COLUMN `creator_id` bigint NOT NULL COMMENT '创建者ID，来源表：fm_user.id',
  MODIFY COLUMN `recipient` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '收信人称呼',
  MODIFY COLUMN `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标题，最多120字',
  MODIFY COLUMN `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '封存正文，仅达到开启条件后由详情接口返回',
  MODIFY COLUMN `cover_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '封面图片地址',
  MODIFY COLUMN `open_date` date NOT NULL COMMENT '约定开启日期，仅控制内容解锁，不作为星海归档日期',
  MODIFY COLUMN `is_opened` tinyint NOT NULL DEFAULT 0 COMMENT '开启标志，0尚未记录开启，1已开启；接口还会结合open_date判断是否可读',
  MODIFY COLUMN `opened_at` datetime NULL DEFAULT NULL COMMENT '首次实际开启时间，未开启时为空',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '封存时间，日期部分作为星海归档日期，发布后固定',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP COMMENT '最近更新时间',
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志，0有效，1已删除';

-- 星海统一内容索引
ALTER TABLE `fm_universe_index`
  COMMENT = '星海统一内容索引',
  MODIFY COLUMN `id` bigint NOT NULL auto_increment COMMENT '主键ID，对应表：fm_universe_index.id',
  MODIFY COLUMN `family_id` bigint NOT NULL COMMENT '家庭ID，来源表：fm_family.id',
  MODIFY COLUMN `source_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '来源类型，MEMORY记忆，CAPSULE珍藏',
  MODIFY COLUMN `source_id` bigint NOT NULL COMMENT '来源ID，MEMORY来源表：fm_memory.id；CAPSULE来源表：fm_time_capsule.id',
  MODIFY COLUMN `node_date` date NOT NULL COMMENT '归档日期，MEMORY来源表：fm_memory.memory_date；CAPSULE来源表：fm_time_capsule.created_at的日期部分，发布后固定',
  MODIFY COLUMN `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题副本，来源表：fm_memory.title或fm_time_capsule.title，由对应业务同步',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '索引行创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP COMMENT '索引行最近同步更新时间',
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志，0有效，1已删除';

-- 用户账号
ALTER TABLE `fm_user`
  COMMENT = '用户账号',
  MODIFY COLUMN `id` bigint NOT NULL auto_increment COMMENT '主键ID，对应表：fm_user.id',
  MODIFY COLUMN `username` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '登录用户名，家庭记忆应用内唯一',
  MODIFY COLUMN `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '登录密码的BCrypt摘要，不保存明文',
  MODIFY COLUMN `nickname` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户显示昵称',
  MODIFY COLUMN `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像图片地址',
  MODIFY COLUMN `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号码',
  MODIFY COLUMN `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '电子邮箱地址',
  MODIFY COLUMN `status` tinyint NOT NULL DEFAULT 1 COMMENT '账号状态，1正常，0停用',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP COMMENT '最近更新时间',
  MODIFY COLUMN `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志，0有效，1已删除';

-- 执行后的注释核对，仅查询元数据。
SELECT TABLE_NAME,TABLE_COMMENT FROM information_schema.TABLES
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME LIKE 'fm\_%' ORDER BY TABLE_NAME;
SELECT TABLE_NAME,COLUMN_NAME,COLUMN_COMMENT FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME LIKE 'fm\_%' ORDER BY TABLE_NAME,ORDINAL_POSITION;
