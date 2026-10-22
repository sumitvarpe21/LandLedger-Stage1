ALTER TABLE ownership_history
    ADD COLUMN blockchain_status VARCHAR(24),
    ADD COLUMN blockchain_payload TEXT,
    ADD COLUMN blockchain_payload_hash VARCHAR(66),
    ADD COLUMN blockchain_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN blockchain_error VARCHAR(500);

ALTER TABLE ownership_history
    ADD CONSTRAINT ck_ownership_history_blockchain_status
        CHECK (blockchain_status IS NULL OR blockchain_status IN ('PENDING', 'CONFIRMED', 'FAILED'));

CREATE INDEX ix_ownership_history_blockchain_pending
    ON ownership_history (occurred_at)
    WHERE blockchain_status = 'PENDING';
