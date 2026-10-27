-- V5__pos_commission.sql
-- CH5 (docs/DB_DESIGN_REVIEW.md): KHÔNG có pos_orders.total_amount và pos_order_items.subtotal —
-- tổng suy ra từ quantity x unit_price; số tiền đã thu nằm ở payment_transactions.amount.
-- Agent 5 — POS & Lương PT theo buổi
-- Khớp DATABASE_SCHEMA.md (nhánh docs/database-schema-complete) và
-- docs/CHANGE_PT_ATTENDANCE_AND_PAY.md mục "Agent 5". Không sửa lại sau khi đã merge
-- vào main (CONTRIBUTING.md mục 5) — mọi thay đổi sau này là migration MỚI (V5_1, ...).
--
-- Quy ước created_at/updated_at theo đúng V1/V2/V2_1/V2_2 (không theo V3, nơi
-- updated_at đang để NOT NULL DEFAULT — khác với BaseEntity.updatedAt là cột nullable).

CREATE TABLE products (
    id              UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    name            NVARCHAR(150) NOT NULL,
    category        VARCHAR(50) NOT NULL,              -- DRINK, SUPPLEMENT, ACCESSORY
    price           DECIMAL(12,2) NOT NULL,
    stock_quantity  INT NOT NULL DEFAULT 0,
    unit            NVARCHAR(20) NULL,                 -- đơn vị tiếng Việt: "hộp", "chai"...
    is_active       BIT NOT NULL DEFAULT 1,
    created_at      DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2 NULL
);

CREATE INDEX idx_products_category ON products(category);

CREATE TABLE pos_orders (
    id              UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    member_id       UNIQUEIDENTIFIER NULL,             -- NULL = khách vãng lai
    staff_id        UNIQUEIDENTIFIER NOT NULL,
    branch_id       UNIQUEIDENTIFIER NULL,
    payment_method  VARCHAR(20) NOT NULL,               -- CASH, LOCAL_CONFIRM
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, COMPLETED, CANCELLED
    created_at      DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2 NULL,
    CONSTRAINT fk_pos_orders_member FOREIGN KEY (member_id) REFERENCES users(id),
    CONSTRAINT fk_pos_orders_staff FOREIGN KEY (staff_id) REFERENCES users(id),
    CONSTRAINT fk_pos_orders_branch FOREIGN KEY (branch_id) REFERENCES branches(id)
);

CREATE INDEX idx_pos_orders_branch_created ON pos_orders(branch_id, created_at);

CREATE TABLE pos_order_items (
    id              UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    order_id        UNIQUEIDENTIFIER NOT NULL,
    product_id      UNIQUEIDENTIFIER NOT NULL,
    quantity        INT NOT NULL,
    unit_price      DECIMAL(12,2) NOT NULL,            -- giá chụp tại thời điểm bán
    created_at      DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2 NULL,
    CONSTRAINT fk_pos_order_items_order FOREIGN KEY (order_id) REFERENCES pos_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_pos_order_items_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE INDEX idx_pos_order_items_order ON pos_order_items(order_id);
CREATE INDEX idx_pos_order_items_product ON pos_order_items(product_id);

CREATE TABLE trainer_pay_rates (
    id                  UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    trainer_id          UNIQUEIDENTIFIER NOT NULL UNIQUE,
    amount_per_session  DECIMAL(12,2) NOT NULL,
    created_at          DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at          DATETIME2 NULL,
    CONSTRAINT fk_trainer_pay_rates_trainer FOREIGN KEY (trainer_id) REFERENCES users(id)
);

-- booking_id KHÔNG map quan hệ JPA sang entity Booking (chỉ UUID thô + FK ở tầng DB) —
-- giảm phụ thuộc cứng vào entity của Agent 3, cùng tinh thần với Booking.branchId
-- (xem ARCHITECTURE.md + Javadoc Booking.java). UNIQUE để 1 buổi tối đa 1 bản ghi lương,
-- chống tính trùng khi BookingSettledEvent có thể được publish lại.
CREATE TABLE commissions (
    id              UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    staff_id        UNIQUEIDENTIFIER NOT NULL,
    booking_id      UNIQUEIDENTIFIER NOT NULL UNIQUE,
    outcome         VARCHAR(20) NOT NULL,               -- COMPLETED, MEMBER_NO_SHOW, LATE_CANCEL
    amount          DECIMAL(12,2) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PAID
    paid_at         DATETIME2 NULL,
    created_at      DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2 NULL,
    CONSTRAINT fk_commissions_staff FOREIGN KEY (staff_id) REFERENCES users(id),
    CONSTRAINT fk_commissions_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE INDEX idx_commissions_staff_status ON commissions(staff_id, status);
