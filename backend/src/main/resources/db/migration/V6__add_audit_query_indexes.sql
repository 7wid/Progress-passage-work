ALTER TABLE audit_log
    ADD KEY idx_audit_created (created_at, id),
    ADD KEY idx_audit_action_created (action, created_at, id);
