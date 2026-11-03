-- 60_leads_reports.sql — Agent 6: lead mẫu và dữ liệu mẫu cho 4 báo cáo.
-- Chạy THỦ CÔNG trên database dev (không phải migration, không chạy trên CI). Chạy lại nhiều lần được:
-- đầu script xoá đúng các dòng seed của chính nó (lead có ghi chú bắt đầu bằng [seed], user seed.*@test.local,
-- giao dịch SEED-*, gói "Seed ..."), rồi nạp lại.
-- Tài khoản tham chiếu theo email: sales@gym.test (SALES) là người được gán lead; nếu chưa có thì lead để chưa gán.
-- Ngày giờ tính tương đối theo giờ Việt Nam (máy chủ SQL trong Docker chạy UTC).
-- Tương thích V7 (CH1): tự nhận biết cột payment_transactions; sau V7 chỉ nạp giao dịch MEMBERSHIP
-- (giao dịch POS cần đơn pos_orders thật của V5 nên bị bỏ qua và báo ra màn hình).
SET NOCOUNT ON;
-- Bắt buộc: bảng users có chỉ mục lọc (filtered index) từ V9, sqlcmd mặc định để QUOTED_IDENTIFIER OFF nên DELETE/INSERT sẽ lỗi.
SET QUOTED_IDENTIFIER ON;

DECLARE @nowVN DATETIME2 = CAST(SYSUTCDATETIME() AT TIME ZONE 'UTC' AT TIME ZONE 'SE Asia Standard Time' AS DATETIME2);
DECLARE @today DATE = CAST(@nowVN AS DATE);

-- 0. Dọn seed cũ (thứ tự theo khoá ngoại)
DELETE FROM leads WHERE notes LIKE '[[]seed]%';
IF OBJECT_ID('bookings', 'U') IS NOT NULL
    EXEC sp_executesql N'DELETE FROM bookings WHERE member_id IN (SELECT id FROM users WHERE email LIKE ''seed.%@test.local'')';
DELETE FROM payment_transactions WHERE transaction_code LIKE 'SEED-%';
DELETE FROM member_packages WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'seed.%@test.local');
DELETE FROM packages WHERE name LIKE 'Seed %';
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'seed.%@test.local');
DELETE FROM users WHERE email LIKE 'seed.%@test.local';

-- 1. Người dùng seed (mật khẩu giả, không đăng nhập được)
INSERT INTO users (id, full_name, email, phone, password_hash, status) VALUES
 ('A6000000-0000-0000-0000-000000000001', N'Seed Member 1', 'seed.m1@test.local', '0900000001', 'seed-not-loginable', 'ACTIVE'),
 ('A6000000-0000-0000-0000-000000000002', N'Seed Member 2', 'seed.m2@test.local', '0900000002', 'seed-not-loginable', 'ACTIVE'),
 ('A6000000-0000-0000-0000-000000000003', N'Seed Member 3', 'seed.m3@test.local', '0900000003', 'seed-not-loginable', 'ACTIVE'),
 ('A6000000-0000-0000-0000-000000000004', N'Seed Member 4', 'seed.m4@test.local', '0900000004', 'seed-not-loginable', 'ACTIVE'),
 ('A6000000-0000-0000-0000-000000000005', N'Seed Member 5', 'seed.m5@test.local', '0900000005', 'seed-not-loginable', 'ACTIVE'),
 ('A6000000-0000-0000-0000-000000000006', N'Seed Member 6', 'seed.m6@test.local', '0900000006', 'seed-not-loginable', 'ACTIVE'),
 ('A6000000-0000-0000-0000-000000000007', N'Seed Trainer', 'seed.trainer@test.local', '0900000007', 'seed-not-loginable', 'ACTIVE');

-- 2. Gói tập
INSERT INTO packages (id, name, price, duration_days, package_type) VALUES
 ('A6000000-0000-0000-0000-0000000000B1', 'Seed Goi 1 thang', 500000, 30, 'TIME_BASED'),
 ('A6000000-0000-0000-0000-0000000000B3', 'Seed Goi 3 thang', 1200000, 90, 'TIME_BASED');

