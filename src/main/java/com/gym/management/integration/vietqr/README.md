# integration/vietqr

Sở hữu: **Agent 4** (Payment & POS Module).

Đặt tại đây:
- `VietQrClient.java` — gọi API sinh mã QR động (VietQR.io hoặc ngân hàng đối tác)
- `MomoClient.java` — gọi API MoMo (tạo đơn, xác thực chữ ký)
- `dto/` riêng cho request/response của cổng thanh toán (không tái sử dụng DTO nghiệp vụ nội bộ)
- `WebhookSignatureVerifier.java` — xác thực HMAC signature từ webhook

Cấu hình liên quan: `gms.vietqr.*`, `gms.momo.*` trong `application.yml.example`.
