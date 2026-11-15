import type { TrainerChoice, TrainerOption } from "./types";

/** Backend tra HH:mm:ss; chi hien HH:mm. Khong cong tru mui gio. */
export const hhmm = (t: string) => t.slice(0, 5);

/** Cuoi khung ngoai gio la 23:59 (cot time khong luu duoc 24:00), nen hien thi la 24:00. */
export function displayEnd(t: string): string {
  const s = hhmm(t);
  return s === "23:59" ? "24:00" : s;
}

export const slotLabel = (start: string, end: string) => `${hhmm(start)} - ${displayEnd(end)}`;

/** Cau thong bao sau khi dat xong, theo trang thai backend tra ve (CONFIRMED: trong ca; PENDING: ngoai gio). */
export function bookedMessage(status: string, staff: boolean): string {
  if (status === "CONFIRMED") {
    return staff ? "Đã đặt lịch hộ hội viên và buổi đã được xác nhận." : "Đã đặt lịch và buổi đã được xác nhận.";
  }
  return staff
    ? "Đã đặt lịch hộ hội viên. Buổi sẽ được xác nhận khi huấn luyện viên đồng ý."
    : "Đã gửi yêu cầu đặt lịch. Buổi sẽ được xác nhận khi huấn luyện viên đồng ý.";
}

/** Danh sach PT cho man dat ngoai gio: xep theo ten (tieng Viet). */
export function toTrainerChoices(list: TrainerOption[]): TrainerChoice[] {
  return list
    .map((t) => ({ trainerId: t.id, trainerName: t.fullName }))
    .sort((a, b) => (a.trainerName ?? "").localeCompare(b.trainerName ?? "", "vi"));
}