-- 3. Đăng ký gói (s, e = số ngày so với hôm nay)
INSERT INTO member_packages (id, user_id, package_id, status, start_date, end_date)
SELECT v.id, v.user_id, v.package_id, v.status, DATEADD(DAY, v.s, @today), DATEADD(DAY, v.e, @today)
FROM (VALUES
 (CAST('A6000000-0000-0000-0000-0000000000C1' AS UNIQUEIDENTIFIER), CAST('A6000000-0000-0000-0000-000000000001' AS UNIQUEIDENTIFIER), CAST('A6000000-0000-0000-0000-0000000000B1' AS UNIQUEIDENTIFIER), 'EXPIRED',   -50, -21),
 ('A6000000-0000-0000-0000-0000000000C2', 'A6000000-0000-0000-0000-000000000001', 'A6000000-0000-0000-0000-0000000000B3', 'ACTIVE',    -20,  69),
 ('A6000000-0000-0000-0000-0000000000C3', 'A6000000-0000-0000-0000-000000000002', 'A6000000-0000-0000-0000-0000000000B1', 'EXPIRED',   -45, -16),
 ('A6000000-0000-0000-0000-0000000000C4', 'A6000000-0000-0000-0000-000000000003', 'A6000000-0000-0000-0000-0000000000B1', 'EXPIRED',   -40, -11),
 ('A6000000-0000-0000-0000-0000000000C5', 'A6000000-0000-0000-0000-000000000003', 'A6000000-0000-0000-0000-0000000000B1', 'ACTIVE',     -1,  28),
 ('A6000000-0000-0000-0000-0000000000C6', 'A6000000-0000-0000-0000-000000000004', 'A6000000-0000-0000-0000-0000000000B1', 'ACTIVE',    -35,  -5),
 ('A6000000-0000-0000-0000-0000000000C7', 'A6000000-0000-0000-0000-000000000005', 'A6000000-0000-0000-0000-0000000000B1', 'CANCELLED', -40, -10),
 ('A6000000-0000-0000-0000-0000000000C8', 'A6000000-0000-0000-0000-000000000006', 'A6000000-0000-0000-0000-0000000000B1', 'PENDING',   -30,  -1)
) AS v(id, user_id, package_id, status, s, e);

-- 4. Giao dịch thanh toán (nạp vào bảng tạm rồi chèn theo cấu trúc bảng hiện tại)
CREATE TABLE #pay (user_id UNIQUEIDENTIFIER, kind VARCHAR(20), related_id UNIQUEIDENTIFIER,
                   amount DECIMAL(12,2), status VARCHAR(20), code VARCHAR(100), d INT);
INSERT INTO #pay (user_id, kind, related_id, amount, status, code, d) VALUES
 ('A6000000-0000-0000-0000-000000000001', 'MEMBERSHIP', 'A6000000-0000-0000-0000-0000000000C1',  500000, 'SUCCESS', 'SEED-001', -50),
 ('A6000000-0000-0000-0000-000000000001', 'MEMBERSHIP', 'A6000000-0000-0000-0000-0000000000C2', 1200000, 'SUCCESS', 'SEED-002', -20),
 ('A6000000-0000-0000-0000-000000000002', 'MEMBERSHIP', 'A6000000-0000-0000-0000-0000000000C3',  500000, 'SUCCESS', 'SEED-003', -45),
 ('A6000000-0000-0000-0000-000000000003', 'MEMBERSHIP', 'A6000000-0000-0000-0000-0000000000C4',  500000, 'SUCCESS', 'SEED-004', -40),
 ('A6000000-0000-0000-0000-000000000003', 'MEMBERSHIP', 'A6000000-0000-0000-0000-0000000000C5',  500000, 'SUCCESS', 'SEED-005',  -1),
 ('A6000000-0000-0000-0000-000000000004', 'MEMBERSHIP', 'A6000000-0000-0000-0000-0000000000C6',  500000, 'SUCCESS', 'SEED-006', -35),
 ('A6000000-0000-0000-0000-000000000005', 'MEMBERSHIP', 'A6000000-0000-0000-0000-0000000000C7',  500000, 'FAILED',  'SEED-007', -40),
 ('A6000000-0000-0000-0000-000000000006', 'MEMBERSHIP', 'A6000000-0000-0000-0000-0000000000C8',  500000, 'PENDING', 'SEED-008', -30),
 ('A6000000-0000-0000-0000-000000000001', 'POS',        'A6000000-0000-0000-0000-0000000000D1',   45000, 'SUCCESS', 'SEED-009',  -3),
 ('A6000000-0000-0000-0000-000000000002', 'POS',        'A6000000-0000-0000-0000-0000000000D2',   30000, 'SUCCESS', 'SEED-010',  -3),
 ('A6000000-0000-0000-0000-000000000001', 'POS',        'A6000000-0000-0000-0000-0000000000D3',   20000, 'FAILED',  'SEED-011',  -3);

IF COL_LENGTH('payment_transactions', 'member_package_id') IS NULL
BEGIN
    -- Cấu trúc hiện tại (related_type, related_id)
    EXEC sp_executesql N'
        INSERT INTO payment_transactions (id, user_id, related_type, related_id, amount, payment_method, status, transaction_code, created_at, updated_at)
        SELECT NEWID(), user_id, kind, related_id, amount, ''CASH'', status, code,
               DATEADD(HOUR, 10, CAST(DATEADD(DAY, d, @t) AS DATETIME2)),
               DATEADD(HOUR, 10, CAST(DATEADD(DAY, d, @t) AS DATETIME2))
        FROM #pay', N'@t DATE', @t = @today;
