-- V6__leads.sql — Agent 6: Leads/CRM
-- Bảng khách tiềm năng do Sales quản lý. Chỉ phụ thuộc bảng users (V1).
-- Trạng thái lưu VARCHAR (không CHECK): NEW, CONTACTED, CONVERTED, LOST.

CREATE TABLE leads (
    id UNIQUEIDENTIFIER NOT NULL CONSTRAINT df_leads_id DEFAULT NEWID(),
    full_name NVARCHAR(150) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    source NVARCHAR(50) NULL,
    status VARCHAR(20) NOT NULL CONSTRAINT df_leads_status DEFAULT 'NEW',
    assigned_sales_id UNIQUEIDENTIFIER NULL,
    converted_user_id UNIQUEIDENTIFIER NULL,
    notes NVARCHAR(500) NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT df_leads_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NULL,
    CONSTRAINT pk_leads PRIMARY KEY (id),
    CONSTRAINT fk_leads_assigned_sales FOREIGN KEY (assigned_sales_id) REFERENCES users(id),
    CONSTRAINT fk_leads_converted_user FOREIGN KEY (converted_user_id) REFERENCES users(id)
);

CREATE INDEX idx_leads_status ON leads(status);
CREATE INDEX idx_leads_phone ON leads(phone);
CREATE INDEX idx_leads_assigned_sales ON leads(assigned_sales_id);
