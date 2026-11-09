import { apiFetch } from "@/lib/api/client";
import { ApiError } from "@/lib/api/errors";
import type { PageResponse } from "@/lib/types";
import type {
  Branch,
  MemberPackage,
  MemberSummary,
  PackageItem,
  PackagePurchase,
  PackageRequestBody,
  PaymentTransaction,
} from "./types";

interface Opts {
  signal?: AbortSignal;
}

// ---- Gói tập (công khai cho người đã đăng nhập) ----

export function listPackages(page = 0, size = 12, opts: Opts = {}) {
  return apiFetch<PageResponse<PackageItem>>("/api/v1/packages", { query: { page, size }, ...opts });
}

export function getPackage(id: string, opts: Opts = {}) {
  return apiFetch<PackageItem>(`/api/v1/packages/${encodeURIComponent(id)}`, opts);
}

// ---- Quản trị gói (Super Admin) ----

export function listAllPackages(page = 0, size = 20, opts: Opts = {}) {
  return apiFetch<PageResponse<PackageItem>>("/api/v1/packages/admin", { query: { page, size }, ...opts });
}

export function createPackage(body: PackageRequestBody) {
  return apiFetch<PackageItem>("/api/v1/packages", { method: "POST", body });
}

export function updatePackage(id: string, body: PackageRequestBody) {
  return apiFetch<PackageItem>(`/api/v1/packages/${encodeURIComponent(id)}`, { method: "PUT", body });
}

/** Ngừng bán (soft delete, is_active = false), không xoá cứng. */
export function deactivatePackage(id: string) {
  return apiFetch<void>(`/api/v1/packages/${encodeURIComponent(id)}`, { method: "DELETE" });
}

// ---- Gói của hội viên ----

/** Gói ACTIVE của chính mình; null khi chưa có (backend trả MEMBERSHIP_NOT_FOUND). */
export async function getMyMembership(opts: Opts = {}): Promise<MemberPackage | null> {
  try {
    return await apiFetch<MemberPackage>("/api/v1/member-packages/me", opts);
  } catch (err) {
    if (err instanceof ApiError && err.is("MEMBERSHIP_NOT_FOUND")) return null;
    throw err;
  }
}

export function listMyMemberships(page = 0, size = 10, opts: Opts = {}) {
  return apiFetch<PageResponse<MemberPackage>>("/api/v1/member-packages/me/history", { query: { page, size }, ...opts });
}

/** Lễ tân, Admin xem gói ACTIVE của một hội viên (H2); null khi hội viên chưa có (backend trả MEMBERSHIP_NOT_FOUND). */
export async function getMemberCurrentMembership(userId: string, opts: Opts = {}): Promise<MemberPackage | null> {
  try {
    return await apiFetch<MemberPackage>("/api/v1/member-packages/current", { query: { userId }, ...opts });
  } catch (err) {
    if (err instanceof ApiError && err.is("MEMBERSHIP_NOT_FOUND")) return null;
    throw err;
  }
}

/** Lễ tân, Admin xem lịch sử gói (mọi trạng thái) của một hội viên (H2), phân trang. */
export function listMemberMemberships(userId: string, page = 0, size = 10, opts: Opts = {}) {
  return apiFetch<PageResponse<MemberPackage>>("/api/v1/member-packages", { query: { userId, page, size }, ...opts });
}

/** Danh sách chi nhánh đang hoạt động (Agent 1), mọi người đã đăng nhập đều xem được. */
export function listBranches(opts: Opts = {}) {
  return apiFetch<Branch[]>("/api/v1/branches", opts);
}

/** Hội viên tự mua (bỏ `userId`) hoặc Lễ tân, Admin bán hộ (truyền `userId` của hội viên). */
export function registerPackage(input: { packageId: string; userId?: string; branchId?: string }) {
  return apiFetch<PackagePurchase>("/api/v1/member-packages", { method: "POST", body: input });
}

export function upgradePackage(memberPackageId: string, input: { newPackageId: string; branchId?: string }) {
  return apiFetch<PackagePurchase>(`/api/v1/member-packages/${encodeURIComponent(memberPackageId)}/upgrade`, {
    method: "POST",
    body: input,
  });
}

export function downgradePackage(memberPackageId: string, input: { newPackageId: string; branchId?: string }) {
  return apiFetch<PackagePurchase>(`/api/v1/member-packages/${encodeURIComponent(memberPackageId)}/downgrade`, {
    method: "POST",
    body: input,
  });
}

// ---- Thanh toán ----

export function listMyPayments(page = 0, size = 10, opts: Opts = {}) {
  return apiFetch<PageResponse<PaymentTransaction>>("/api/v1/payments/me", { query: { page, size }, ...opts });
}

export interface PaymentFilter {
  from?: string; // yyyy-MM-dd
  to?: string;
  status?: string;
  userId?: string;
}

/** Lễ tân, Admin tra cứu giao dịch. Lọc `status` và `userId` do backend (H3) làm; màn hình vẫn lọc thêm `status` ở trình duyệt để đúng cả khi chạy với backend cũ. */
export function listPayments(filter: PaymentFilter, page = 0, size = 20, opts: Opts = {}) {
  return apiFetch<PageResponse<PaymentTransaction>>("/api/v1/payments", {
    query: { from: filter.from, to: filter.to, status: filter.status, userId: filter.userId, page, size },
    ...opts,
  });
}

/** Xác nhận đã thu tiền tại chỗ. Không gửi body nên giữ phương thức mặc định đã ghi lúc tạo giao dịch. */
export function confirmPayment(transactionId: string) {
  return apiFetch<PaymentTransaction>(`/api/v1/payments/${encodeURIComponent(transactionId)}/confirm`, { method: "POST" });
}

// ---- Tìm hội viên (Agent 1) ----

export function searchMembers(q: string, opts: Opts = {}) {
  return apiFetch<PageResponse<MemberSummary>>("/api/v1/users", { query: { role: "MEMBER", q, page: 0, size: 8 }, ...opts });
}
