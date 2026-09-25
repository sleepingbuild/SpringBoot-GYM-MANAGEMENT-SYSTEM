-- V2b__user_profiles.sql
-- Agent 1 - Module Ho so ca nhan
-- Ap dung cho Member va Trainer (tu xem/sua tuoi, can nang, chieu cao, muc tieu, avatar).

CREATE TABLE user_profiles (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    user_id UNIQUEIDENTIFIER NOT NULL UNIQUE,
    age INT NULL,
    weight_kg DECIMAL(5,2) NULL,
    height_cm DECIMAL(5,2) NULL,
    goal NVARCHAR(255) NULL,
    avatar_url VARCHAR(255) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
