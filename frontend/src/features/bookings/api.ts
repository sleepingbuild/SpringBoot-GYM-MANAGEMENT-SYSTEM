import { apiFetch } from "@/lib/api/client";
import type { Booking } from "@/features/schedule/bookingTypes";
import type { AvailableSlot, AvailableTrainer, CreateBookingInput } from "./types";

/** Cac PT con it nhat mot slot trong trong ngay (hoc vien, le tan, admin). */
export const listTrainersWithSlots = (date: string) =>
  apiFetch<AvailableTrainer[]>("/api/v1/trainer-schedules/trainers", { query: { date } });

export const listAvailableSlots = (trainerId: string, date: string) =>
  apiFetch<AvailableSlot[]>("/api/v1/trainer-schedules/available", { query: { trainerId, date } });

export const createBooking = (body: CreateBookingInput) =>
  apiFetch<Booking>("/api/v1/bookings", { method: "POST", body });

/**
 * Cac buoi trong mot ngay, moi chi nhanh (le tan, admin). Khong gui branchId.
 * Backend tra danh sach; neu sau nay doi sang dang phan trang (content[]) thi van doc duoc.
 */
export async function listBookingsByDate(date: string): Promise<Booking[]> {
  const data = await apiFetch<unknown>("/api/v1/bookings", { query: { date } });
  if (Array.isArray(data)) return data as Booking[];
  const content = (data as { content?: unknown } | null)?.content;
  return Array.isArray(content) ? (content as Booking[]) : [];
}
