/** Tên vai trò đúng như backend trả về trong AuthResponse.roles (không có tiền tố ROLE_). */
export const ROLES = ["SUPER_ADMIN", "RECEPTIONIST", "SALES", "TRAINER", "MEMBER"] as const;
export type Role = (typeof ROLES)[number];

export const ROLE_LABELS: Record<Role, string> = {
  SUPER_ADMIN: "Quản trị viên",
  RECEPTIONIST: "Lễ tân",
  SALES: "Nhân viên kinh doanh",
  TRAINER: "Huấn luyện viên",
  MEMBER: "Hội viên",
};

export function hasAnyRole(userRoles: readonly string[], allowed: readonly Role[] | undefined): boolean {
  if (!allowed || allowed.length === 0) return true;
  return allowed.some((r) => userRoles.includes(r));
}
