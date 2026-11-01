import { ApiError, errorMessage } from "@/lib/api/errors";

/**
 * Thông báo tiếng Việt cho mã lỗi của module gói tập và thanh toán.
 * Backend có thể gửi `message` bằng đúng tên mã lỗi (ví dụ "PACKAGE_NOT_FOUND") khi không có lời giải thích riêng,
 * nên ở đây coi đó là "chưa có message" và dùng bảng dưới thay vì hiện tên mã cho người dùng.
 */
const MESSAGES: Record<string, string> = {
  PACKAGE_NOT_FOUND: "Không tìm thấy gói tập, hoặc gói đã ngừng bán.",
  MEMBERSHIP_NOT_FOUND: "Chưa có gói tập đang hoạt động.",
  MEMBERSHIP_EXPIRED: "Gói tập đã hết hạn.",
  MEMBERSHIP_NO_SESSION: "Gói tập đã hết buổi.",
  MEMBERSHIP_PENDING_EXISTS: "Đang có một gói chờ thanh toán. Hoàn tất thanh toán gói đó trước khi đăng ký thêm.",
  PAYMENT_NOT_FOUND: "Không tìm thấy giao dịch thanh toán.",
  PAYMENT_DUPLICATE_TRANSACTION: "Trùng mã giao dịch. Thử lại.",
  PAYMENT_NOT_REFUNDABLE: "Giao dịch này không thể hoàn tiền.",
  VALIDATION_ERROR: "Dữ liệu chưa hợp lệ.",
  DATA_CONFLICT: "Dữ liệu bị trùng hoặc vi phạm ràng buộc.",
  AUTH_FORBIDDEN_ROLE: "Bạn không có quyền thực hiện thao tác này.",
  RESOURCE_NOT_FOUND: "Không tìm thấy dữ liệu yêu cầu.",
};

export function membershipErrorMessage(err: unknown, fallback = "Đã có lỗi xảy ra. Thử lại sau."): string {
  if (err instanceof ApiError && err.kind !== "network" && err.kind !== "session") {
    const code = err.errorCode;
    const hasOwnMessage = err.message !== "" && err.message !== code;
    if (hasOwnMessage) return err.message; // lời giải thích tiếng Việt do backend gửi
    if (code && MESSAGES[code]) return MESSAGES[code];
    return fallback;
  }
  return errorMessage(err, fallback);
}
