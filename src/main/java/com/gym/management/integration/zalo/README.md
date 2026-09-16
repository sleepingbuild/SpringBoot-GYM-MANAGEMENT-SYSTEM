# integration/zalo

Sở hữu: **Agent 6** (Notification, Leads, Dashboard & Open API).

Đặt tại đây:
- `ZaloZnsClient.java` — gửi Zalo ZNS theo template đã duyệt
- `SmsBrandnameClient.java` — gửi SMS Brandname (fallback khi ZNS lỗi)
- Interface chung `NotificationSender` nên đặt ở `service/` để `ReminderScheduler`
  không phụ thuộc trực tiếp implementation cụ thể (dễ đổi nhà cung cấp sau này).

Cấu hình liên quan: `gms.zalo-zns.*`, `gms.sms-brandname.*` trong `application.yml.example`.
