import { ApiError, errorMessage } from "@/lib/api/errors";

const LEAD_MESSAGES: Record<string, string> = {
  LEAD_NOT_FOUND: "Không tìm thấy khách tiềm năng này. Có thể lead đã được người khác xử lý.",
  LEAD_ALREADY_CONVERTED: "Khách này đã được chuyển thành hội viên.",
  LEAD_INVALID_STATUS_TRANSITION: "Không thể chuyển sang trạng thái này.",
  AUTH_EMAIL_ALREADY_EXISTS: "Email đã được sử dụng.",
  AUTH_PHONE_ALREADY_EXISTS: "Số điện thoại đã được sử dụng bởi một tài khoản khác.",
  DATA_CONFLICT: "Dữ liệu bị trùng hoặc vi phạm ràng buộc.",
};

/** Ưu tiên bảng thông báo của module Leads theo errorCode, còn lại dùng thông báo chung. */
export function leadErrorMessage(err: unknown): string {
  if (err instanceof ApiError && err.errorCode && LEAD_MESSAGES[err.errorCode]) {
    return LEAD_MESSAGES[err.errorCode];
  }
  return errorMessage(err);
}

export function isAbort(err: unknown): boolean {
  return err instanceof DOMException && err.name === "AbortError";
}
