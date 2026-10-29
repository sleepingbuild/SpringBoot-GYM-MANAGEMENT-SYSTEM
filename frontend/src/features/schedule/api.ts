import { apiFetch } from "@/lib/api/client";
import type { TrainerSchedule, TrainerScheduleInput } from "./types";

const BASE = "/api/v1/trainer-schedules";

/**
 * Lich lam viec cua chinh PT dang dang nhap trong khoang [from, to] (yyyy-MM-dd).
 * Backend tra danh sach; neu sau nay doi sang dang phan trang (content[]) thi van doc duoc.
 */
export async function listMySchedules(from: string, to: string): Promise<TrainerSchedule[]> {
  const data = await apiFetch<unknown>(BASE, { query: { from, to } });
  if (Array.isArray(data)) return data as TrainerSchedule[];
  const content = (data as { content?: unknown } | null)?.content;
  return Array.isArray(content) ? (content as TrainerSchedule[]) : [];
}

export const createSchedule = (body: TrainerScheduleInput) =>
  apiFetch<TrainerSchedule>(BASE, { method: "POST", body });

export const updateSchedule = (id: string, body: TrainerScheduleInput) =>
  apiFetch<TrainerSchedule>(`${BASE}/${id}`, { method: "PUT", body });

export const deleteSchedule = (id: string) => apiFetch<unknown>(`${BASE}/${id}`, { method: "DELETE" });
