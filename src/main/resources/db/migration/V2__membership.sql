-- V2__membership.sql
-- Agent 2 - Membership, Package & Payment Module
-- Ban toi thieu (packages, member_packages) de cac module phu thuoc (Booking, Commission)
-- co the tham chieu duoc tu dau. Agent 2 mo rong CRUD/nghiep vu day du qua migration moi,
-- KHONG sua lai file nay sau khi da merge.

CREATE TABLE packages (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    name NVARCHAR(150) NOT NULL,
    description NVARCHAR(500) NULL,
    price DECIMAL(12,2) NOT NULL,
    duration_days INT NULL,
    session_count INT NULL,
    package_type VARCHAR(20) NOT NULL, -- TIME_BASED, SESSION_BASED, PT_1ON1
    peak_type VARCHAR(20) NOT NULL DEFAULT 'FULL_TIME', -- OFF_PEAK, FULL_TIME
    max_sessions_per_week INT NULL, -- NULL = khong gioi han, 0 = khong cho dat PT, N = toi da N buoi/tuan
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL
);

CREATE TABLE member_packages (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    user_id UNIQUEIDENTIFIER NOT NULL,
    package_id UNIQUEIDENTIFIER NOT NULL,
    branch_id UNIQUEIDENTIFIER NULL,
    -- PENDING (cho thanh toan) -> ACTIVE (start_date = luc thanh toan xong) -> EXPIRED (lazy-check)
    -- SCHEDULED danh cho ha cap (gio cu chay het han) -> tu chuyen ACTIVE dung ngay
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, ACTIVE, EXPIRED, SCHEDULED, CANCELLED
    start_date DATE NOT NULL,
    end_date DATE NULL,
    remaining_sessions INT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT fk_member_packages_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_member_packages_package FOREIGN KEY (package_id) REFERENCES packages(id),
    CONSTRAINT fk_member_packages_branch FOREIGN KEY (branch_id) REFERENCES branches(id)
);

CREATE INDEX idx_member_packages_user ON member_packages(user_id);
CREATE INDEX idx_member_packages_status ON member_packages(status);
