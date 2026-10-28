"use client";

import { useAuth } from "@/lib/auth/AuthContext";
import { type Role, hasAnyRole } from "@/lib/roles";
import { Alert } from "../ui/Alert";

/**
 * Bao quanh nội dung trang chỉ dành cho một số vai trò. Đây chỉ là lớp giao diện;
 * backend mới là nơi phân quyền thật (trả AUTH_FORBIDDEN_ROLE).
 */
export function RequireRole({ roles, children }: { roles: Role[]; children: React.ReactNode }) {
  const { user } = useAuth();
  if (!user || !hasAnyRole(user.roles, roles)) {
    return <Alert tone="warning" title="Bạn không có quyền xem trang này">Tài khoản hiện tại không thuộc vai trò được phép.</Alert>;
  }
  return <>{children}</>;
}
