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

export interface Commission {
  id: string;
  staffId: string;
  bookingId: string;
  outcome: CommissionOutcome;
  amount: number;
  status: CommissionStatus;
  paidAt: string | null;
  createdAt: string;
}

export interface TrainerPayRate {
  trainerId: string;
  amountPerSession: number;
}
