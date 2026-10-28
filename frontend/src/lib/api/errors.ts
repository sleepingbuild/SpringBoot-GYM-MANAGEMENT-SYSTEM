export type ApiErrorKind =
  | "session" //   phiên đăng nhập không hợp lệ/hết hạn (đã thử làm mới token mà không được)
  | "forbidden" // đã đăng nhập nhưng không đủ quyền (AUTH_FORBIDDEN_ROLE)
  | "validation" // 400 dữ liệu không hợp lệ
  | "notFound" //   404
  | "conflict" //   409 trùng dữ liệu
  | "network" //    không kết nối được máy chủ
  | "server" //     5xx
  | "other"; //     còn lại (kể cả 401 có errorCode riêng như FACE_NOT_RECOGNIZED)

/** Lỗi chuẩn hoá từ backend. Xử lý theo `errorCode`, không theo `status`. */
export class ApiError extends Error {
  readonly kind: ApiErrorKind;
  readonly status: number;
  readonly errorCode?: string;

  constructor(kind: ApiErrorKind, status: number, message: string, errorCode?: string) {
    super(message);
    this.name = "ApiError";
    this.kind = kind;
    this.status = status;
    this.errorCode = errorCode;
  }

  is(code: string): boolean {
    return this.errorCode === code;
  }
}

/** Thông báo mặc định cho mã lỗi dùng chung khi server không kèm message. Mỗi module tự thêm bảng riêng ở features/<module>. */
const DEFAULT_MESSAGES: Record<string, string> = {
  VALIDATION_ERROR: "Dữ liệu chưa hợp lệ.",
  DATA_CONFLICT: "Dữ liệu bị trùng hoặc vi phạm ràng buộc.",
  AUTH_INVALID_CREDENTIALS: "Email hoặc mật khẩu không đúng.",
  AUTH_FORBIDDEN_ROLE: "Bạn không có quyền thực hiện thao tác này.",
  AUTH_EMAIL_ALREADY_EXISTS: "Email đã được sử dụng.",
  AUTH_PHONE_ALREADY_EXISTS: "Số điện thoại đã được sử dụng.",
  PROFILE_INVALID_AGE: "Ngày sinh không hợp lệ: cần từ 18 tuổi trở lên tính đến ngày đăng ký.",
  INTERNAL_SERVER_ERROR: "Máy chủ gặp lỗi. Thử lại sau.",
};

export function kindFromResponse(status: number, errorCode?: string): ApiErrorKind {
  if (errorCode === "AUTH_FORBIDDEN_ROLE") return "forbidden";
  if (status >= 500) return "server";
  if (status === 404) return "notFound";
  if (status === 409) return "conflict";
  if (status === 400) return "validation";
  return "other";
}

/** Chữ để hiện cho người dùng. Ưu tiên message từ server (tiếng Việt), rồi tới bảng mặc định. */
export function errorMessage(err: unknown, fallback = "Đã có lỗi xảy ra. Thử lại sau."): string {
  if (err instanceof ApiError) {
    if (err.kind === "network") return "Không kết nối được máy chủ. Kiểm tra backend đã chạy chưa.";
    if (err.kind === "session") return "Phiên đăng nhập đã hết hạn. Đăng nhập lại để tiếp tục.";
    if (err.message) return err.message;
    if (err.errorCode && DEFAULT_MESSAGES[err.errorCode]) return DEFAULT_MESSAGES[err.errorCode];
  }
  return fallback;
}
