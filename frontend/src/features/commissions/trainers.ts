import type { Trainer } from "./types";

/** Tên hiển thị của PT theo id; không có trong danh sách (PT đã nghỉ hoặc chưa tải) thì rút gọn mã. */
export function trainerLabel(trainers: Trainer[], id: string): string {
  return trainers.find((t) => t.id === id)?.fullName ?? `PT ${id.slice(0, 8)}`;
}

/** Sắp xếp theo tên tiếng Việt để danh sách chọn dễ tìm. */
export const sortTrainers = (trainers: Trainer[]): Trainer[] =>
  [...trainers].sort((a, b) => a.fullName.localeCompare(b.fullName, "vi"));
