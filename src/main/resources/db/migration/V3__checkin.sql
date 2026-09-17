-- V3__checkin.sql
-- Agent 3 - Check-in Engine & Access Control
-- Gom ca check_ins va face_profiles (dang ky khuon mat truoc boi Admin/Le tan).

CREATE TABLE check_ins (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    user_id UNIQUEIDENTIFIER NOT NULL,
    member_package_id UNIQUEIDENTIFIER NULL, -- null neu bi tu choi truoc khi xac dinh duoc goi
    branch_id UNIQUEIDENTIFIER NULL,
    checkin_time DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    method VARCHAR(20) NOT NULL, -- QR, CARD, FACE
    status VARCHAR(20) NOT NULL, -- SUCCESS, DENIED_EXPIRED, DENIED_TIME, DENIED_NO_SESSION, DENIED_ANTI_PASSBACK, DENIED_FACE_NOT_RECOGNIZED
    device_id VARCHAR(100) NULL,
    CONSTRAINT fk_checkins_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_checkins_member_package FOREIGN KEY (member_package_id) REFERENCES member_packages(id),
    CONSTRAINT fk_checkins_branch FOREIGN KEY (branch_id) REFERENCES branches(id)
);

CREATE INDEX idx_checkins_user_time ON check_ins(user_id, checkin_time DESC);
CREATE INDEX idx_checkins_branch_time ON check_ins(branch_id, checkin_time DESC);

-- Anh khuon mat da dang ky truoc (boi Admin/Le tan) dung de doi chieu khi hoi vien/PT quet cam.
-- Giai doan Mock: luu them image_hash (SHA-256 cua anh) de MockFaceMatcher so khop chinh xac byte-for-byte.
-- Khi thay bang SDK/Cloud API that (Agent 3 giai doan sau), se bo sung cot face_embedding (NVARCHAR(MAX), JSON vector)
-- va MockFaceMatcher se duoc thay bang implementation goi API that, khong doi schema bang nay.
CREATE TABLE face_profiles (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    user_id UNIQUEIDENTIFIER NOT NULL UNIQUE,
    image VARBINARY(MAX) NOT NULL,
    image_hash VARCHAR(64) NOT NULL,
    registered_by UNIQUEIDENTIFIER NULL, -- user_id cua Admin/Le tan da upload anh
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT fk_face_profiles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_face_profiles_registered_by FOREIGN KEY (registered_by) REFERENCES users(id)
);

CREATE INDEX idx_face_profiles_hash ON face_profiles(image_hash);
