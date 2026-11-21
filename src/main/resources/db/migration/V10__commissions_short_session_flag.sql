-- Agent 5: ghi nhan buoi PT day ngan (ra truoc gio ket thuc) nhung co ly do giai trinh. Chi de hien thi,
-- khong doi so tien luong. Du lieu cu mac dinh 0.
ALTER TABLE commissions ADD short_session_with_reason BIT NOT NULL
    CONSTRAINT df_commissions_short_session DEFAULT 0;
