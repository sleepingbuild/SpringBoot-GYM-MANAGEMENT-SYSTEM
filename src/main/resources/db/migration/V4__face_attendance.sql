CREATE TABLE face_profiles (
    id              UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    user_id         UNIQUEIDENTIFIER NOT NULL,
    descriptor      NVARCHAR(MAX)    NOT NULL,
    reference_image VARCHAR(255)     NULL,
    registered_by   UNIQUEIDENTIFIER NULL,
    created_at      DATETIME2        NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2        NULL,
    CONSTRAINT uq_face_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_face_profiles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_face_profiles_registered_by FOREIGN KEY (registered_by) REFERENCES users(id)
);

CREATE TABLE staff_attendances (
    id             UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    staff_id       UNIQUEIDENTIFIER NOT NULL,
    date           DATE             NOT NULL,
    check_in_time  DATETIME2        NULL,
    check_out_time DATETIME2        NULL,
    method         VARCHAR(20)      NOT NULL DEFAULT 'MANUAL',
    notes          NVARCHAR(255)    NULL,
    created_at     DATETIME2        NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_staff_attendances_staff_date UNIQUE (staff_id, date),
    CONSTRAINT fk_staff_attendances_staff FOREIGN KEY (staff_id) REFERENCES users(id)
);

CREATE INDEX idx_staff_attendances_date ON staff_attendances(date);
