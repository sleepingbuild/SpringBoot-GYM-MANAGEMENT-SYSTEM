-- 10_packages_memberships.sql  (Agent 2)
-- Du lieu mau: goi tap va goi cua hoi vien, de thu giao dien va cac module khac.
--
-- QUY TAC (xem REPLIES_FRONTEND_PLANS.md muc 0.2):
--   * KHONG nam trong migration Flyway, chay thu cong tren database DEV.
--   * Chay lai nhieu lan khong loi (IF NOT EXISTS), khong ghi cung UUID, tai khoan tham chieu theo EMAIL.
--   * Ngay gio tuong doi theo gio Viet Nam (SQL Server trong Docker chay UTC).
--   * Can 00_users.sql (Agent 1) chay truoc. Thieu tai khoan nao thi bo qua phan cua tai khoan do.
--
-- CHAY (giu dau tieng Viet, -f 65001):
--   docker cp 10_packages_memberships.sql <container>:/tmp/10.sql
--   docker exec <container> /opt/mssql-tools18/bin/sqlcmd -f 65001 -S localhost -U sa -P "<mat khau>" -C -d <database> -b -i /tmp/10.sql
--
-- KET QUA:
--   4 goi dang ban + 1 goi da ngung ban (de thu 404 cong khai).
--   member.active@gym.test  : goi PT 1-1 ACTIVE, con 10/12 buoi, con han (de Agent 3 dat lich).
--   member.expired@gym.test : goi 1 thang da het han (status EXPIRED).
--   member.nopackage@gym.test: KHONG co goi nao (de thu dang ky moi).
--   Giao dich thanh toan SUCCESS cho hai goi tren duoc them neu bang da qua V7 (co cot member_package_id).

SET NOCOUNT ON;

DECLARE @nowVn DATETIME2 = CAST(SYSUTCDATETIME() AT TIME ZONE 'UTC' AT TIME ZONE 'SE Asia Standard Time' AS DATETIME2);
DECLARE @today DATE = CAST(@nowVn AS DATE);

-- ---------------------------------------------------------------------------
-- 1. Goi tap
-- ---------------------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM packages WHERE name = N'Gói 1 tháng (Full-time)')
    INSERT INTO packages (name, description, price, duration_days, session_count, package_type, peak_type, max_sessions_per_week, is_active)
    VALUES (N'Gói 1 tháng (Full-time)', N'Tập không giới hạn trong 30 ngày, mọi khung giờ.', 500000, 30, NULL, 'TIME_BASED', 'FULL_TIME', NULL, 1);

IF NOT EXISTS (SELECT 1 FROM packages WHERE name = N'Gói 3 tháng (Off-peak)')
    INSERT INTO packages (name, description, price, duration_days, session_count, package_type, peak_type, max_sessions_per_week, is_active)
    VALUES (N'Gói 3 tháng (Off-peak)', N'Tập không giới hạn trong 90 ngày, ngoài giờ cao điểm.', 1200000, 90, NULL, 'TIME_BASED', 'OFF_PEAK', NULL, 1);

IF NOT EXISTS (SELECT 1 FROM packages WHERE name = N'Gói 12 buổi tập')
    INSERT INTO packages (name, description, price, duration_days, session_count, package_type, peak_type, max_sessions_per_week, is_active)
    VALUES (N'Gói 12 buổi tập', N'12 buổi tập, dùng trong vòng 90 ngày.', 600000, 90, 12, 'SESSION_BASED', 'FULL_TIME', NULL, 1);

IF NOT EXISTS (SELECT 1 FROM packages WHERE name = N'Gói PT 1-1 (12 buổi)')
    INSERT INTO packages (name, description, price, duration_days, session_count, package_type, peak_type, max_sessions_per_week, is_active)
    VALUES (N'Gói PT 1-1 (12 buổi)', N'12 buổi tập cùng huấn luyện viên, tối đa 3 buổi mỗi tuần.', 3600000, 90, 12, 'PT_1ON1', 'FULL_TIME', 3, 1);

IF NOT EXISTS (SELECT 1 FROM packages WHERE name = N'Gói thử nghiệm (đã ngừng bán)')
    INSERT INTO packages (name, description, price, duration_days, session_count, package_type, peak_type, max_sessions_per_week, is_active)
    VALUES (N'Gói thử nghiệm (đã ngừng bán)', N'Gói đã gỡ khỏi danh sách, dùng để thử trường hợp không tìm thấy.', 100000, 7, NULL, 'TIME_BASED', 'FULL_TIME', NULL, 0);

