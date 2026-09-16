# integration/hardware

Sở hữu: **Agent 3** (Check-in Engine & Access Control).

Đặt tại đây:
- `HardwareGatewayClient.java` — interface chung cho mọi loại đầu đọc (QR/RFID/FaceID)
- `QrGatewayClient.java`, `RfidGatewayClient.java`, `FaceIdGatewayClient.java` — implementation
  cụ thể (mock trước, tích hợp SDK thật sau khi có thiết bị)

Nguyên tắc: `CheckInService` chỉ phụ thuộc `HardwareGatewayClient` (interface),
không phụ thuộc trực tiếp implementation cụ thể, để dễ mock trong test và dễ
đổi nhà cung cấp phần cứng sau này mà không sửa business logic.
