-- Agent 5: hội viên tự mua hàng qua POS (thanh toán mô phỏng) nên đơn không có nhân viên thu tiền.
-- V5_1 (không dùng V10) để không đụng số phiên bản với migration của agent khác; spring.flyway.out-of-order = true
-- cho phép áp dụng trên DB đã chạy tới V9. Khoá ngoại fk_pos_orders_staff giữ nguyên (NULL không vi phạm khoá ngoại).
ALTER TABLE pos_orders ALTER COLUMN staff_id UNIQUEIDENTIFIER NULL;
