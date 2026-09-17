-- V2__membership.sql
-- Agent 2 - Membership & Package Module
-- Ban toi thieu de Check-in Engine (V3) co the hoat dong duoc.
-- Cac truong/nghiep vu con lai (freeze_history, upgrade...) se bo sung sau boi Agent 2.

CREATE TABLE packages (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    name NVARCHAR(150) NOT NULL,
    description NVARCHAR(500) NULL,
    price DECIMAL(12,2) NOT NULL,
    duration_days INT NULL,
    session_count INT NULL,
    package_type VARCHAR(20) NOT NULL, -- TIME_BASED, SESSION_BASED, PT_1ON1
    peak_type VARCHAR(20) NOT NULL DEFAULT 'FULL_TIME', -- OFF_PEAK, FULL_TIME
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL
);

CREATE TABLE member_packages (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    user_id UNIQUEIDENTIFIER NOT NULL,
    package_id UNIQUEIDENTIFIER NOT NULL,
    branch_id UNIQUEIDENTIFIER NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    remaining_sessions INT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, EXPIRED, FROZEN, PENDING
    total_frozen_days INT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT fk_member_packages_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_member_packages_package FOREIGN KEY (package_id) REFERENCES packages(id),
    CONSTRAINT fk_member_packages_branch FOREIGN KEY (branch_id) REFERENCES branches(id)
);

CREATE INDEX idx_member_packages_user ON member_packages(user_id);
CREATE INDEX idx_member_packages_status ON member_packages(status);
