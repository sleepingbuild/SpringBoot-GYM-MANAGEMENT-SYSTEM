-- 00_users.sql  (Agent 1)
-- Tai khoan thu cho moi vai tro. CHI DUNG CHO DATABASE DEV, KHONG phai migration Flyway, khong chay tu dong.
-- Mat khau chung: Gym@12345   (BCrypt cost 10)
-- Chay lai nhieu lan khong loi: tai khoan da co (theo email) thi bo qua.
-- Luu y: file luu UTF-8 co BOM de SSMS doc dung chu Viet; neu chay bang sqlcmd thi them tham so -f 65001.
-- Thu tu chay: 00_users.sql roi cac file khac trong scripts/dev-seed/ theo thu tu ten.

SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON; -- bang users co filtered index (V9); sqlcmd mac dinh tat tuy chon nay

DECLARE @hash VARCHAR(255) = '$2a$10$sypVy5i6QbvQjcD3FaEU3O.Q.L1JkYeV3C/PFWEUQGjueE2.lTwem';

DECLARE @seed TABLE (
    email     VARCHAR(150),
    full_name NVARCHAR(150),
    phone     VARCHAR(20),
    role_name VARCHAR(50)
);

INSERT INTO @seed (email, full_name, phone, role_name) VALUES
    ('admin@gym.test',            N'Quản trị viên Demo',     '0911000001', 'SUPER_ADMIN'),
    ('reception@gym.test',        N'Lễ tân Demo',            '0911000002', 'RECEPTIONIST'),
    ('sales@gym.test',            N'Nhân viên Sales Demo',   '0911000003', 'SALES'),
    ('trainer1@gym.test',         N'Huấn luyện viên Một',    '0911000004', 'TRAINER'),
    ('trainer2@gym.test',         N'Huấn luyện viên Hai',    '0911000005', 'TRAINER'),
    ('member.active@gym.test',    N'Hội viên Đang tập',      '0911000006', 'MEMBER'),
    ('member.nopackage@gym.test', N'Hội viên Chưa có gói',   '0911000007', 'MEMBER'),
    ('member.expired@gym.test',   N'Hội viên Hết hạn',       '0911000008', 'MEMBER');

-- 1. Tao user chua co (theo email)
INSERT INTO users (full_name, email, phone, password_hash, status)
SELECT s.full_name, s.email, s.phone, @hash, 'ACTIVE'
FROM @seed s
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email = s.email);

-- 2. Gan vai tro chua co
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM @seed s
JOIN users u ON u.email = s.email
JOIN roles r ON r.name = s.role_name
WHERE NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);

-- 3. Ket qua
SELECT u.email, u.full_name, r.name AS role_name, u.status
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
WHERE u.email LIKE '%@gym.test'
ORDER BY r.name, u.email;
