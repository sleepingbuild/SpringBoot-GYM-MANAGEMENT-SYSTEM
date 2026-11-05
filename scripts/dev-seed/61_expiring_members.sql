-- 61_expiring_members.sql — Agent 6: hội viên mẫu để thử GET /leads/expiring-soon và tab "Hội viên sắp hết hạn".
-- Chạy THỦ CÔNG trên database dev SAU 60_leads_reports.sql (cần gói "Seed Goi 1 thang" do file 60 tạo).
-- Chạy lại nhiều lần được. File 60 khi chạy lại sẽ xoá luôn các hội viên seed.exp*@test.local của file này
-- (cùng mẫu email seed.%@test.local), khi đó chạy lại file 61.
-- Chỉ thêm gói ACTIVE còn hạn nên KHÔNG làm đổi số của các báo cáo doanh thu, gia hạn, top gói.
-- Ngày tính tương đối theo giờ Việt Nam (máy chủ SQL trong Docker chạy UTC).
SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;

DECLARE @nowVN DATETIME2 = CAST(SYSUTCDATETIME() AT TIME ZONE 'UTC' AT TIME ZONE 'SE Asia Standard Time' AS DATETIME2);
DECLARE @today DATE = CAST(@nowVN AS DATE);
DECLARE @pkg UNIQUEIDENTIFIER = (SELECT TOP 1 id FROM packages WHERE name = 'Seed Goi 1 thang');
IF @pkg IS NULL
BEGIN
    RAISERROR('Chua co goi "Seed Goi 1 thang". Hay chay 60_leads_reports.sql truoc.', 16, 1);
    RETURN;
END

-- 0. Dọn seed cũ của chính file này
DELETE FROM member_packages WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'seed.exp%@test.local');
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'seed.exp%@test.local');
DELETE FROM users WHERE email LIKE 'seed.exp%@test.local';

-- 1. Người dùng seed (mật khẩu giả, không đăng nhập được); Exp 6 là tài khoản không còn hoạt động
INSERT INTO users (id, full_name, email, phone, password_hash, status) VALUES
 ('A6100000-0000-0000-0000-000000000001', N'Seed Exp 1', 'seed.exp1@test.local', '0900000101', 'seed-not-loginable', 'ACTIVE'),
 ('A6100000-0000-0000-0000-000000000002', N'Seed Exp 2', 'seed.exp2@test.local', '0900000102', 'seed-not-loginable', 'ACTIVE'),
 ('A6100000-0000-0000-0000-000000000003', N'Seed Exp 3', 'seed.exp3@test.local', '0900000103', 'seed-not-loginable', 'ACTIVE'),
 ('A6100000-0000-0000-0000-000000000004', N'Seed Exp 4', 'seed.exp4@test.local', '0900000104', 'seed-not-loginable', 'ACTIVE'),
 ('A6100000-0000-0000-0000-000000000005', N'Seed Exp 5', 'seed.exp5@test.local', '0900000105', 'seed-not-loginable', 'ACTIVE'),
 ('A6100000-0000-0000-0000-000000000006', N'Seed Exp 6', 'seed.exp6@test.local', '0900000106', 'seed-not-loginable', 'INACTIVE');

-- 2. Gói tập (s, e = số ngày so với hôm nay; rem = số buổi còn lại, NULL = không giới hạn)
INSERT INTO member_packages (id, user_id, package_id, status, start_date, end_date, remaining_sessions)
SELECT v.id, v.user_id, @pkg, v.status, DATEADD(DAY, v.s, @today), DATEADD(DAY, v.e, @today), v.rem
FROM (VALUES
 (CAST('A6100000-0000-0000-0000-0000000000D1' AS UNIQUEIDENTIFIER), CAST('A6100000-0000-0000-0000-000000000001' AS UNIQUEIDENTIFIER), 'ACTIVE',     -29,  1, CAST(NULL AS INT)),
 ('A6100000-0000-0000-0000-0000000000D2', 'A6100000-0000-0000-0000-000000000002', 'ACTIVE',     -24,  6, 4),
 ('A6100000-0000-0000-0000-0000000000D3', 'A6100000-0000-0000-0000-000000000003', 'ACTIVE',     -20, 10, NULL),
 ('A6100000-0000-0000-0000-0000000000D4', 'A6100000-0000-0000-0000-000000000004', 'ACTIVE',      -5, 45, NULL),
 ('A6100000-0000-0000-0000-0000000000D5', 'A6100000-0000-0000-0000-000000000005', 'ACTIVE',     -27,  3, NULL),
 ('A6100000-0000-0000-0000-0000000000D6', 'A6100000-0000-0000-0000-000000000005', 'SCHEDULED',    4, 34, NULL),
 ('A6100000-0000-0000-0000-0000000000D7', 'A6100000-0000-0000-0000-000000000006', 'ACTIVE',     -28,  2, NULL)
) AS v(id, user_id, status, s, e, rem);

PRINT 'Seed 61_expiring_members OK. Hom nay (gio VN) = ' + CONVERT(VARCHAR(10), @today, 23);
PRINT 'Ky vong GET /leads/expiring-soon (sau khi da chay 60): days=7 -> 2 nguoi (Exp 1, Exp 2); days=14 -> 3 (them Exp 3);';
PRINT '  days=30 -> 4 (them Seed Member 3, het han +28); days=60 -> 5 (them Exp 4); days=90 -> 6 (them Seed Member 1, +69).';
PRINT '  Exp 5 (da co goi SCHEDULED ke tiep) va Exp 6 (tai khoan INACTIVE) khong bao gio xuat hien.';
SELECT u.email, mp.status, mp.end_date, DATEDIFF(DAY, @today, mp.end_date) AS days_left
FROM member_packages mp JOIN users u ON u.id = mp.user_id
WHERE u.email LIKE 'seed.exp%@test.local' ORDER BY u.email, mp.start_date;
