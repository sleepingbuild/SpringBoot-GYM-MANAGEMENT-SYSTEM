-- V3__schedule_booking.sql
-- Agent 3 — Lịch làm việc PT & Booking Engine
-- Khớp DATABASE_SCHEMA.md mục "Agent 3". Không sửa lại sau khi đã merge vào main
-- (CONTRIBUTING.md mục 5) — mọi thay đổi sau này là migration MỚI (V3_1, ...).

CREATE TABLE trainer_schedules (
    id              UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    trainer_id      UNIQUEIDENTIFIER NOT NULL,
    work_date       DATE NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    is_active       BIT NOT NULL DEFAULT 1,
    created_at      DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_trainer_schedules_trainer FOREIGN KEY (trainer_id) REFERENCES users(id)
);

CREATE INDEX idx_trainer_schedule_trainer_date ON trainer_schedules (trainer_id, work_date);

CREATE TABLE bookings (
    id                  UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    member_id           UNIQUEIDENTIFIER NOT NULL,
    trainer_id          UNIQUEIDENTIFIER NOT NULL,
    branch_id           UNIQUEIDENTIFIER NULL,
    booking_date        DATE NOT NULL,
    start_time          TIME NOT NULL,
    end_time            TIME NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    check_in_time       DATETIME2 NULL,
    check_in_method     VARCHAR(20) NULL,
    check_out_time      DATETIME2 NULL,
    check_out_method    VARCHAR(20) NULL,
    notes               NVARCHAR(500) NULL,
    created_at          DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at          DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_bookings_member FOREIGN KEY (member_id) REFERENCES users(id),
    CONSTRAINT fk_bookings_trainer FOREIGN KEY (trainer_id) REFERENCES users(id),
    CONSTRAINT fk_bookings_branch FOREIGN KEY (branch_id) REFERENCES branches(id)
);

CREATE INDEX idx_booking_trainer_date ON bookings (trainer_id, booking_date);
CREATE INDEX idx_booking_member_date ON bookings (member_id, booking_date);

-- Không có UNIQUE constraint cứng cho (trainer_id, booking_date, start_time): booking đã
-- CANCELLED vẫn có thể tồn tại cùng slot với 1 booking active khác — logic "1 người/slot/PT"
-- được kiểm soát ở tầng service (BookingServiceImpl.validateBookingRules), không phải DB
-- constraint, để không chặn nhầm khi có bản ghi lịch sử đã huỷ (xem DATABASE_SCHEMA.md).
