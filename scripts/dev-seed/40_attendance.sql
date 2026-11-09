-- 40_attendance.sql (Agent 4): cham cong nhan su 30 ngay + luot vao phong tap cua hoi vien + them tai khoan thu.
-- CHI DUNG CHO DATABASE DEV. Khong nam trong Flyway, chay thu cong, chay lai nhieu lan khong loi.
-- Thu tu chay: sau 00_users.sql (Agent 1). Can backend da chay it nhat mot lan de Flyway tao bang (V4, V8).
-- Mat khau chung cua tai khoan moi: Gym@12345 (cung ma BCrypt voi 00_users.sql).
--
-- Quy tac:
--   * Tham chieu user theo email, khong ghi cung UUID. Ngay gio TUONG DOI theo gio Viet Nam.
--   * staff_attendances: dong seed co notes bat dau bang [dev-seed]; chay lai se xoa roi tao lai CHINH cac dong do.
--     Neu da co dong that cua (nhan su, ngay) thi KHONG ghi de.
--   * gym_check_ins khong co cot ghi chu nen danh dau bang chu ky thoi gian: dong seed co phan le cua giay la
--     dung .777 (777000000 nano giay). Chay lai chi xoa cac dong co chu ky nay, khong dong vao luot quet that.
--   * Trang thai chap cong theo StaffAttendanceServiceImpl.computeStatus: ca 07:00-21:00 (application.yml), khong co
--     phut an han. Muon = vao SAU 07:00:00; ve som = ra TRUOC 21:00:00; ABSENT = dong co check_in_time rong.
--   * Hoi vien moi KHONG co goi tap (goi thuoc Agent 2): luot vao phong tap duoc ghi thang vao gym_check_ins de
--     man hinh co du lieu, con quet kiosk that voi ho se bi chan MEMBERSHIP_NOT_FOUND (dung quy tac).

SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON; -- bang users co filtered index (V9); sqlcmd mac dinh tat tuy chon nay

DECLARE @now DATETIME2 = CAST(SYSUTCDATETIME() AT TIME ZONE 'UTC' AT TIME ZONE 'SE Asia Standard Time' AS DATETIME2);
DECLARE @today DATE = CAST(@now AS DATE);
DECLARE @nowSec DATETIME2(0) = CAST(@now AS DATETIME2(0));
DECLARE @hash VARCHAR(255) = '$2a$10$sypVy5i6QbvQjcD3FaEU3O.Q.L1JkYeV3C/PFWEUQGjueE2.lTwem';

IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'reception@gym.test')
   OR NOT EXISTS (SELECT 1 FROM users WHERE email = 'member.active@gym.test')
BEGIN
    PRINT 'THIEU USER: can reception@gym.test va member.active@gym.test. Hay chay 00_users.sql truoc.';
    RETURN;
END

-- 1) Tai khoan moi: 3 nhan su va 12 hoi vien (bo qua neu email hoac so dien thoai da co).
DECLARE @seed TABLE (
    email     VARCHAR(150),
    full_name NVARCHAR(150),
    phone     VARCHAR(20),
    role_name VARCHAR(50)
);

INSERT INTO @seed (email, full_name, phone, role_name) VALUES
    ('reception2@gym.test', N'Lễ tân Hai',          '0912000001', 'RECEPTIONIST'),
    ('reception3@gym.test', N'Lễ tân Ba',           '0912000002', 'RECEPTIONIST'),
    ('sales2@gym.test',     N'Nhân viên Sales Hai', '0912000003', 'SALES'),
    ('member01@gym.test',   N'Nguyễn Minh Anh',     '0912000101', 'MEMBER'),
    ('member02@gym.test',   N'Trần Quốc Bảo',       '0912000102', 'MEMBER'),
    ('member03@gym.test',   N'Lê Thị Cẩm Linh',     '0912000103', 'MEMBER'),
    ('member04@gym.test',   N'Phạm Hoàng Dũng',     '0912000104', 'MEMBER'),
    ('member05@gym.test',   N'Võ Thanh Hà',         '0912000105', 'MEMBER'),
    ('member06@gym.test',   N'Đặng Gia Huy',        '0912000106', 'MEMBER'),
    ('member07@gym.test',   N'Bùi Ngọc Lan',        '0912000107', 'MEMBER'),
    ('member08@gym.test',   N'Hoàng Đức Mạnh',      '0912000108', 'MEMBER'),
    ('member09@gym.test',   N'Phan Thị Ngân',       '0912000109', 'MEMBER'),
    ('member10@gym.test',   N'Vũ Tuấn Kiệt',        '0912000110', 'MEMBER'),
    ('member11@gym.test',   N'Đỗ Khánh Vy',         '0912000111', 'MEMBER'),
    ('member12@gym.test',   N'Ngô Thành Long',      '0912000112', 'MEMBER');

INSERT INTO users (full_name, email, phone, password_hash, status)
SELECT s.full_name, s.email, s.phone, @hash, 'ACTIVE'
FROM @seed s
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email = s.email)
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.phone = s.phone);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM @seed s
JOIN users u ON u.email = s.email
JOIN roles r ON r.name = s.role_name
WHERE NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);

