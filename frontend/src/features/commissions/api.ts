import { apiFetch } from "@/lib/api/client";
import type { PageResponse } from "@/lib/types";
import type { Commission, TrainerPayRate } from "./types";

const COMMISSIONS = "/api/v1/commissions";
const RATES = "/api/v1/trainer-pay-rates";

/** PT xem lương của chính mình. */
export const listMyCommissions = (page = 0, size = 20) =>
  apiFetch<PageResponse<Commission>>(`${COMMISSIONS}/me`, { query: { page, size } });

/** Super Admin tra cứu; month dạng yyyy-MM (bỏ trống = mọi tháng), staffId là UUID của PT. */
export const searchCommissions = (params: { staffId?: string; month?: string; page?: number; size?: number }) =>
  apiFetch<PageResponse<Commission>>(COMMISSIONS, { query: { page: 0, size: 20, ...params } });

export const markCommissionPaid = (id: string) => apiFetch<Commission>(`${COMMISSIONS}/${id}/mark-paid`, { method: "POST" });

export const getPayRate = (trainerId: string) => apiFetch<TrainerPayRate>(`${RATES}/${trainerId}`);

export const setPayRate = (trainerId: string, amountPerSession: number) =>
  apiFetch<TrainerPayRate>(`${RATES}/${trainerId}`, { method: "PUT", body: { amountPerSession } });
