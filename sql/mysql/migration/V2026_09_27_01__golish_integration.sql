-- Apply once before enabling shuhe.golish.enabled. Immutable approved payloads
-- and verified originals are retained separately from editable ticket.ext_json.
CREATE TABLE IF NOT EXISTS shuhe_golish_document (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    sha256 CHAR(64) NOT NULL,
    content LONGBLOB NOT NULL,
    created_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS shuhe_golish_delivery (
    ticket_id BIGINT NOT NULL PRIMARY KEY,
    external_id VARCHAR(128) NOT NULL UNIQUE,
    payload LONGTEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(200) NOT NULL DEFAULT '',
    receipt LONGTEXT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at BIGINT NOT NULL,
    lease_until BIGINT NOT NULL DEFAULT 0,
    lease_token VARCHAR(36) NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_golish_delivery_due (next_attempt_at, lease_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
