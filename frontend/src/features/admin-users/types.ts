import type { Role } from "@/lib/roles";

/** AdminUserResponse.java: tài khoản hiển thị cho Super Admin (không có mật khẩu). */
export interface AdminUser {
  id: string;
  fullName: string;
  email: string;
  phone: string | null;
  status: "ACTIVE" | "INACTIVE" | string;
  roles: Role[];
}

export type UserGroup = "CUSTOMER" | "STAFF";
export type StatusFilter = "" | "ACTIVE" | "INACTIVE";

export interface StaffFormValues {
  fullName: string;
  email: string;
  phone: string;
  password: string;
  roles: Role[];
}
