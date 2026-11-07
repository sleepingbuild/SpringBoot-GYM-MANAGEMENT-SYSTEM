"use client";

import { useAuth } from "@/lib/auth/AuthContext";
import { hasAnyRole } from "@/lib/roles";
import { DailyAttendanceReport } from "./DailyAttendanceReport";
import { MyAttendance } from "./MyAttendance";

/** Quản trị viên xem báo cáo theo ngày; lễ tân và Sales xem bảng của chính mình. */
export function AttendanceView() {
  const { user } = useAuth();
  if (user && hasAnyRole(user.roles, ["SUPER_ADMIN"])) {
    return <DailyAttendanceReport />;
  }
  return <MyAttendance />;
}
