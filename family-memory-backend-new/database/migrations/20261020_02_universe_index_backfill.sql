-- 星海历史数据回填；先执行 20261020_01_universe_index_schema.sql。
-- 执行期间暂停记忆、珍藏及家庭的新增、修改、删除；回填后配合同事务同步代码上线。
-- 客户端必须遇错停止；发生任何错误请 ROLLBACK，不要继续执行 COMMIT。
-- 只维护 fm_universe_index，不更新或删除原业务记录，不开启任何珍藏。
-- 可重复执行：唯一键防重；已存在记录以业务源的家庭、日期、标题为准重新同步。
USE `family_memory_new`;
SET NAMES utf8mb4;

-- 一、回填前检查。下面的结果应为空；如有记录，先处理数据问题，再执行事务部分。
SELECT 'MEMORY' AS `source_type`, m.`id` AS `source_id`, m.`family_id`
FROM `fm_memory` m
LEFT JOIN `fm_family` f ON f.`id` = m.`family_id`
WHERE m.`deleted` = 0
  AND (f.`id` IS NULL OR (f.`deleted` = 0 AND
       (m.`memory_date` IS NULL OR YEAR(m.`memory_date`) = 0 OR MONTH(m.`memory_date`) = 0
        OR DAY(m.`memory_date`) = 0 OR m.`title` IS NULL OR CHAR_LENGTH(TRIM(m.`title`)) = 0)))
UNION ALL
SELECT 'CAPSULE', c.`id`, c.`family_id`
FROM `fm_time_capsule` c
LEFT JOIN `fm_family` f ON f.`id` = c.`family_id`
WHERE c.`deleted` = 0
  AND (f.`id` IS NULL OR (f.`deleted` = 0 AND
       (c.`created_at` IS NULL OR YEAR(c.`created_at`) = 0 OR MONTH(c.`created_at`) = 0
        OR DAY(c.`created_at`) = 0 OR c.`title` IS NULL OR CHAR_LENGTH(TRIM(c.`title`)) = 0)));

-- 二、回填当前有效业务内容。索引 created_at / updated_at 是索引行的审计时间，
-- 不替代 node_date，也不改变原内容的历史时间。
START TRANSACTION;

INSERT INTO `fm_universe_index`
  (`family_id`, `source_type`, `source_id`, `node_date`, `title`, `deleted`)
SELECT m.`family_id`, 'MEMORY', m.`id`, m.`memory_date`, m.`title`, 0
FROM `fm_memory` m
INNER JOIN `fm_family` f ON f.`id` = m.`family_id` AND f.`deleted` = 0
WHERE m.`deleted` = 0
ON DUPLICATE KEY UPDATE
  `family_id` = VALUES(`family_id`),
  `node_date` = VALUES(`node_date`),
  `title` = VALUES(`title`),
  `deleted` = 0;

INSERT INTO `fm_universe_index`
  (`family_id`, `source_type`, `source_id`, `node_date`, `title`, `deleted`)
SELECT c.`family_id`, 'CAPSULE', c.`id`, DATE(c.`created_at`), c.`title`, 0
FROM `fm_time_capsule` c
INNER JOIN `fm_family` f ON f.`id` = c.`family_id` AND f.`deleted` = 0
WHERE c.`deleted` = 0
ON DUPLICATE KEY UPDATE
  `family_id` = VALUES(`family_id`),
  `node_date` = VALUES(`node_date`),
  `title` = VALUES(`title`),
  `deleted` = 0;

-- 三、重跑时，将业务源已删除、已不存在或家庭已失效的旧索引逻辑删除。
-- 不硬删除索引行；source_type + source_id 的唯一性仍然保留。
UPDATE `fm_universe_index` i
LEFT JOIN `fm_family` f ON f.`id` = i.`family_id`
LEFT JOIN `fm_memory` m ON i.`source_type` = 'MEMORY' AND m.`id` = i.`source_id`
LEFT JOIN `fm_time_capsule` c ON i.`source_type` = 'CAPSULE' AND c.`id` = i.`source_id`
SET i.`deleted` = 1
WHERE i.`deleted` = 0
  AND (
    f.`id` IS NULL OR f.`deleted` <> 0
    OR (i.`source_type` = 'MEMORY'
        AND (m.`id` IS NULL OR m.`deleted` <> 0 OR m.`family_id` <> i.`family_id`))
    OR (i.`source_type` = 'CAPSULE'
        AND (c.`id` IS NULL OR c.`deleted` <> 0 OR c.`family_id` <> i.`family_id`))
  );

COMMIT;

-- 四、提交后执行 20261020_03_universe_index_verify.sql。
-- VALUES() 用法兼容当前项目的 MySQL 8.0；较新 8.0 版本可能提示该写法已弃用。
