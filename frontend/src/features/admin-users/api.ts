import { apiFetch } from "@/lib/api/client";
import type { PageResult } from "@/features/bookings/types";
import type { Role } from "@/lib/roles";
import type { AdminUser, StaffFormValues, StatusFilter, UserGroup } from "./types";

const BASE = "/api/v1/admin/users";

export function listUsers(group: UserGroup, q: string, status: StatusFilter, page: number, signal?: AbortSignal) {
  return apiFetch<PageResult<AdminUser>>(BASE, { query: { group, q: q.trim(), status, page, size: 20 }, signal });
}

export function createStaff(values: StaffFormValues) {
  return apiFetch<AdminUser>(BASE, {
    method: "POST",
    body: {
      fullName: values.fullName.trim(),
      email: values.email.trim(),
      phone: values.phone.trim() || null,
      password: values.password,
      roles: values.roles,
    },
  });
}

export function updateUser(id: string, fullName: string, phone: string) {
  return apiFetch<AdminUser>(`${BASE}/${id}`, { method: "PUT", body: { fullName: fullName.trim(), phone: phone.trim() || null } });
}

export function setUserRoles(id: string, roles: Role[]) {
  return apiFetch<AdminUser>(`${BASE}/${id}/roles`, { method: "PUT", body: { roles } });
}

export function setUserStatus(id: string, status: "ACTIVE" | "INACTIVE") {
  return apiFetch<AdminUser>(`${BASE}/${id}/status`, { method: "PUT", body: { status } });
}
