import type { AvailableSlot, CreateBookingInput } from "./types";

export interface BuildCreateInputParams {
  slot: AvailableSlot;
  /** true khi le tan hoac admin dat ho. */
  staff: boolean;
  memberId: string | null;
  /** Chuoi rong nghia la khong chon chi nhanh. */
  branchId: string;
  notes: string;
}

/**
 * Dung body POST /bookings.
 * Hoi vien tu dat: khong bao gio gui memberId (backend lay tu nguoi dang nhap).
 * Chi nhanh va ghi chu chi gui khi co noi dung.
 */
export function buildCreateInput(p: BuildCreateInputParams): CreateBookingInput {
  const input: CreateBookingInput = {
    trainerId: p.slot.trainerId,
    bookingDate: p.slot.date,
    startTime: p.slot.startTime,
    endTime: p.slot.endTime,
  };
  if (p.staff && p.memberId) input.memberId = p.memberId;
  if (p.branchId) input.branchId = p.branchId;
  const notes = p.notes.trim();
  if (notes) input.notes = notes;
  return input;
}

/** Le tan, admin phai chon hoi vien truoc khi dat. Tra ve cau nhac, hoac null neu da du. */
export function missingMemberHint(staff: boolean, memberId: string | null): string | null {
  if (!staff || memberId) return null;
  return "Chọn hội viên trước khi chọn khung giờ.";
}
