ALTER TABLE tech_request
    MODIFY category_id BIGINT UNSIGNED NULL,
    MODIFY title VARCHAR(80) NULL,
    MODIFY background TEXT NULL,
    MODIFY description TEXT NULL,
    MODIFY expected_result TEXT NULL,
    MODIFY expected_deadline DATE NULL,
    MODIFY urgency VARCHAR(16) NULL,
    MODIFY contact_info VARCHAR(255) NULL;
