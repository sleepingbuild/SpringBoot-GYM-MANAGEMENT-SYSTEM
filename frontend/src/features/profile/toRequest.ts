import type { UpdateProfileRequest } from "@/lib/types";

export interface ProfileFormValues {
  dateOfBirth: string;
  weightKg: string;
  heightCm: string;
  goal: string;
}

function num(v: string): number | null {
  const t = v.trim().replace(",", ".");
  if (t === "") return null;
  const n = Number(t);
  return Number.isFinite(n) ? n : null;
}

/** Chuỗi trống thành null để backend lưu "chưa có", không gửi chuỗi rỗng hay NaN. */
export function toRequest(v: ProfileFormValues): UpdateProfileRequest {
  return {
    dateOfBirth: v.dateOfBirth.trim() === "" ? null : v.dateOfBirth,
    weightKg: num(v.weightKg),
    heightCm: num(v.heightCm),
    goal: v.goal.trim() === "" ? null : v.goal.trim(),
  };
}
