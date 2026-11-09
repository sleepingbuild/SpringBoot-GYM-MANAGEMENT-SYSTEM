-- 30_pos.sql (Agent 5): san pham POS mau va muc luong moi buoi cua PT.
-- CHI DUNG CHO DATABASE DEV. Khong nam trong Flyway, chay thu cong, chay lai nhieu lan khong loi.
-- Thu tu chay: sau 00_users.sql (Agent 1). Khong can 10_ va 20_.
--
-- Quy tac:
--   * San pham: chi them khi chua co san pham cung ten (khong ghi de gia/ton kho da sua tay).
--   * Muc luong PT: tham chieu user theo email; chi them khi PT chua co muc luong, khong ghi de.
--   * Co san pham ton kho thap (BCAA: 3) va het hang (Khan the thao: 0) de thu giao dien.
--
-- Chua chay thu tren SQL Server that khi viet file nay: neu loi cu phap, bao Agent 5.

DECLARE @products TABLE (name NVARCHAR(150), category VARCHAR(50), price DECIMAL(12,2), stock INT, unit NVARCHAR(20));
INSERT INTO @products (name, category, price, stock, unit) VALUES
    (N'Nước suối 500ml',         'DRINK',      10000,  100, N'chai'),
    (N'Nước tăng lực',           'DRINK',      25000,   40, N'chai'),
    (N'Whey Protein 2lb',        'SUPPLEMENT', 850000,   8, N'hộp'),
    (N'BCAA 30 lần dùng',        'SUPPLEMENT', 450000,   3, N'hộp'),
    (N'Găng tay tập',            'ACCESSORY',  120000,  15, N'đôi'),
    (N'Khăn thể thao',           'ACCESSORY',   60000,   0, N'cái');

INSERT INTO products (name, category, price, stock_quantity, unit, is_active)
SELECT p.name, p.category, p.price, p.stock, p.unit, 1
FROM @products p
WHERE NOT EXISTS (SELECT 1 FROM products x WHERE x.name = p.name);

DECLARE @trainer1 UNIQUEIDENTIFIER = (SELECT id FROM users WHERE email = 'trainer1@gym.test');
DECLARE @trainer2 UNIQUEIDENTIFIER = (SELECT id FROM users WHERE email = 'trainer2@gym.test');

IF @trainer1 IS NULL OR @trainer2 IS NULL
BEGIN
    PRINT 'THIEU USER: can trainer1, trainer2 @gym.test de dat muc luong. Hay chay 00_users.sql truoc. San pham da duoc them.';
    RETURN;
END

INSERT INTO trainer_pay_rates (trainer_id, amount_per_session)
SELECT t.id, t.amount
FROM (VALUES (@trainer1, CAST(150000 AS DECIMAL(12,2))), (@trainer2, CAST(200000 AS DECIMAL(12,2)))) AS t(id, amount)
WHERE NOT EXISTS (SELECT 1 FROM trainer_pay_rates r WHERE r.trainer_id = t.id);