END
ELSE
BEGIN
    -- Sau V7 (CH1): chỉ giao dịch gói tập; POS cần pos_orders thật nên bỏ qua
    EXEC sp_executesql N'
        INSERT INTO payment_transactions (id, user_id, member_package_id, amount, payment_method, status, transaction_code, created_at, updated_at)
        SELECT NEWID(), user_id, related_id, amount, ''CASH'', status, code,
               DATEADD(HOUR, 10, CAST(DATEADD(DAY, d, @t) AS DATETIME2)),
               DATEADD(HOUR, 10, CAST(DATEADD(DAY, d, @t) AS DATETIME2))
        FROM #pay WHERE kind = ''MEMBERSHIP''', N'@t DATE', @t = @today;
    PRINT 'Sau V7: bo qua 3 giao dich POS mau (can pos_orders that). posTotal bao cao se la 0 cho den khi co don POS that.';
END
DROP TABLE #pay;

-- 5. Booking có check-in (cần bảng bookings của V3)
IF OBJECT_ID('bookings', 'U') IS NOT NULL
BEGIN
    EXEC sp_executesql N'
        INSERT INTO bookings (member_id, trainer_id, booking_date, start_time, end_time, status, check_in_time, check_in_method)
        SELECT v.member_id, ''A6000000-0000-0000-0000-000000000007'', DATEADD(DAY, v.d, @t), ''08:00'', ''09:00'', v.status,
               CASE WHEN v.mins IS NULL THEN NULL ELSE DATEADD(MINUTE, v.mins, CAST(DATEADD(DAY, v.d, @t) AS DATETIME2)) END,
               CASE WHEN v.mins IS NULL THEN NULL ELSE ''FACE'' END
        FROM (VALUES
            (CAST(''A6000000-0000-0000-0000-000000000001'' AS UNIQUEIDENTIFIER), -2, ''COMPLETED'', 450),
            (''A6000000-0000-0000-0000-000000000002'', -2, ''COMPLETED'', 470),
            (''A6000000-0000-0000-0000-000000000001'', -1, ''COMPLETED'', 1090),
            (''A6000000-0000-0000-0000-000000000003'', -1, ''CONFIRMED'', NULL),
            (''A6000000-0000-0000-0000-000000000004'', -1, ''CANCELLED'', 1120)
        ) AS v(member_id, d, status, mins)', N'@t DATE', @t = @today;
END

-- 6. Lead mẫu (gán cho sales@gym.test nếu tài khoản đã có; lead số 6 đã chuyển thành hội viên seed.m1)
DECLARE @salesId UNIQUEIDENTIFIER = (SELECT TOP 1 id FROM users WHERE email = 'sales@gym.test');
DECLARE @convertedId UNIQUEIDENTIFIER = (SELECT TOP 1 id FROM users WHERE email = 'seed.m1@test.local');

INSERT INTO leads (id, full_name, phone, source, status, assigned_sales_id, converted_user_id, notes, created_at)
SELECT NEWID(), v.full_name, v.phone, v.source, v.status,
       CASE WHEN v.assigned = 1 THEN @salesId END,
       CASE WHEN v.converted = 1 THEN @convertedId END,
       v.notes, DATEADD(DAY, -v.d, @nowVN)
FROM (VALUES
 (N'Nguyen Van An',        '0987000001', N'Facebook',  'NEW',       0, 0, N'[seed] Quan tam goi 3 thang',         1),
 (N'Tran Thi Binh',        '0987000002', N'Zalo',      'NEW',       1, 0, N'[seed] Hoi gia goi 1 thang',          2),
 (N'Le Van Cuong',         '0987000003', N'Gioi thieu',N'CONTACTED',1, 0, N'[seed] Hen toi tham quan phong tap',  4),
 (N'Pham Thi Dung',        '0987000004', N'Website',   'CONTACTED', 1, 0, N'[seed] Can PT ca nhan',               5),
 (N'Hoang Van Em',         '0987000005', N'Facebook',  'LOST',      1, 0, N'[seed] Chuyen sang phong khac',       9),
 (N'Vu Thi Phuong',        '0987000006', N'Walk-in',   'CONVERTED', 1, 1, N'[seed] Da dang ky goi 3 thang',       12),
 (N'Dang Van Giang',       '0987000007', N'Zalo',      'NEW',       0, 0, N'[seed] Chua lien he',                 0),
 (N'Nguyen Van An (trung)','0987000001', N'Website',   'NEW',       1, 0, N'[seed] Trung so voi lead dau tien',   3)
) AS v(full_name, phone, source, status, assigned, converted, notes, d);

PRINT 'Seed 60_leads_reports OK. Hom nay (gio VN) = ' + CONVERT(VARCHAR(10), @today, 23);
SELECT 'leads' AS tbl, COUNT(*) AS n FROM leads WHERE notes LIKE '[[]seed]%'
UNION ALL SELECT 'users', COUNT(*) FROM users WHERE email LIKE 'seed.%@test.local'
UNION ALL SELECT 'member_packages', COUNT(*) FROM member_packages WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'seed.%@test.local')
UNION ALL SELECT 'payment_transactions', COUNT(*) FROM payment_transactions WHERE transaction_code LIKE 'SEED-%';