-- ---------------------------------------------------------------------------
-- 2. Goi cua hoi vien
-- ---------------------------------------------------------------------------
DECLARE @uActive  UNIQUEIDENTIFIER = (SELECT TOP 1 id FROM users WHERE email = 'member.active@gym.test');
DECLARE @uExpired UNIQUEIDENTIFIER = (SELECT TOP 1 id FROM users WHERE email = 'member.expired@gym.test');
DECLARE @pkgPt    UNIQUEIDENTIFIER = (SELECT TOP 1 id FROM packages WHERE name = N'Gói PT 1-1 (12 buổi)');
DECLARE @pkgMonth UNIQUEIDENTIFIER = (SELECT TOP 1 id FROM packages WHERE name = N'Gói 1 tháng (Full-time)');
DECLARE @branch   UNIQUEIDENTIFIER = (SELECT TOP 1 id FROM branches ORDER BY created_at);

-- member.active: goi PT 1-1, bat dau 10 ngay truoc, het han = bat dau + 90 - 1 ngay, da dung 2/12 buoi
IF @uActive IS NULL
    PRINT N'Bo qua member.active@gym.test: chua co tai khoan (chay 00_users.sql truoc).';
ELSE IF NOT EXISTS (SELECT 1 FROM member_packages WHERE user_id = @uActive AND status = 'ACTIVE')
    INSERT INTO member_packages (user_id, package_id, branch_id, status, start_date, end_date, remaining_sessions)
    VALUES (@uActive, @pkgPt, @branch, 'ACTIVE', DATEADD(DAY, -10, @today), DATEADD(DAY, 79, @today), 10);

-- member.expired: goi 1 thang bat dau 50 ngay truoc, da het han 21 ngay truoc
IF @uExpired IS NULL
    PRINT N'Bo qua member.expired@gym.test: chua co tai khoan (chay 00_users.sql truoc).';
ELSE IF NOT EXISTS (SELECT 1 FROM member_packages WHERE user_id = @uExpired AND status = 'EXPIRED')
    INSERT INTO member_packages (user_id, package_id, branch_id, status, start_date, end_date, remaining_sessions)
    VALUES (@uExpired, @pkgMonth, @branch, 'EXPIRED', DATEADD(DAY, -50, @today), DATEADD(DAY, -21, @today), NULL);

-- ---------------------------------------------------------------------------
-- 3. Giao dich thanh toan SUCCESS cho hai goi tren (chi khi da qua V7: co cot member_package_id)
--    Dung SQL dong vi SQL Server bien dich ten cot truoc khi chay, nen truoc V7 cau INSERT nay se loi.
-- ---------------------------------------------------------------------------
IF COL_LENGTH('payment_transactions', 'member_package_id') IS NULL
BEGIN
    PRINT N'Bang payment_transactions chua qua V7: bo qua giao dich thanh toan mau.';
END
ELSE
BEGIN
    DECLARE @mpActive  UNIQUEIDENTIFIER = (SELECT TOP 1 id FROM member_packages WHERE user_id = @uActive AND status = 'ACTIVE');
    DECLARE @mpExpired UNIQUEIDENTIFIER = (SELECT TOP 1 id FROM member_packages WHERE user_id = @uExpired AND status = 'EXPIRED');
    DECLARE @tsActive  DATETIME2 = DATEADD(DAY, -10, @nowVn);
    DECLARE @tsExpired DATETIME2 = DATEADD(DAY, -50, @nowVn);
    DECLARE @ins NVARCHAR(MAX) = N'
        INSERT INTO payment_transactions (user_id, member_package_id, amount, payment_method, status, transaction_code, created_at, updated_at)
        VALUES (@u, @mp, @amount, ''CASH'', ''SUCCESS'', @code, @ts, @ts);';

    IF @mpActive IS NOT NULL AND NOT EXISTS (SELECT 1 FROM payment_transactions WHERE transaction_code = 'SEED-TX-ACTIVE')
        EXEC sp_executesql @ins,
             N'@u UNIQUEIDENTIFIER, @mp UNIQUEIDENTIFIER, @amount DECIMAL(12,2), @code VARCHAR(100), @ts DATETIME2',
             @u = @uActive, @mp = @mpActive, @amount = 3600000, @code = 'SEED-TX-ACTIVE', @ts = @tsActive;

    IF @mpExpired IS NOT NULL AND NOT EXISTS (SELECT 1 FROM payment_transactions WHERE transaction_code = 'SEED-TX-EXPIRED')
        EXEC sp_executesql @ins,
             N'@u UNIQUEIDENTIFIER, @mp UNIQUEIDENTIFIER, @amount DECIMAL(12,2), @code VARCHAR(100), @ts DATETIME2',
             @u = @uExpired, @mp = @mpExpired, @amount = 500000, @code = 'SEED-TX-EXPIRED', @ts = @tsExpired;
END

PRINT N'Xong 10_packages_memberships.sql';
