/** Lỗi do chính hook tạo ra (không phải lỗi của trình duyệt). */
export class CameraSetupError extends Error {
  readonly code: "insecure" | "models";

  constructor(code: "insecure" | "models", message: string) {
    super(message);
    this.name = "CameraSetupError";
    this.code = code;
  }
}

/** Chữ hiển thị cho người dùng khi không bật được camera hoặc không tải được mô hình. */
export function cameraErrorMessage(e: unknown): string {
  if (e instanceof CameraSetupError) return e.message;
  const name = typeof e === "object" && e !== null && "name" in e ? String((e as { name: unknown }).name) : "";
  switch (name) {
    case "NotAllowedError":
    case "SecurityError":
      return "Bạn chưa cho phép dùng camera. Bấm vào biểu tượng ổ khóa trên thanh địa chỉ, cho phép camera rồi thử lại.";
    case "NotFoundError":
    case "OverconstrainedError":
      return "Không tìm thấy camera trên thiết bị này.";
    case "NotReadableError":
      return "Camera đang được ứng dụng khác sử dụng. Đóng ứng dụng đó rồi thử lại.";
    default:
      return "Không bật được camera. Thử tải lại trang.";
  }
}
