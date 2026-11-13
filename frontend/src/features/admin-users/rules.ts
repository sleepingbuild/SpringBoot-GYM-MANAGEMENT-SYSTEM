import type { Role } from "@/lib/roles";
import type { StaffFormValues } from "./types";

/** Vai trò nhân viên (không gồm MEMBER). Khớp UserAdminServiceImpl.STAFF_ROLES ở backend. */
export const STAFF_ROLES: readonly Role[] = ["SUPER_ADMIN", "RECEPTIONIST", "TRAINER", "SALES"];

/**
 * Bật/tắt một vai trò trong danh sách đang chọn. MEMBER loại trừ vai trò nhân viên và ngược lại,
 * giống luật backend (MEMBER không kết hợp với vai trò nhân viên).
 */
export function toggleRole(selected: readonly Role[], role: Role): Role[] {
  if (selected.includes(role)) return selected.filter((r) => r !== role);
  if (role === "MEMBER") return ["MEMBER"];
  return [...selected.filter((r) => r !== "MEMBER"), role];
}

export type StaffFormErrors = Partial<Record<"fullName" | "email" | "phone" | "password" | "roles", string>>;

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** Kiểm tra form tạo nhân viên, cùng giới hạn với AdminUserCreateRequest ở backend. */
export function validateStaffForm(values: StaffFormValues): StaffFormErrors {
  const errors: StaffFormErrors = {};
  const fullName = values.fullName.trim();
  if (!fullName) errors.fullName = "Nhập họ tên.";
  else if (fullName.length > 150) errors.fullName = "Họ tên tối đa 150 ký tự.";

  const email = values.email.trim();
  if (!email) errors.email = "Nhập email.";
  else if (!EMAIL_PATTERN.test(email)) errors.email = "Email không hợp lệ.";
  else if (email.length > 150) errors.email = "Email tối đa 150 ký tự.";

  if (values.phone.trim().length > 20) errors.phone = "Số điện thoại tối đa 20 ký tự.";

  if (values.password.length < 8) errors.password = "Mật khẩu tối thiểu 8 ký tự.";
  else if (values.password.length > 72) errors.password = "Mật khẩu tối đa 72 ký tự.";

  if (values.roles.length === 0) errors.roles = "Chọn ít nhất một vai trò.";
  else if (values.roles.includes("MEMBER")) errors.roles = "Tài khoản nhân viên không dùng vai trò hội viên.";
  return errors;
}
