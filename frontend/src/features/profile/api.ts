import { apiFetch } from "@/lib/api/client";
import type { Profile, UpdateProfileRequest } from "@/lib/types";

export const getMyProfile = () => apiFetch<Profile>("/api/v1/profile/me");

export const updateMyProfile = (body: UpdateProfileRequest) =>
  apiFetch<Profile>("/api/v1/profile/me", { method: "PUT", body });
