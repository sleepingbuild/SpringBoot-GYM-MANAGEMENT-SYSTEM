import { apiFetch } from "@/lib/api/client";
import type { Booking } from "./bookingTypes";

const BASE = "/api/v1/bookings";

/**
 * Cac buoi cua PT dang dang nhap. Backend tra danh sach (khong tham so loc);
 * neu sau nay doi sang dang phan trang (content[]) thi van doc duoc.
 */
export async function listMyBookings(): Promise<Booking[]> {
  const data = await apiFetch<unknown>(`${BASE}/me`);
  if (Array.isArray(data)) return data as Booking[];
  const content = (data as { content?: unknown } | null)?.content;
  return Array.isArray(content) ? (content as Booking[]) : [];
}

export const confirmBooking = (id: string) => apiFetch<Booking>(`${BASE}/${id}/confirm`, { method: "POST" });

export const completeBooking = (id: string) => apiFetch<Booking>(`${BASE}/${id}/complete`, { method: "POST" });

export const cancelBooking = (id: string) => apiFetch<Booking>(`${BASE}/${id}/cancel`, { method: "POST" });

export const submitEarlyLeaveReason = (id: string, reason: string) =>
  apiFetch<Booking>(`${BASE}/${id}/early-leave-reason`, { method: "PUT", body: { reason } });
