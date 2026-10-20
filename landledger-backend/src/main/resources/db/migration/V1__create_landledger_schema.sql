CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(24) NOT NULL CHECK (role IN ('CITIZEN', 'LAND_OFFICER', 'ADMIN')),
    phone VARCHAR(40) NOT NULL DEFAULT '',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE UNIQUE INDEX uk_users_email_lower ON users (LOWER(email));

CREATE TABLE registration_requests (
    id UUID PRIMARY KEY,
    applicant_id UUID NOT NULL REFERENCES users(id),
    parcel_id VARCHAR(80) NOT NULL,
    survey_number VARCHAR(80) NOT NULL,
    village VARCHAR(120) NOT NULL,
    district VARCHAR(120) NOT NULL,
    state VARCHAR(120) NOT NULL,
    area DOUBLE PRECISION NOT NULL CHECK (area > 0),
    land_type VARCHAR(40) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    status VARCHAR(24) NOT NULL CHECK (status IN ('PENDING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED')),
    reviewed_by UUID REFERENCES users(id),
    review_remarks VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_registration_request_coordinates CHECK (
        (latitude IS NULL AND longitude IS NULL) OR (latitude IS NOT NULL AND longitude IS NOT NULL)
    ),
    CONSTRAINT ck_registration_request_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_registration_request_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);
CREATE INDEX ix_registration_requests_applicant_created ON registration_requests (applicant_id, created_at DESC);
CREATE INDEX ix_registration_requests_status_created ON registration_requests (status, created_at DESC);
CREATE UNIQUE INDEX uk_registration_requests_active_parcel
    ON registration_requests (LOWER(parcel_id))
    WHERE status IN ('PENDING', 'UNDER_REVIEW');

CREATE TABLE land_parcels (
    id UUID PRIMARY KEY,
    parcel_id VARCHAR(80) NOT NULL,
    survey_number VARCHAR(80) NOT NULL,
    village VARCHAR(120) NOT NULL,
    district VARCHAR(120) NOT NULL,
    state VARCHAR(120) NOT NULL,
    area DOUBLE PRECISION NOT NULL CHECK (area > 0),
    land_type VARCHAR(40) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    owner_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_land_parcels_parcel_id UNIQUE (parcel_id),
    CONSTRAINT ck_land_parcel_coordinates CHECK (
        (latitude IS NULL AND longitude IS NULL) OR (latitude IS NOT NULL AND longitude IS NOT NULL)
    ),
    CONSTRAINT ck_land_parcel_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_land_parcel_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);
CREATE INDEX ix_land_parcels_owner_id ON land_parcels (owner_id);
CREATE UNIQUE INDEX uk_land_parcels_parcel_id_lower ON land_parcels (LOWER(parcel_id));

CREATE TABLE ownership_transfers (
    id UUID PRIMARY KEY,
    parcel_id UUID NOT NULL REFERENCES land_parcels(id),
    from_owner_id UUID NOT NULL REFERENCES users(id),
    to_owner_id UUID NOT NULL REFERENCES users(id),
    requested_by UUID NOT NULL REFERENCES users(id),
    status VARCHAR(24) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    reason VARCHAR(1000),
    reviewed_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    decided_at TIMESTAMPTZ,
    CONSTRAINT ck_transfer_distinct_owners CHECK (from_owner_id <> to_owner_id)
);
CREATE INDEX ix_ownership_transfers_created ON ownership_transfers (created_at DESC);
CREATE INDEX ix_ownership_transfers_parties ON ownership_transfers (from_owner_id, to_owner_id);
CREATE UNIQUE INDEX uk_one_pending_transfer_per_parcel
    ON ownership_transfers (parcel_id)
    WHERE status = 'PENDING';

CREATE TABLE ownership_history (
    id UUID PRIMARY KEY,
    parcel_id UUID NOT NULL REFERENCES land_parcels(id),
    owner_id UUID NOT NULL REFERENCES users(id),
    event VARCHAR(500) NOT NULL,
    transaction_reference VARCHAR(200),
    occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_ownership_history_parcel_date ON ownership_history (parcel_id, occurred_at);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor VARCHAR(254) NOT NULL,
    action VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_audit_logs_created ON audit_logs (created_at DESC);

CREATE TABLE user_notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    text VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_user_notifications_user_date ON user_notifications (user_id, created_at DESC);
