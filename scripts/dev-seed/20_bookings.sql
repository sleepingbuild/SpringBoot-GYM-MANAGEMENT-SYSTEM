-- 20_bookings.sql (Agent 3): lich lam viec PT va booking mau o nhieu trang thai.
-- CHI DUNG CHO DATABASE DEV. Khong nam trong Flyway, chay thu cong, chay lai nhieu lan khong loi.
-- Thu tu chay: sau 00_users.sql (Agent 1) va 10_packages_memberships.sql (Agent 2).
--
-- Quy tac:
--   * Tham chieu user theo email, khong ghi cung UUID.
--   * Ngay gio TUONG DOI theo gio Viet Nam (may chu SQL trong Docker chay UTC).
--   * Booking mau danh dau notes = N'[dev-seed]'; chay lai se xoa roi tao lai CHINH cac dong do.
--   * Chay ban ghi dem theo gio that, nen booking "sap dien ra" va "da quet mat" luon dung moc tuong doi.
--
-- Luu y: neu chay trong khoang 22:30-24:00 gio Viet Nam, gio ket thuc cua booking "sap dien ra"
-- se qua nua dem (end_time nho hon start_time). Chay vao gio khac, hoac bo qua booking do.

DECLARE @now DATETIME2 = CAST(SYSUTCDATETIME() AT TIME ZONE 'UTC' AT TIME ZONE 'SE Asia Standard Time' AS DATETIME2);
DECLARE @today DATE = CAST(@now AS DATE);
DECLARE @yesterday DATE = DATEADD(DAY, -1, @today);
DECLARE @tomorrow DATE = DATEADD(DAY, 1, @today);

DECLARE @trainer1 UNIQUEIDENTIFIER = (SELECT id FROM users WHERE email = 'trainer1@gym.test');
DECLARE @trainer2 UNIQUEIDENTIFIER = (SELECT id FROM users WHERE email = 'trainer2@gym.test');
DECLARE @memberActive UNIQUEIDENTIFIER = (SELECT id FROM users WHERE email = 'member.active@gym.test');
DECLARE @memberExpired UNIQUEIDENTIFIER = (SELECT id FROM users WHERE email = 'member.expired@gym.test');

IF @trainer1 IS NULL OR @trainer2 IS NULL OR @memberActive IS NULL OR @memberExpired IS NULL
BEGIN
    PRINT 'THIEU USER: can trainer1, trainer2, member.active, member.expired @gym.test. Hay chay 00_users.sql truoc.';
    RETURN;
END

-- 1) Lich lam viec: moi PT lam 06:00-22:00 trong 8 ngay (hom nay den 7 ngay sau), chi them neu chua co.
DECLARE @trainers TABLE (id UNIQUEIDENTIFIER);
INSERT INTO @trainers (id) VALUES (@trainer1), (@trainer2);

;WITH n AS (
    SELECT TOP (8) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) - 1 AS d FROM sys.all_objects
)
INSERT INTO trainer_schedules (trainer_id, work_date, start_time, end_time, is_active)
SELECT t.id, DATEADD(DAY, n.d, @today), '06:00', '22:00', 1
FROM @trainers t
CROSS JOIN n
WHERE NOT EXISTS (
    SELECT 1 FROM trainer_schedules s
    WHERE s.trainer_id = t.id AND s.work_date = DATEADD(DAY, n.d, @today)
);

-- 2) Booking mau: xoa cac dong seed cu roi tao lai.
DELETE FROM bookings WHERE notes = N'[dev-seed]';

-- Moc thoi gian tuong doi
DECLARE @soon DATETIME2 = DATEADD(MINUTE, 60, @now);          -- CONFIRMED sap dien ra (de thu huy muon: con duoi 2 gio)
DECLARE @ready DATETIME2 = DATEADD(MINUTE, -70, @now);        -- CONFIRMED da hoc xong, da quet mat (de thu "Hoan thanh")

INSERT INTO bookings
    (member_id, trainer_id, booking_date, start_time, end_time, status,
     check_in_time, check_in_method, check_out_time, check_out_method,
     pt_check_in_time, pt_check_in_method, pt_check_out_time, pt_check_out_method, notes)
VALUES
-- PENDING, ngay mai 09:00-10:00
(@memberActive, @trainer1, @tomorrow, '09:00', '10:00', 'PENDING',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, N'[dev-seed]'),

-- CANCELLED, ngay mai 11:00-12:00
(@memberActive, @trainer1, @tomorrow, '11:00', '12:00', 'CANCELLED',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, N'[dev-seed]'),

-- CONFIRMED sap dien ra (+60 phut), chua quet mat: thu huy muon
(@memberActive, @trainer2, CAST(@soon AS DATE), CAST(@soon AS TIME(0)), CAST(DATEADD(MINUTE, 60, @soon) AS TIME(0)), 'CONFIRMED',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, N'[dev-seed]'),

-- CONFIRMED da hoc xong, hoc vien va PT deu da quet vao, PT quet ra sau gio ket thuc: thu "Hoan thanh"
(@memberActive, @trainer1, CAST(@ready AS DATE), CAST(@ready AS TIME(0)), CAST(DATEADD(MINUTE, 60, @ready) AS TIME(0)), 'CONFIRMED',
 DATEADD(MINUTE, 2, @ready), 'FACE', NULL, NULL,
 @ready, 'FACE', DATEADD(MINUTE, 65, @ready), 'FACE', N'[dev-seed]'),

-- COMPLETED, hom qua 08:00-09:00
(@memberActive, @trainer1, @yesterday, '08:00', '09:00', 'COMPLETED',
 DATEADD(MINUTE, 480 + 2, CAST(@yesterday AS DATETIME2)), 'FACE', DATEADD(MINUTE, 480 + 58, CAST(@yesterday AS DATETIME2)), 'FACE',
 DATEADD(MINUTE, 480, CAST(@yesterday AS DATETIME2)), 'FACE', DATEADD(MINUTE, 480 + 60, CAST(@yesterday AS DATETIME2)), 'FACE', N'[dev-seed]'),

-- LATE_CANCELLED, hom qua 10:00-11:00
(@memberActive, @trainer2, @yesterday, '10:00', '11:00', 'LATE_CANCELLED',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, N'[dev-seed]'),

-- NO_SHOW (hoc vien vang, PT da quet vao), hom qua 14:00-15:00
(@memberExpired, @trainer1, @yesterday, '14:00', '15:00', 'NO_SHOW',
 NULL, NULL, NULL, NULL,
 DATEADD(MINUTE, 14 * 60, CAST(@yesterday AS DATETIME2)), 'FACE', NULL, NULL, N'[dev-seed]'),

-- PT_NO_SHOW (PT vang), hom qua 16:00-17:00
(@memberActive, @trainer2, @yesterday, '16:00', '17:00', 'PT_NO_SHOW',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, N'[dev-seed]');

PRINT 'Da tao lich lam viec (8 ngay, 2 PT) va 8 booking mau.';
