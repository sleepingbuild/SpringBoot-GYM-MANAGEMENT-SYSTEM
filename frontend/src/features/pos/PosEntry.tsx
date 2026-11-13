"use client";

import { useAuth } from "@/lib/auth/AuthContext";
import { hasAnyRole } from "@/lib/roles";
import { MemberPosWorkspace } from "./MemberPosWorkspace";
import { PosWorkspace } from "./PosWorkspace";

/** Nhân viên (Lễ tân, Admin) thấy quầy POS; hội viên thấy cửa hàng tự mua. Nhân viên được ưu tiên nếu có cả hai vai trò. */
export function PosEntry() {
  const { user } = useAuth();
  const isStaff = !!user && hasAnyRole(user.roles, ["RECEPTIONIST", "SUPER_ADMIN"]);
  return isStaff ? <PosWorkspace /> : <MemberPosWorkspace />;
}
