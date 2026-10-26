-- CH2: lượt vào phòng tập của hội viên mà KHÔNG gắn với buổi PT.
-- Lượt có booking phù hợp chỉ cập nhật bookings (tránh đếm hai lần).
CREATE TABLE gym_check_ins (
    id             UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    member_id      UNIQUEIDENTIFIER NOT NULL,
    branch_id      UNIQUEIDENTIFIER NULL,
    check_in_time  DATETIME2        NOT NULL,
    check_out_time DATETIME2        NULL,
    method         VARCHAR(20)      NOT NULL DEFAULT 'FACE',
    created_at     DATETIME2        NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at     DATETIME2        NULL,
    CONSTRAINT fk_gym_check_ins_member FOREIGN KEY (member_id) REFERENCES users(id),
    CONSTRAINT fk_gym_check_ins_branch FOREIGN KEY (branch_id) REFERENCES branches(id)
);

CREATE INDEX idx_gym_check_ins_member_time ON gym_check_ins(member_id, check_in_time);
