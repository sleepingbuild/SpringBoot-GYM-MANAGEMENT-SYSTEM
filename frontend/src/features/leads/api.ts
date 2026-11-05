import { apiFetch } from "@/lib/api/client";
import type { PageResponse } from "@/lib/types";
import type { ConvertInput, ExpiringMember, Lead, LeadInput, LeadStatus } from "./types";

export interface ListParams {
  status: LeadStatus | "";
  keyword: string;
  page: number;
  size: number;
  sort: string;
}

export const listLeads = (p: ListParams, signal?: AbortSignal) =>
  apiFetch<PageResponse<Lead>>("/api/v1/leads", {
    query: { status: p.status, keyword: p.keyword, page: p.page, size: p.size, sort: p.sort },
    signal,
  });

function toBody(v: LeadInput) {
  return {
    fullName: v.fullName.trim(),
    phone: v.phone.trim(),
    source: v.source.trim() || null,
    notes: v.notes.trim() || null,
  };
}

export const createLead = (v: LeadInput) => apiFetch<Lead>("/api/v1/leads", { method: "POST", body: toBody(v) });

export const updateLead = (id: string, v: LeadInput) =>
  apiFetch<Lead>(`/api/v1/leads/${id}`, { method: "PUT", body: toBody(v) });

export const changeLeadStatus = (id: string, status: "CONTACTED" | "LOST") =>
  apiFetch<Lead>(`/api/v1/leads/${id}/status`, { method: "PATCH", body: { status } });

/** password để trống thì không gửi, backend sẽ sinh mật khẩu ngẫu nhiên và trả temporaryPassword. */
export const convertLead = (id: string, v: ConvertInput) =>
  apiFetch<Lead>(`/api/v1/leads/${id}/convert`, {
    method: "POST",
    body: { email: v.email.trim(), ...(v.password ? { password: v.password } : {}) },
  });

export const listExpiringMembers = (days: number, signal?: AbortSignal) =>
  apiFetch<ExpiringMember[]>("/api/v1/leads/expiring-soon", { query: { days }, signal });
