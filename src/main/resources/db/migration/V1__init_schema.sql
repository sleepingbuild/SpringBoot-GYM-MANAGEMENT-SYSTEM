-- V1__init_schema.sql
-- Agent 1 - Base & Security
-- Bang nen tang: roles, users, user_roles, branches
-- Cu phap T-SQL cho SQL Server (KHONG dung cu phap PostgreSQL nhu gen_random_uuid()/pgcrypto).
-- Text co dau tieng Viet dung NVARCHAR (+ tien to N truoc chuoi khi INSERT); ma/email/phone/status dung VARCHAR (ASCII).

CREATE TABLE roles (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description NVARCHAR(255) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL
);

CREATE TABLE users (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    full_name NVARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(20) NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    avatar_url VARCHAR(255) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL
);

CREATE TABLE user_roles (
    user_id UNIQUEIDENTIFIER NOT NULL,
    role_id UNIQUEIDENTIFIER NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE TABLE branches (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    name NVARCHAR(150) NOT NULL,
    address NVARCHAR(255) NULL,
    phone VARCHAR(20) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);

-- Seed 5 role chuan cua he thong (RoleName enum phai khop chinh xac cac gia tri nay)
INSERT INTO roles (name, description) VALUES
    ('SUPER_ADMIN', N'Chủ phòng gym - quản lý toàn hệ thống'),
    ('RECEPTIONIST', N'Lễ tân - check-in, bán gói, POS'),
    ('TRAINER', N'Huấn luyện viên - quản lý lịch dạy, khách hàng PT'),
    ('SALES', N'Sales/CSKH - quản lý Leads, hoa hồng bán gói'),
    ('MEMBER', N'Hội viên - xem gói tập, đặt lịch PT/Group X');

-- Seed 1 chi nhanh mac dinh de cac agent khac co branch_id dung ngay khi phat trien
INSERT INTO branches (name, address, phone, status) VALUES
    (N'Chi nhánh Trung tâm', N'Đang cập nhật', N'Đang cập nhật', 'ACTIVE');
