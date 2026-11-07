/** Khop voi AvailableTrainerResponse cua backend (GET /api/v1/trainer-schedules/trainers). */
export interface AvailableTrainer {
  trainerId: string;
  trainerName: string | null;
  availableSlotCount: number;
}

/** Khop voi AvailableSlotResponse (GET /api/v1/trainer-schedules/available). Ngay yyyy-MM-dd, gio HH:mm:ss. */
export interface AvailableSlot {
  trainerId: string;
  date: string;
  startTime: string;
  endTime: string;
}

/** Khop voi BookingRequest. Hoc vien tu dat thi khong gui memberId. */
export interface CreateBookingInput {
  trainerId: string;
  bookingDate: string;
  startTime: string;
  endTime: string;
  notes?: string;
}
