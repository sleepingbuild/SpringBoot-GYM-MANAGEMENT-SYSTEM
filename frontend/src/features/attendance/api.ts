import { apiFetch } from "@/lib/api/client";
import type { CheckInHistoryItem } from "./types";

/** GET /api/v1/check-ins/me: toàn bộ lịch sử của hội viên, mới nhất trước (chưa phân trang). */
export async function getMyCheckIns(signal?: AbortSignal): Promise<CheckInHistoryItem[]> {
  const data = await apiFetch<CheckInHistoryItem[] | undefined>("/api/v1/check-ins/me", { signal });
  return data ?? [];
}
