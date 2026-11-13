-- 41_pt_live_session.sql (Agent 4): mot buoi PT DANG DIEN RA (PT da quet vao, chua quet ra) de thu man hinh PT.
-- CHI DUNG CHO DATABASE DEV. Khong nam trong Flyway, chay thu cong, chay lai nhieu lan khong loi.
-- Thu tu chay: sau 00_users.sql (Agent 1) va 40_attendance.sql (Agent 4, can member04@gym.test).
--
-- Tao 1 booking CONFIRMED hom nay, o khung gio chan hien tai (gio Viet Nam):
--   * PT: trainer2@gym.test.  Hoi vien: member04@gym.test (khong co luot vao phong tap hom nay trong 40_attendance.sql,
--     nen khong bi dem hai lan).
--   * PT va hoi vien da quet vao, CHUA ai quet ra: pt_check_out_time va check_out_time de trong.
-- Khong dung member.active va trainer1 vi 20_bookings.sql (Agent 3) da co buoi "da hoc xong" cua ho; dat them o day
-- se tao hai buoi chong gio cua cung mot nguoi va lam kiosk chon nham buoi khi quet.
--
-- Quy tac:
--   * Dong seed co notes = [dev-seed-a4] (khac [dev-seed] cua 20_bookings.sql nen hai file khong xoa nham nhau).
--   * Chay lai: xoa dong seed cu CHUA co hoa hong roi tao lai. Dong da co hoa hong (buoi da hoan thanh) duoc giu,
--     khong gay loi fk_commissions_booking.
--   * Chi chay trong khung 06:00-21:59 gio Viet Nam (khung gio dat lich la 06:00-22:00). Ngoai khung thi bo qua.
--   * Hoi vien member04 khong co goi tap (goi thuoc Agent 2). Muon thu nut "Hoan thanh" (tru buoi cua goi) thi dung
--     buoi "da hoc xong" cua member.active trong 20_bookings.sql; buoi o day dung de thu hien thi gio vao/ra cua PT va quet ra.

SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;

DECLARE @now DATETIME2 = CAST(SYSUTCDATETIME() AT TIME ZONE 'UTC' AT TIME ZONE 'SE Asia Standard Time' AS DATETIME2);
DECLARE @today DATE = CAST(@now AS DATE);
DECLARE @midnight DATETIME2 = CAST(@today AS DATETIME2);
DECLARE @hour INT = DATEPART(HOUR, @now);

DECLARE @trainer UNIQUEIDENTIFIER = (SELECT id FROM users WHERE email = 'trainer2@gym.test');
DECLARE @member UNIQUEIDENTIFIER = (SELECT id FROM users WHERE email = 'member04@gym.test');

IF @trainer IS NULL OR @member IS NULL
BEGIN
    PRINT 'THIEU USER: can trainer2@gym.test (00_users.sql) va member04@gym.test (40_attendance.sql). Hay chay hai file do truoc.';
    RETURN;
END

-- 1) Xoa dong seed cu chua co hoa hong
IF OBJECT_ID(N'dbo.commissions', N'U') IS NOT NULL
    DELETE b FROM bookings b
    WHERE b.notes = N'[dev-seed-a4]'
      AND NOT EXISTS (SELECT 1 FROM commissions c WHERE c.booking_id = b.id);
ELSE
    DELETE FROM bookings WHERE notes = N'[dev-seed-a4]';

IF @hour < 6 OR @hour > 21
BEGIN
    PRINT 'Ngoai khung 06:00-21:59 gio Viet Nam: khong tao buoi dang dien ra. Chay lai trong khung gio dat lich.';
    RETURN;
END

-- 2) Khung gio chan hien tai; PT vao sau gio bat dau 3 phut, hoi vien vao sau 4 phut (khong vuot qua thoi diem hien tai)
DECLARE @slotStart DATETIME2 = DATEADD(HOUR, @hour, @midnight);
DECLARE @minsIn INT = DATEDIFF(MINUTE, @slotStart, @now);
DECLARE @ptIn DATETIME2 = DATEADD(MINUTE, CASE WHEN @minsIn >= 4 THEN 3 ELSE 0 END, @slotStart);
DECLARE @memberIn DATETIME2 = DATEADD(MINUTE, CASE WHEN @minsIn >= 4 THEN 4 ELSE 0 END, @slotStart);

INSERT INTO bookings
    (member_id, trainer_id, booking_date, start_time, end_time, status,
     check_in_time, check_in_method, check_out_time, check_out_method,
     pt_check_in_time, pt_check_in_method, pt_check_out_time, pt_check_out_method, notes)
VALUES
(@member, @trainer, @today, CAST(@slotStart AS TIME(0)), CAST(DATEADD(MINUTE, 60, @slotStart) AS TIME(0)), 'CONFIRMED',
 @memberIn, 'FACE', NULL, NULL,
 @ptIn, 'FACE', NULL, NULL, N'[dev-seed-a4]');

-- 3) Ket qua
SELECT b.booking_date, b.start_time, b.end_time, b.status,
       t.email AS pt, m.email AS hoi_vien,
       b.pt_check_in_time, b.pt_check_out_time, b.check_in_time, b.check_out_time
FROM bookings b
JOIN users t ON t.id = b.trainer_id
JOIN users m ON m.id = b.member_id
WHERE b.notes = N'[dev-seed-a4]'
ORDER BY b.booking_date DESC, b.start_time DESC;

PRINT 'Xong. Thu: dang nhap trainer2@gym.test, xem buoi hien tai co gio PT vao va gio ra de trong; quet ra o /kiosk de thu ra som (can dang ky khuon mat cua trainer2 truoc).';
