-- 星海回填核对，MySQL 8.0；全部为只读 SELECT。
USE `family_memory_new`;

-- 1. 两类内容的预期数量与有效索引数量应一致，difference 应为 0。
WITH expected AS (
  SELECT 'MEMORY' AS source_type, m.id AS source_id
  FROM fm_memory m INNER JOIN fm_family f ON f.id = m.family_id AND f.deleted = 0
  WHERE m.deleted = 0
  UNION ALL
  SELECT 'CAPSULE', c.id
  FROM fm_time_capsule c INNER JOIN fm_family f ON f.id = c.family_id AND f.deleted = 0
  WHERE c.deleted = 0
), counts AS (
  SELECT source_type, COUNT(*) AS expected_count, 0 AS actual_count FROM expected GROUP BY source_type
  UNION ALL SELECT source_type, 0, COUNT(*) FROM fm_universe_index WHERE deleted = 0 GROUP BY source_type
  UNION ALL SELECT 'MEMORY', 0, 0
  UNION ALL SELECT 'CAPSULE', 0, 0
)
SELECT source_type, SUM(expected_count) AS expected_count, SUM(actual_count) AS actual_count,
       SUM(actual_count) - SUM(expected_count) AS difference
FROM counts GROUP BY source_type ORDER BY source_type;

-- 2. 缺失或字段不一致的索引；结果应为空。标题按字节比较，大小写差异也会检出。
WITH expected AS (
  SELECT 'MEMORY' AS source_type, m.id AS source_id, m.family_id, m.memory_date AS node_date, m.title
  FROM fm_memory m INNER JOIN fm_family f ON f.id = m.family_id AND f.deleted = 0
  WHERE m.deleted = 0
  UNION ALL
  SELECT 'CAPSULE', c.id, c.family_id, DATE(c.created_at), c.title
  FROM fm_time_capsule c INNER JOIN fm_family f ON f.id = c.family_id AND f.deleted = 0
  WHERE c.deleted = 0
)
SELECT e.source_type, e.source_id, e.family_id AS expected_family_id,
       i.family_id AS actual_family_id, e.node_date AS expected_date, i.node_date AS actual_date,
       e.title AS expected_title, i.title AS actual_title, i.deleted
FROM expected e
LEFT JOIN fm_universe_index i ON i.source_type = e.source_type AND i.source_id = e.source_id
WHERE i.id IS NULL OR i.deleted <> 0 OR NOT (i.family_id <=> e.family_id)
   OR NOT (i.node_date <=> e.node_date) OR NOT (BINARY i.title <=> BINARY e.title);

-- 3. 不应存在的有效索引（含未知来源类型）；结果应为空。
WITH expected AS (
  SELECT 'MEMORY' AS source_type, m.id AS source_id
  FROM fm_memory m INNER JOIN fm_family f ON f.id = m.family_id AND f.deleted = 0
  WHERE m.deleted = 0
  UNION ALL
  SELECT 'CAPSULE', c.id
  FROM fm_time_capsule c INNER JOIN fm_family f ON f.id = c.family_id AND f.deleted = 0
  WHERE c.deleted = 0
)
SELECT i.id, i.family_id, i.source_type, i.source_id, i.node_date
FROM fm_universe_index i
LEFT JOIN expected e ON e.source_type = i.source_type AND e.source_id = i.source_id
WHERE i.deleted = 0 AND e.source_id IS NULL;

-- 4. 检查按天分开的两类数量。正式接口必须在校验家庭成员权限后限定 family_id；
-- 此处为了迁移核对，列出所有家庭的统计，不返回标题或正文。
SELECT family_id, YEAR(node_date) AS year, MONTH(node_date) AS month, DAY(node_date) AS day,
       SUM(source_type = 'MEMORY') AS memory_count,
       SUM(source_type = 'CAPSULE') AS capsule_count
FROM fm_universe_index
WHERE deleted = 0
GROUP BY family_id, node_date
ORDER BY family_id, node_date DESC;
