import { apiFetch } from "@/lib/api/client";
import type { Lead } from "./types";

/** SalesUserResponse.java: Sales đang hoạt động, để Super Admin chọn người phụ trách. */
export interface SalesUser {
  id: string;
  fullName: string;
  phone?: string | null;
}

export const listSalesUsers = (signal?: AbortSignal) => apiFetch<SalesUser[]>("/api/v1/leads/sales-users", { signal });

/** salesId = null nghĩa là bỏ giao (lead về trạng thái chưa gán). */
export const assignLead = (id: string, salesId: string | null) =>
  apiFetch<Lead>(`/api/v1/leads/${id}/assign`, { method: "PATCH", body: { salesId } });
