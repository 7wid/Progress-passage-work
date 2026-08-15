CREATE INDEX idx_request_submitted_at
    ON tech_request (submitted_at);

CREATE INDEX idx_request_category_submitted
    ON tech_request (category_id, submitted_at);