DECLARE @missing INT = (SELECT COUNT(*) FROM @seed s WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email = s.email));
IF @missing > 0
    PRINT CONCAT('CANH BAO: ', @missing, ' tai khoan moi khong tao duoc (trung so dien thoai 0912...). Cac tai khoan do se khong co du lieu.');

-- 2) Cham cong nhan su: xoa dong seed cu, tao lai 30 ngay gan nhat (bo Chu nhat).
DELETE FROM staff_attendances WHERE LEFT(notes, 10) = N'[dev-seed]';

DECLARE @staff TABLE (email VARCHAR(150));
INSERT INTO @staff (email) VALUES
    ('reception@gym.test'), ('sales@gym.test'),
    ('reception2@gym.test'), ('reception3@gym.test'), ('sales2@gym.test');

;WITH n AS (
    SELECT TOP (30) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS d FROM sys.all_objects
),
workdays AS (
    -- 1900-01-01 la thu Hai, nen DATEDIFF % 7 = 6 la Chu nhat (khong phu thuoc DATEFIRST cua may)
    SELECT DATEADD(DAY, -d, @today) AS dt FROM n
    WHERE DATEDIFF(DAY, '19000101', DATEADD(DAY, -d, @today)) % 7 <> 6
),
base AS (
    SELECT u.id AS staff_id, w.dt,
           (CHECKSUM(u.email, CONVERT(VARCHAR(10), w.dt, 120)) & 2147483647) % 100 AS r,
           (CHECKSUM(u.email, CONVERT(VARCHAR(10), w.dt, 120), 'in') & 2147483647) % 15 AS m1,
           (CHECKSUM(u.email, CONVERT(VARCHAR(10), w.dt, 120), 'out') & 2147483647) % 30 AS m2
    FROM @staff st
    JOIN users u ON u.email = st.email
    CROSS JOIN workdays w
),
calc AS (
    SELECT staff_id, dt, r, m1, m2,
           CASE WHEN r < 50 THEN 'ON'
                WHEN r < 70 THEN 'LATE'
                WHEN r < 82 THEN 'EARLY'
                WHEN r < 90 THEN 'LATE_EARLY'
                WHEN r < 95 THEN 'ABSENT'
                ELSE 'NONE' END AS kind
    FROM base
)
INSERT INTO staff_attendances (staff_id, date, check_in_time, check_out_time, method, notes)
SELECT c.staff_id, c.dt,
       -- vao: dung gio 06:45-06:59; muon 07:05-07:47
       CASE WHEN c.kind IN ('ON', 'EARLY') THEN DATEADD(MINUTE, 419 - c.m1, CAST(c.dt AS DATETIME2))
            WHEN c.kind IN ('LATE', 'LATE_EARLY') THEN DATEADD(MINUTE, 425 + c.m1 * 3, CAST(c.dt AS DATETIME2))
            ELSE NULL END,
       -- ra: du gio 21:00-21:29; ve som 19:28-20:55; mot so ngay quen quet ra
       CASE WHEN c.kind IN ('ON', 'LATE') AND c.r % 8 = 0 THEN NULL
            WHEN c.kind IN ('ON', 'LATE') THEN DATEADD(MINUTE, 1260 + c.m2, CAST(c.dt AS DATETIME2))
            WHEN c.kind IN ('EARLY', 'LATE_EARLY') THEN DATEADD(MINUTE, 1255 - c.m2 * 3, CAST(c.dt AS DATETIME2))
            ELSE NULL END,
       CASE WHEN c.r % 3 = 0 THEN 'MANUAL' ELSE 'FACE' END,
       CASE WHEN c.kind = 'ABSENT' THEN N'[dev-seed] Vắng không phép'
            WHEN c.kind IN ('ON', 'LATE') AND c.r % 8 = 0 THEN N'[dev-seed] Quên quét ra'
            WHEN c.kind IN ('EARLY', 'LATE_EARLY') THEN N'[dev-seed] Về sớm'
            ELSE N'[dev-seed]' END
FROM calc c
WHERE c.kind <> 'NONE'
  AND NOT EXISTS (SELECT 1 FROM staff_attendances a WHERE a.staff_id = c.staff_id AND a.date = c.dt);

-- Hom nay (chi khi da qua 07:30): hai nhan su moi dang trong ca, chua quet ra. Khong dung tai khoan demo de khong
-- lam lech luot quet that khi ban thu kiosk.
IF @now >= DATEADD(MINUTE, 450, CAST(@today AS DATETIME2))
BEGIN
    INSERT INTO staff_attendances (staff_id, date, check_in_time, check_out_time, method, notes)
    SELECT u.id, @today, DATEADD(MINUTE, t.minutes_of_day, CAST(@today AS DATETIME2)), NULL, 'FACE', N'[dev-seed]'
    FROM (VALUES ('reception2@gym.test', 412), ('reception3@gym.test', 432)) AS t(email, minutes_of_day)
    JOIN users u ON u.email = t.email
    WHERE NOT EXISTS (SELECT 1 FROM staff_attendances a WHERE a.staff_id = u.id AND a.date = @today);
