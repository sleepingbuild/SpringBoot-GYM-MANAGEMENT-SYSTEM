import type { LeadStatus } from "./types";

export const STATUS_LABELS: Record<LeadStatus, string> = {
  NEW: "Mới",
  CONTACTED: "Đã liên hệ",
  CONVERTED: "Đã thành hội viên",
  LOST: "Đã mất",
};

/** Bảng chuyển trạng thái khớp backend (LeadStatus.java). CONVERTED và LOST là trạng thái cuối. */
const NEXT: Record<LeadStatus, readonly LeadStatus[]> = {
  NEW: ["CONTACTED", "LOST", "CONVERTED"],
  CONTACTED: ["LOST", "CONVERTED"],
  CONVERTED: [],
  LOST: [],
};

/** Các trạng thái có thể đặt bằng PATCH /status. CONVERTED chỉ đi qua /convert nên không nằm ở đây. */
export function allowedStatusChanges(status: LeadStatus): Array<"CONTACTED" | "LOST"> {
  return NEXT[status].filter((s): s is "CONTACTED" | "LOST" => s === "CONTACTED" || s === "LOST");
}

export function canConvert(status: LeadStatus): boolean {
  return NEXT[status].includes("CONVERTED");
}

/** Lead đã chuyển thành hội viên thì backend không cho sửa nữa. */
export function canEdit(status: LeadStatus): boolean {
  return status !== "CONVERTED";
}
