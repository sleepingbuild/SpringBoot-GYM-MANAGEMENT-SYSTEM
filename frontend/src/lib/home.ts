import type { Role } from "./roles";

/** Trang đầu tiên sau đăng nhập theo vai trò (không còn trang Tổng quan). Vai trò cao hơn được xét trước. */
const HOME_BY_ROLE: [Role, string][] = [
  ["SUPER_ADMIN", "/admin/packages"],
  ["RECEPTIONIST", "/bookings"],
  ["SALES", "/leads"],
  ["TRAINER", "/schedule"],
  ["MEMBER", "/my-membership"],
];

export function homePathFor(roles: readonly string[]): string {
  return HOME_BY_ROLE.find(([r]) => roles.includes(r))?.[1] ?? "/profile";
}
