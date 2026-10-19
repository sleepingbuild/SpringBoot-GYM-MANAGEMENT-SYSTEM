-- gms_schema_v2.sql
-- GYM MANAGEMENT SYSTEM (GMS) - thiet ke co so du lieu muc tieu v2 (19 bang, 28 khoa ngoai)
-- Cu phap T-SQL cho Microsoft SQL Server 2022. Day la script THIET KE, tuong ung voi DATABASE_SCHEMA.md
-- va ERD_TONG_QUAT.md; he thong that tao bang bang cac migration Flyway (src/main/resources/db/migration).
-- Cach kiem tra tren DB trong (khong dung gms_db): xem docs/README.md muc "Chay thu script thiet ke".
-- Quy uoc: UNIQUEIDENTIFIER cho khoa chinh; DATETIME2 cho thoi gian; NVARCHAR cho chu tieng Viet,
-- VARCHAR cho ma/email/enum; enum luu VARCHAR (khong dung CHECK cho enum).


CREATE TABLE branches (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    name NVARCHAR(150) NOT NULL,
    address NVARCHAR(255) NULL,
    phone VARCHAR(20) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT pk_branches PRIMARY KEY (id)
);

CREATE TABLE packages (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    name NVARCHAR(150) NOT NULL,
    description NVARCHAR(500) NULL,
    price DECIMAL(12,2) NOT NULL,
    duration_days INT NULL,
    session_count INT NULL,
    package_type VARCHAR(20) NOT NULL,
    peak_type VARCHAR(20) NOT NULL DEFAULT 'FULL_TIME',
    max_sessions_per_week INT NULL,
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT pk_packages PRIMARY KEY (id)
);

CREATE TABLE products (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    name NVARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    unit NVARCHAR(20) NULL,
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT pk_products PRIMARY KEY (id)
);
CREATE INDEX idx_products_category ON products (category);

CREATE TABLE roles (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    name VARCHAR(50) NOT NULL,
    description NVARCHAR(255) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uq_roles_name UNIQUE (name)
);

