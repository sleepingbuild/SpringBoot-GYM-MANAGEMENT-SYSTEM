import { apiFetch } from "@/lib/api/client";
import type { CommissionsReport, DateRange, MonthRange, RenewalReport, RevenueGroupBy, RevenueReport, TopPackagesReport, TrafficReport } from "./types";

export const getRevenue = (r: DateRange, groupBy: RevenueGroupBy, signal?: AbortSignal) =>
  apiFetch<RevenueReport>("/api/v1/reports/revenue", { query: { from: r.from, to: r.to, groupBy }, signal });

export const getRenewalRate = (r: DateRange, windowDays: number, signal?: AbortSignal) =>
  apiFetch<RenewalReport>("/api/v1/reports/renewal-rate", { query: { from: r.from, to: r.to, windowDays }, signal });

export const getCheckinTraffic = (r: DateRange, signal?: AbortSignal) =>
  apiFetch<TrafficReport>("/api/v1/reports/checkin-traffic", { query: { from: r.from, to: r.to }, signal });

export const getTopPackages = (r: DateRange, limit: number, signal?: AbortSignal) =>
  apiFetch<TopPackagesReport>("/api/v1/reports/top-packages", { query: { from: r.from, to: r.to, limit }, signal });

export const getCommissionsSummary = (r: MonthRange, signal?: AbortSignal) =>
  apiFetch<CommissionsReport>("/api/v1/reports/commissions-summary", { query: { fromMonth: r.from, toMonth: r.to }, signal });
