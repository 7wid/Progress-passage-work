-- Link every acceptance decision to the delivery that was actually reviewed.
-- Historical rows without a safely identifiable delivery remain NULL. New
-- application-created rows always set delivery_id.

SET @delivery_id_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'acceptance'
      AND COLUMN_NAME = 'delivery_id'
);
SET @delivery_migration_sql = IF(
    @delivery_id_column_exists = 0,
    'ALTER TABLE acceptance ADD COLUMN delivery_id BIGINT UNSIGNED NULL AFTER request_id',
    'SELECT 1'
);
PREPARE delivery_migration_statement FROM @delivery_migration_sql;
EXECUTE delivery_migration_statement;
DEALLOCATE PREPARE delivery_migration_statement;

-- A malformed historical data set can contain repeated decisions for one
-- delivery. Backfill only the newest matching decision so the unique index can
-- still be created; older ambiguous rows deliberately remain NULL.
DROP TEMPORARY TABLE IF EXISTS acceptance_delivery_backfill;
CREATE TEMPORARY TABLE acceptance_delivery_backfill AS
SELECT ranked.acceptance_id,
       ranked.delivery_id
FROM (
    SELECT candidate.acceptance_id,
           candidate.delivery_id,
           ROW_NUMBER() OVER (
               PARTITION BY candidate.delivery_id
               ORDER BY candidate.acceptance_created_at DESC,
                        candidate.acceptance_id DESC
           ) AS delivery_rank
    FROM (
        SELECT acceptance_row.id AS acceptance_id,
               acceptance_row.created_at AS acceptance_created_at,
               (
                   SELECT delivery_row.id
                   FROM delivery AS delivery_row
                   WHERE delivery_row.request_id = acceptance_row.request_id
                     AND delivery_row.created_at <= acceptance_row.created_at
                   ORDER BY delivery_row.created_at DESC,
                            delivery_row.id DESC
                   LIMIT 1
               ) AS delivery_id
        FROM acceptance AS acceptance_row
        WHERE acceptance_row.delivery_id IS NULL
    ) AS candidate
    WHERE candidate.delivery_id IS NOT NULL
) AS ranked
WHERE ranked.delivery_rank = 1;

UPDATE acceptance AS acceptance_row
JOIN acceptance_delivery_backfill AS backfill
  ON backfill.acceptance_id = acceptance_row.id
SET acceptance_row.delivery_id = backfill.delivery_id
WHERE acceptance_row.delivery_id IS NULL;

DROP TEMPORARY TABLE acceptance_delivery_backfill;

SET @delivery_unique_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'acceptance'
      AND INDEX_NAME = 'uk_acceptance_delivery'
);
SET @delivery_migration_sql = IF(
    @delivery_unique_index_exists = 0,
    'ALTER TABLE acceptance ADD UNIQUE KEY uk_acceptance_delivery (delivery_id)',
    'SELECT 1'
);
PREPARE delivery_migration_statement FROM @delivery_migration_sql;
EXECUTE delivery_migration_statement;
DEALLOCATE PREPARE delivery_migration_statement;

SET @delivery_foreign_key_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'acceptance'
      AND CONSTRAINT_NAME = 'fk_acceptance_delivery'
);
SET @delivery_migration_sql = IF(
    @delivery_foreign_key_exists = 0,
    'ALTER TABLE acceptance ADD CONSTRAINT fk_acceptance_delivery FOREIGN KEY (delivery_id) REFERENCES delivery (id)',
    'SELECT 1'
);
PREPARE delivery_migration_statement FROM @delivery_migration_sql;
EXECUTE delivery_migration_statement;
DEALLOCATE PREPARE delivery_migration_statement;