END

-- 3) Luot vao phong tap: xoa dong seed cu (theo chu ky .777), tao lai.
DELETE FROM gym_check_ins WHERE DATEPART(NANOSECOND, check_in_time) = 777000000;

DECLARE @members TABLE (email VARCHAR(150), day_from INT, day_to INT, pct INT);
INSERT INTO @members (email, day_from, day_to, pct) VALUES
    ('member.active@gym.test',  1, 30, 65),
    ('member.expired@gym.test', 31, 75, 30),   -- tap truoc khi goi het han
    ('member01@gym.test', 1, 30, 70), ('member02@gym.test', 1, 30, 60),
    ('member03@gym.test', 1, 30, 55), ('member04@gym.test', 1, 30, 50),
    ('member05@gym.test', 1, 30, 45), ('member06@gym.test', 1, 30, 40),
    ('member07@gym.test', 1, 30, 35), ('member08@gym.test', 1, 30, 30),
    ('member09@gym.test', 1, 30, 25), ('member10@gym.test', 1, 30, 20),
    ('member11@gym.test', 1, 30, 15), ('member12@gym.test', 1, 30, 10);

;WITH n AS (
    SELECT TOP (75) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS d FROM sys.all_objects
)
INSERT INTO gym_check_ins (member_id, branch_id, check_in_time, check_out_time, method)
SELECT u.id, NULL,
       DATEADD(MILLISECOND, 777, DATEADD(MINUTE, 360 + h.mt, CAST(x.dt AS DATETIME2))),
       DATEADD(MILLISECOND, 777, DATEADD(MINUTE, 360 + h.mt + 45 + h.md, CAST(x.dt AS DATETIME2))),
       CASE WHEN h.mm = 0 THEN 'MANUAL' ELSE 'FACE' END
FROM @members m
JOIN users u ON u.email = m.email
JOIN n ON n.d BETWEEN m.day_from AND m.day_to
CROSS APPLY (SELECT DATEADD(DAY, -n.d, @today) AS dt) x
CROSS APPLY (SELECT CONVERT(VARCHAR(10), x.dt, 120) AS ds) y
CROSS APPLY (SELECT (CHECKSUM(m.email, y.ds) & 2147483647) % 100 AS r,
                    (CHECKSUM(m.email, y.ds, 't') & 2147483647) % 840 AS mt,
                    (CHECKSUM(m.email, y.ds, 'd') & 2147483647) % 76 AS md,
                    (CHECKSUM(m.email, y.ds, 'm') & 2147483647) % 5 AS mm) h
WHERE h.r < m.pct;

-- Hom nay (chi khi da qua 07:30): ba hoi vien dang o trong phong tap, chua quet ra.
IF @now >= DATEADD(MINUTE, 450, CAST(@today AS DATETIME2))
BEGIN
    INSERT INTO gym_check_ins (member_id, branch_id, check_in_time, check_out_time, method)
    SELECT u.id, NULL,
           DATEADD(MILLISECOND, 777, CAST(DATEADD(MINUTE, -t.mins, @nowSec) AS DATETIME2(7))),
           NULL, 'FACE'
    FROM (VALUES ('member01@gym.test', 25), ('member02@gym.test', 40), ('member03@gym.test', 55)) AS t(email, mins)
    JOIN users u ON u.email = t.email;
END

-- 4) Ket qua
SELECT 'staff_attendances (seed)' AS bang, COUNT(*) AS so_dong FROM staff_attendances WHERE LEFT(notes, 10) = N'[dev-seed]'
UNION ALL
SELECT 'gym_check_ins (seed)', COUNT(*) FROM gym_check_ins WHERE DATEPART(NANOSECOND, check_in_time) = 777000000;

SELECT status AS trang_thai_cham_cong, COUNT(*) AS so_dong
FROM (
    SELECT CASE
        WHEN check_in_time IS NULL THEN 'ABSENT'
        WHEN check_out_time IS NULL THEN CASE WHEN CAST(check_in_time AS TIME) > '07:00:00' THEN 'LATE' ELSE 'ON_TIME' END
        WHEN CAST(check_in_time AS TIME) > '07:00:00' AND CAST(check_out_time AS TIME) < '21:00:00' THEN 'LATE_AND_LEFT_EARLY'
        WHEN CAST(check_in_time AS TIME) > '07:00:00' THEN 'LATE'
        WHEN CAST(check_out_time AS TIME) < '21:00:00' THEN 'LEFT_EARLY'
        ELSE 'ON_TIME' END AS status
    FROM staff_attendances WHERE LEFT(notes, 10) = N'[dev-seed]'
) s
GROUP BY status
ORDER BY status;

SELECT u.email, COUNT(*) AS so_luot_vao_phong_tap
FROM gym_check_ins g
JOIN users u ON u.id = g.member_id
WHERE DATEPART(NANOSECOND, g.check_in_time) = 777000000
GROUP BY u.email
ORDER BY so_luot_vao_phong_tap DESC;
