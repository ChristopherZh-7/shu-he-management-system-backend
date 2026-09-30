-- Apply once before enabling shuhe.golish.enabled. Credentials are deployment env only.
CREATE TABLE IF NOT EXISTS project_golish_job (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ticket_id BIGINT NOT NULL,
  round_id BIGINT NOT NULL,
  source_job_id BIGINT DEFAULT NULL,
  kind VARCHAR(16) NOT NULL,
  request_key VARCHAR(160) NOT NULL,
  request_json LONGTEXT NOT NULL,
  remote_id VARCHAR(100) DEFAULT NULL,
  state VARCHAR(48) NOT NULL,
  error VARCHAR(1000) NOT NULL DEFAULT '',
  result_json LONGTEXT DEFAULT NULL,
  report_file VARCHAR(500) DEFAULT NULL,
  report_ready BIT NOT NULL DEFAULT b'0',
  imported BIT NOT NULL DEFAULT b'0',
  next_poll DATETIME NOT NULL,
  lease_token VARCHAR(36) NOT NULL DEFAULT '',
  lease_until DATETIME NOT NULL DEFAULT '1970-01-01',
  creator VARCHAR(64) DEFAULT '', create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) DEFAULT '', update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT NOT NULL DEFAULT b'0',
  UNIQUE KEY uk_golish_request(request_key),
  KEY idx_golish_ticket(ticket_id, id), KEY idx_golish_poll(imported, next_poll)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS project_golish_event (
  event_id VARCHAR(100) NOT NULL PRIMARY KEY,
  job_id BIGINT NOT NULL,
  received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS project_golish_finding (
  job_id BIGINT NOT NULL,
  finding_id BIGINT NOT NULL,
  vulnerability_id BIGINT NOT NULL,
  PRIMARY KEY(job_id, finding_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- Model-generated evidence can contain supplementary Unicode characters.
-- Preserve those in the existing business projections as well as the frozen JSON.
ALTER TABLE project_round CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE project_round_target CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE project_round_vulnerability CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
