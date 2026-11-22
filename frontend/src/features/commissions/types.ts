import type { BadgeTone } from "@/components/ui/Badge";
/** Khớp CommissionResponse.java / TrainerPayRateResponse.java (Agent 5). */
export type CommissionOutcome = "COMPLETED" | "MEMBER_NO_SHOW" | "LATE_CANCEL";
export type CommissionStatus = "PENDING" | "PAID";

export const OUTCOME_LABEL: Record<CommissionOutcome, string> = {
  COMPLETED: "Dạy hoàn thành",
  MEMBER_NO_SHOW: "Hội viên vắng",
  LATE_CANCEL: "Hội viên huỷ muộn",
};

export const STATUS_LABEL: Record<CommissionStatus, string> = {
  PENDING: "Chờ chi trả",
  PAID: "Đã chi trả",
};

export const STATUS_TONE: Record<CommissionStatus, BadgeTone> = {
  PENDING: "warning",
  PAID: "success",
};

export interface Commission {
  id: string;
  staffId: string;
  bookingId: string;
  outcome: CommissionOutcome;
  amount: number;
  status: CommissionStatus;
  paidAt: string | null;
  createdAt: string;
  /** PT ra trước giờ kết thúc nhưng có lý do giải trình. Chỉ để hiển thị, không đổi số tiền. */
  shortSessionWithReason: boolean;
}

/** Nhãn loại buổi; buổi dạy ngắn có lý do được ghi chú thêm (tiền không đổi). */
export function outcomeText(c: Pick<Commission, "outcome" | "shortSessionWithReason">): string {
  const base = OUTCOME_LABEL[c.outcome] ?? c.outcome;
  return c.shortSessionWithReason ? `${base} (ra sớm, có lý do)` : base;
}

export interface TrainerPayRate {
  trainerId: string;
  amountPerSession: number;
}

/** TrainerResponse.java (Agent 1). */
export interface Trainer {
  id: string;
  fullName: string;
  avatarUrl: string | null;
}
