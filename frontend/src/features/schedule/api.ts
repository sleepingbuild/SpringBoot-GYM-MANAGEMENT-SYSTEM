import { apiFetch } from "@/lib/api/client";
import type { TrainerOption, TrainerSchedule, TrainerScheduleInput } from "./types";

const BASE = "/api/v1/trainer-schedules";

/**
 * Lich lam viec trong khoang [from, to] (yyyy-MM-dd).
 * Khong truyen trainerId: lich cua chinh nguoi dang dang nhap (PT).
 * Truyen trainerId: lich cua PT do (Super Admin).
 * Backend tra danh sach; neu sau nay doi sang dang phan trang (content[]) thi van doc duoc.
 */
export async function listSchedules(from: string, to: string, trainerId?: string): Promise<TrainerSchedule[]> {
  const query: Record<string, string> = { from, to };
  if (trainerId) query.trainerId = trainerId;
  const data = await apiFetch<unknown>(BASE, { query });
  if (Array.isArray(data)) return data as TrainerSchedule[];
  const content = (data as { content?: unknown } | null)?.content;
  return Array.isArray(content) ? (content as TrainerSchedule[]) : [];
}

/** Lich cua chinh PT dang dang nhap. */
export const listMySchedules = (from: string, to: string) => listSchedules(from, to);

export const createSchedule = (body: TrainerScheduleInput) =>
  apiFetch<TrainerSchedule>(BASE, { method: "POST", body });

export const updateSchedule = (id: string, body: TrainerScheduleInput) =>
  apiFetch<TrainerSchedule>(`${BASE}/${id}`, { method: "PUT", body });

export const deleteSchedule = (id: string) => apiFetch<unknown>(`${BASE}/${id}`, { method: "DELETE" });

/** Huan luyen vien dang hoat dong (Agent 1). Ai dang nhap cung goi duoc. */
export const listTrainers = () => apiFetch<TrainerOption[]>("/api/v1/trainers");
