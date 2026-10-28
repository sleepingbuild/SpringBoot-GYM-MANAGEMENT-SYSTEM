import type { Role } from "./roles";

/** Vỏ chuẩn của mọi response từ backend (ApiResponse.java). */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data?: T;
  errorCode?: string;
  timestamp?: string;
}

/** AuthResponse.java */
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  email: string;
  fullName: string;
  roles: Role[];
}

export interface SessionUser {
  email: string;
  fullName: string;
  roles: Role[];
}

/** ProfileResponse.java. dateOfBirth dạng yyyy-MM-dd; age do backend tính sẵn. */
export interface Profile {
  userId: string;
  fullName: string;
  email: string;
  dateOfBirth: string | null;
  age: number | null;
  weightKg: number | null;
  heightCm: number | null;
  goal: string | null;
  avatarUrl: string | null;
}

/** UpdateProfileRequest.java */
export interface UpdateProfileRequest {
  dateOfBirth: string | null;
  weightKg: number | null;
  heightCm: number | null;
  goal: string | null;
}

/**
 * PageResponse.java - TODO(Agent 1): đối chiếu với file thật trước khi các agent dùng.
 * Tạm giả định các trường phổ biến; sai thì sửa ở đây một chỗ.
 */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
