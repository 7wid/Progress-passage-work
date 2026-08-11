-- MySQL DDL may already have been applied when a previous Flyway run was interrupted.
-- Check every object separately so this migration can safely finish from a partial state.

SET @assignment_check_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'request_member'
      AND CONSTRAINT_NAME = 'chk_request_member_type'
);
SET @assignment_sql = IF(
    @assignment_check_exists = 0,
    'ALTER TABLE request_member ADD CONSTRAINT chk_request_member_type CHECK (member_type IN (''OWNER'', ''PARTICIPANT''))',
    'SELECT 1'
);
PREPARE assignment_statement FROM @assignment_sql;
EXECUTE assignment_statement;
DEALLOCATE PREPARE assignment_statement;

SET @owner_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'request_member'
      AND COLUMN_NAME = 'owner_request_id'
);
SET @assignment_sql = IF(
    @owner_column_exists = 0,
    'ALTER TABLE request_member ADD COLUMN owner_request_id BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN member_type = ''OWNER'' THEN request_id ELSE NULL END) STORED',
    'SELECT 1'
);
PREPARE assignment_statement FROM @assignment_sql;
EXECUTE assignment_statement;
DEALLOCATE PREPARE assignment_statement;

SET @owner_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'request_member'
      AND INDEX_NAME = 'uk_request_member_single_owner'
);
SET @assignment_sql = IF(
    @owner_index_exists = 0,
    'ALTER TABLE request_member ADD UNIQUE KEY uk_request_member_single_owner (owner_request_id)',
    'SELECT 1'
);
PREPARE assignment_statement FROM @assignment_sql;
EXECUTE assignment_statement;
DEALLOCATE PREPARE assignment_statement;
