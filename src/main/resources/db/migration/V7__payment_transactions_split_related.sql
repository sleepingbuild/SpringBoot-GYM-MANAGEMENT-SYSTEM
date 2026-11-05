-- V7__payment_transactions_split_related.sql
-- Agent 2 - CH1 (xem DECISIONS_DB_V2.md): tach payment_transactions.related_type/related_id
-- (polymorphic thu cong, khong co khoa ngoai) thanh 2 khoa ngoai that:
--   member_package_id -> member_packages(id)   (giao dich cua goi tap)
--   pos_order_id      -> pos_orders(id)        (giao dich cua don POS, Agent 5)
-- Moi giao dich thuoc DUNG MOT trong hai loai. user_id cho phep NULL (khach vang lai cua POS).
--
-- PHU THUOC: bang pos_orders (V5, Agent 5) phai ton tai truoc V7. Do spring.flyway.out-of-order = true
-- nen V5 merge sau cac V nho hon van chay duoc, nhung V7 CHI duoc merge sau khi V5 da len master.
-- Khong sua file nay sau khi da merge vao master; moi thay doi sau nay la migration moi.
--
-- Moi buoc cach nhau bang GO: SQL Server bien dich ca batch truoc khi chay, nen cot vua ADD
-- khong dung duoc trong cung batch (loi "Invalid column name").

-- 1. Them 2 cot moi (nullable, chua rang buoc)
ALTER TABLE payment_transactions ADD member_package_id UNIQUEIDENTIFIER NULL;
ALTER TABLE payment_transactions ADD pos_order_id UNIQUEIDENTIFIER NULL;
GO

-- 2. Chuyen du lieu cu theo related_type
UPDATE payment_transactions SET member_package_id = related_id WHERE related_type = 'MEMBERSHIP';
UPDATE payment_transactions SET pos_order_id = related_id WHERE related_type = 'POS';
GO

-- 3. Dung mot trong hai. Neu buoc nay loi nghia la du lieu cu co related_type khac MEMBERSHIP/POS.
ALTER TABLE payment_transactions WITH CHECK ADD CONSTRAINT ck_payment_transactions_related
    CHECK ((member_package_id IS NOT NULL AND pos_order_id IS NULL)
        OR (member_package_id IS NULL AND pos_order_id IS NOT NULL));
GO

-- 4. Khoa ngoai that. Neu loi nghia la du lieu cu tro toi member_packages/pos_orders khong ton tai.
ALTER TABLE payment_transactions WITH CHECK ADD CONSTRAINT fk_payment_transactions_member_package
    FOREIGN KEY (member_package_id) REFERENCES member_packages(id);
ALTER TABLE payment_transactions WITH CHECK ADD CONSTRAINT fk_payment_transactions_pos_order
    FOREIGN KEY (pos_order_id) REFERENCES pos_orders(id);
GO

-- 5. Chi muc moi
CREATE INDEX idx_payment_transactions_member_package ON payment_transactions(member_package_id);
CREATE INDEX idx_payment_transactions_pos_order ON payment_transactions(pos_order_id);
GO

-- 6. Bo cot va chi muc cu (chi muc phai bo truoc khi bo cot)
DROP INDEX idx_payment_transactions_related ON payment_transactions;
GO
ALTER TABLE payment_transactions DROP COLUMN related_type, related_id;
GO

-- 7. Nguoi mua co the vang mat (khach vang lai cua POS)
ALTER TABLE payment_transactions ALTER COLUMN user_id UNIQUEIDENTIFIER NULL;
GO
