CREATE TABLE third_party_api_audits
(
    id                  BIGSERIAL PRIMARY KEY,
    api_name            VARCHAR(50) NOT NULL,
    api_type            VARCHAR(20) NOT NULL,
    object_id           BIGINT,
    object_name         VARCHAR(20),
    http_response_code  VARCHAR(3),
    request_params      TEXT        NOT NULL,
    response            TEXT        NOT NULL,
    is_success          BOOLEAN     NOT NULL,
    created_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);