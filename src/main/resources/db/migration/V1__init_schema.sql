-- V1__init_schema.sql
-- Agent 1 - Base & Security
-- Bảng nền tảng: roles, users, user_roles, branches

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(20) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    avatar_url VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE branches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    address VARCHAR(255),
    phone VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);

-- Seed 5 role chuẩn của hệ thống (RoleName enum phải khớp chính xác các giá trị này)
INSERT INTO roles (name, description) VALUES
    ('SUPER_ADMIN', 'Chủ phòng gym - quản lý toàn hệ thống'),
    ('RECEPTIONIST', 'Lễ tân - check-in, bán gói, POS'),
    ('TRAINER', 'Huấn luyện viên - quản lý lịch dạy, khách hàng PT'),
    ('SALES', 'Sales/CSKH - quản lý Leads, hoa hồng bán gói'),
    ('MEMBER', 'Hội viên - xem gói tập, đặt lịch PT/Group X');

-- Seed 1 chi nhánh mặc định để các agent khác có branch_id dùng ngay khi phát triển
INSERT INTO branches (name, address, phone, status) VALUES
    ('Chi nhánh Trung tâm', 'Đang cập nhật', 'Đang cập nhật', 'ACTIVE');
