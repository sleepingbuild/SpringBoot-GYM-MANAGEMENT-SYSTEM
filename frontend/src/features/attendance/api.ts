import { apiFetch } from "@/lib/api/client";
import type { CheckInHistoryItem, StaffAttendanceItem } from "./types";

/** GET /api/v1/check-ins/me: toàn bộ lịch sử của hội viên, mới nhất trước (chưa phân trang). */
export async function getMyCheckIns(signal?: AbortSignal): Promise<CheckInHistoryItem[]> {
  const data = await apiFetch<CheckInHistoryItem[] | undefined>("/api/v1/check-ins/me", { signal });
  return data ?? [];
}

/** GET /api/v1/staff-attendance/me (Lễ tân, Sales): lịch sử chấm công của chính mình, ngày mới nhất trước. */
export async function getMyAttendance(signal?: AbortSignal): Promise<StaffAttendanceItem[]> {
  const data = await apiFetch<StaffAttendanceItem[] | undefined>("/api/v1/staff-attendance/me", { signal });
  return data ?? [];
}

/** GET /api/v1/staff-attendance?date=yyyy-MM-dd (Quản trị viên): chấm công toàn bộ nhân sự trong ngày. */
export async function getAttendanceByDate(date: string, signal?: AbortSignal): Promise<StaffAttendanceItem[]> {
  const data = await apiFetch<StaffAttendanceItem[] | undefined>("/api/v1/staff-attendance", {
    query: { date },
    signal,
  });
  return data ?? [];
}
