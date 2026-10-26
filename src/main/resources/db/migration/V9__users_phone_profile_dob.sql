-- V9__users_phone_profile_dob.sql
-- Agent 1 - SHARED. Khong sua cac migration cu; moi thay doi schema nam trong file nay.
--   1) users.phone: thay UNIQUE thuong (chi cho 1 dong NULL) bang filtered unique index
--      -> nhieu user khong co so dien thoai van hop le, so da co thi van khong duoc trung.
--   2) CH3: user_profiles.age -> date_of_birth (quy tac >= 18 tuoi so voi users.created_at kiem tra o tang service)
--   3) CH4: bo users.avatar_url (chi giu user_profiles.avatar_url)

-- 1a. Chuoi rong/khoang trang tro thanh NULL
UPDATE users SET phone = NULL WHERE phone IS NOT NULL AND LTRIM(RTRIM(phone)) = '';

-- 1b. Go UNIQUE constraint tu dat ten tren users.phone (ten do SQL Server sinh nen phai tra cuu).
--     Luu y T-SQL: khong duoc goi ham (QUOTENAME) hay noi chuoi ben trong EXEC( ... ),
--     phai gan vao bien truoc roi EXEC sp_executesql.
DECLARE @uq sysname;
DECLARE @drop nvarchar(400);
SELECT @uq = kc.name
FROM sys.key_constraints kc
JOIN sys.index_columns ic ON ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id
JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
WHERE kc.parent_object_id = OBJECT_ID('users') AND kc.type = 'UQ' AND c.name = 'phone';
IF @uq IS NOT NULL
BEGIN
    SET @drop = N'ALTER TABLE users DROP CONSTRAINT ' + QUOTENAME(@uq);
    EXEC sp_executesql @drop;
END

-- 1c. Filtered unique index: chi rang buoc khi phone co gia tri
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uq_users_phone' AND object_id = OBJECT_ID('users'))
    CREATE UNIQUE INDEX uq_users_phone ON users(phone) WHERE phone IS NOT NULL;

-- 2. CH3: tuoi -> ngay sinh. Tuoi cu khong doi chinh xac duoc thanh ngay sinh nen de NULL, nguoi dung nhap lai.
IF COL_LENGTH('user_profiles', 'date_of_birth') IS NULL
    ALTER TABLE user_profiles ADD date_of_birth DATE NULL;
IF COL_LENGTH('user_profiles', 'age') IS NOT NULL
    ALTER TABLE user_profiles DROP COLUMN age;

-- 3. CH4: bo users.avatar_url
IF COL_LENGTH('users', 'avatar_url') IS NOT NULL
    ALTER TABLE users DROP COLUMN avatar_url;
