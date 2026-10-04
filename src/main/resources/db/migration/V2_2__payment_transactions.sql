-- V2_2__payment_transactions.sql
-- Agent 2 - Membership, Package & Payment Module
-- Bo sung bang payment_transactions (chua co trong V2__membership.sql ban base).
-- Dung chung cho ca giao dich Membership (Agent 2) lan POS (Agent 5, se tham chieu lai
-- qua PaymentService.confirmPayment, khong tu tao service rieng).
--
-- GHI CHU: them cot updated_at (nullable) so voi DATABASE_SCHEMA.md goc (chi co created_at)
-- vi entity extends BaseEntity (Agent 1) co san @LastModifiedDate -> can cot tuong ung
-- de Hibernate schema-validation khong loi. Khong anh huong nghiep vu.

CREATE TABLE payment_transactions (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    user_id UNIQUEIDENTIFIER NOT NULL,
    related_type VARCHAR(20) NOT NULL,      -- MEMBERSHIP, POS
    related_id UNIQUEIDENTIFIER NOT NULL,   -- id cua member_packages hoac pos_orders
    amount DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,    -- CASH, LOCAL_CONFIRM
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SUCCESS, FAILED
    transaction_code VARCHAR(100) NOT NULL UNIQUE,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT fk_payment_transactions_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_payment_transactions_user ON payment_transactions(user_id);
CREATE INDEX idx_payment_transactions_related ON payment_transactions(related_type, related_id);