CREATE TABLE users (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    full_name NVARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_status ON users (status);
-- Loc duy nhat: nhieu user co the khong co so dien thoai (NULL), nhung so da co thi khong trung.
CREATE UNIQUE INDEX uq_users_phone ON users (phone) WHERE phone IS NOT NULL;

CREATE TABLE bookings (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    member_id UNIQUEIDENTIFIER NOT NULL,
    trainer_id UNIQUEIDENTIFIER NOT NULL,
    branch_id UNIQUEIDENTIFIER NULL,
    booking_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    check_in_time DATETIME2 NULL,
    check_in_method VARCHAR(20) NULL,
    check_out_time DATETIME2 NULL,
    check_out_method VARCHAR(20) NULL,
    pt_check_in_time DATETIME2 NULL,
    pt_check_in_method VARCHAR(20) NULL,
    pt_check_out_time DATETIME2 NULL,
    pt_check_out_method VARCHAR(20) NULL,
    pt_early_leave_reason NVARCHAR(255) NULL,
    notes NVARCHAR(500) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_bookings PRIMARY KEY (id),
    CONSTRAINT fk_bookings_member_id FOREIGN KEY (member_id) REFERENCES users(id),
    CONSTRAINT fk_bookings_trainer_id FOREIGN KEY (trainer_id) REFERENCES users(id),
    CONSTRAINT fk_bookings_branch_id FOREIGN KEY (branch_id) REFERENCES branches(id)
);
CREATE INDEX idx_booking_trainer_date ON bookings (trainer_id, booking_date);
CREATE INDEX idx_booking_member_date ON bookings (member_id, booking_date);

CREATE TABLE commissions (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    staff_id UNIQUEIDENTIFIER NOT NULL,
    booking_id UNIQUEIDENTIFIER NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT pk_commissions PRIMARY KEY (id),
    CONSTRAINT uq_commissions_booking_id UNIQUE (booking_id),
    CONSTRAINT fk_commissions_staff_id FOREIGN KEY (staff_id) REFERENCES users(id),
    CONSTRAINT fk_commissions_booking_id FOREIGN KEY (booking_id) REFERENCES bookings(id)
);
CREATE INDEX idx_commissions_staff_status ON commissions (staff_id, status);

CREATE TABLE face_profiles (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    user_id UNIQUEIDENTIFIER NOT NULL,
    descriptor NVARCHAR(MAX) NOT NULL,
    reference_image VARCHAR(255) NULL,
    registered_by UNIQUEIDENTIFIER NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT pk_face_profiles PRIMARY KEY (id),
    CONSTRAINT uq_face_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_face_profiles_user_id FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_face_profiles_registered_by FOREIGN KEY (registered_by) REFERENCES users(id)
);

CREATE TABLE gym_check_ins (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    member_id UNIQUEIDENTIFIER NOT NULL,
    branch_id UNIQUEIDENTIFIER NULL,
    check_in_time DATETIME2 NOT NULL,
    check_out_time DATETIME2 NULL,
    method VARCHAR(20) NOT NULL DEFAULT 'FACE',
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT pk_gym_check_ins PRIMARY KEY (id),
    CONSTRAINT fk_gym_check_ins_member_id FOREIGN KEY (member_id) REFERENCES users(id),
    CONSTRAINT fk_gym_check_ins_branch_id FOREIGN KEY (branch_id) REFERENCES branches(id)
);
CREATE INDEX idx_gym_check_ins_member_time ON gym_check_ins (member_id, check_in_time);

CREATE TABLE leads (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    full_name NVARCHAR(150) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    source NVARCHAR(50) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    assigned_sales_id UNIQUEIDENTIFIER NULL,
    converted_user_id UNIQUEIDENTIFIER NULL,
    notes NVARCHAR(500) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT pk_leads PRIMARY KEY (id),
    CONSTRAINT fk_leads_assigned_sales_id FOREIGN KEY (assigned_sales_id) REFERENCES users(id),
    CONSTRAINT fk_leads_converted_user_id FOREIGN KEY (converted_user_id) REFERENCES users(id)
);
CREATE INDEX idx_leads_status ON leads (status);
CREATE INDEX idx_leads_phone ON leads (phone);

CREATE TABLE member_packages (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    user_id UNIQUEIDENTIFIER NOT NULL,
    package_id UNIQUEIDENTIFIER NOT NULL,
    branch_id UNIQUEIDENTIFIER NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    start_date DATE NOT NULL,
    end_date DATE NULL,
    remaining_sessions INT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT pk_member_packages PRIMARY KEY (id),
    CONSTRAINT fk_member_packages_branch_id FOREIGN KEY (branch_id) REFERENCES branches(id),
    CONSTRAINT fk_member_packages_package_id FOREIGN KEY (package_id) REFERENCES packages(id),
    CONSTRAINT fk_member_packages_user_id FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX idx_member_packages_status ON member_packages (status);
CREATE INDEX idx_member_packages_user ON member_packages (user_id);

CREATE TABLE pos_orders (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    member_id UNIQUEIDENTIFIER NULL,
    staff_id UNIQUEIDENTIFIER NOT NULL,
    branch_id UNIQUEIDENTIFIER NULL,
    payment_method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT pk_pos_orders PRIMARY KEY (id),
    CONSTRAINT fk_pos_orders_member_id FOREIGN KEY (member_id) REFERENCES users(id),
    CONSTRAINT fk_pos_orders_staff_id FOREIGN KEY (staff_id) REFERENCES users(id),
    CONSTRAINT fk_pos_orders_branch_id FOREIGN KEY (branch_id) REFERENCES branches(id)
);
CREATE INDEX idx_pos_orders_branch_created ON pos_orders (branch_id, created_at);

CREATE TABLE staff_attendances (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    staff_id UNIQUEIDENTIFIER NOT NULL,
    date DATE NOT NULL,
    check_in_time DATETIME2 NULL,
    check_out_time DATETIME2 NULL,
    method VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    notes NVARCHAR(255) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT pk_staff_attendances PRIMARY KEY (id),
    CONSTRAINT uq_staff_attendances_staff_id_date UNIQUE (staff_id, date),
    CONSTRAINT fk_staff_attendances_staff_id FOREIGN KEY (staff_id) REFERENCES users(id)
);
CREATE INDEX idx_staff_attendances_date ON staff_attendances (date);

CREATE TABLE trainer_pay_rates (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    trainer_id UNIQUEIDENTIFIER NOT NULL,
    amount_per_session DECIMAL(12,2) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT pk_trainer_pay_rates PRIMARY KEY (id),
    CONSTRAINT uq_trainer_pay_rates_trainer_id UNIQUE (trainer_id),
    CONSTRAINT fk_trainer_pay_rates_trainer_id FOREIGN KEY (trainer_id) REFERENCES users(id)
);

CREATE TABLE trainer_schedules (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    trainer_id UNIQUEIDENTIFIER NOT NULL,
    work_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_trainer_schedules PRIMARY KEY (id),
    CONSTRAINT fk_trainer_schedules_trainer_id FOREIGN KEY (trainer_id) REFERENCES users(id)
);
CREATE INDEX idx_trainer_schedule_trainer_date ON trainer_schedules (trainer_id, work_date);

CREATE TABLE user_profiles (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    user_id UNIQUEIDENTIFIER NOT NULL,
    date_of_birth DATE NULL,
    weight_kg DECIMAL(5,2) NULL,
    height_cm DECIMAL(5,2) NULL,
    goal NVARCHAR(255) NULL,
    avatar_url VARCHAR(255) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT pk_user_profiles PRIMARY KEY (id),
    CONSTRAINT uq_user_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_user_profiles_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE user_roles (
    user_id UNIQUEIDENTIFIER NOT NULL,
    role_id UNIQUEIDENTIFIER NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_role_id FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE payment_transactions (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    user_id UNIQUEIDENTIFIER NULL,
    member_package_id UNIQUEIDENTIFIER NULL,
    pos_order_id UNIQUEIDENTIFIER NULL,
    amount DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_code VARCHAR(100) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT pk_payment_transactions PRIMARY KEY (id),
    CONSTRAINT uq_payment_transactions_transaction_code UNIQUE (transaction_code),
    CONSTRAINT fk_payment_transactions_user_id FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_payment_transactions_member_package_id FOREIGN KEY (member_package_id) REFERENCES member_packages(id),
    CONSTRAINT fk_payment_transactions_pos_order_id FOREIGN KEY (pos_order_id) REFERENCES pos_orders(id),
    CONSTRAINT ck_payment_transactions_target CHECK ((member_package_id IS NOT NULL AND pos_order_id IS NULL) OR (member_package_id IS NULL AND pos_order_id IS NOT NULL))
);
CREATE INDEX idx_payment_transactions_user ON payment_transactions (user_id);
CREATE INDEX idx_payment_transactions_member_package ON payment_transactions (member_package_id);
CREATE INDEX idx_payment_transactions_pos_order ON payment_transactions (pos_order_id);

CREATE TABLE pos_order_items (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    order_id UNIQUEIDENTIFIER NOT NULL,
    product_id UNIQUEIDENTIFIER NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT pk_pos_order_items PRIMARY KEY (id),
    CONSTRAINT fk_pos_order_items_order_id FOREIGN KEY (order_id) REFERENCES pos_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_pos_order_items_product_id FOREIGN KEY (product_id) REFERENCES products(id)
);
CREATE INDEX idx_pos_order_items_order ON pos_order_items (order_id);
CREATE INDEX idx_pos_order_items_product ON pos_order_items (product_id);
