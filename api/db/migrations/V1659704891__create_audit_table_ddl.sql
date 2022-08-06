CREATE TABLE audits
(
    id          BIGSERIAL PRIMARY KEY,
    object_type VARCHAR(20) NOT NULL,
    object_id   BIGINT,
    action_name VARCHAR(20) NOT NULL,
    data        JSONB,
    performed_by  UUID, -- Can be null in case of system triggered actions
    performed_by_user_type VARCHAR(50),
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);