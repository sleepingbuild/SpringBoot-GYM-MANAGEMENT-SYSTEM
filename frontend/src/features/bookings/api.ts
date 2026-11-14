import { apiFetch } from "@/lib/api/client";
import type { Booking } from "@/features/schedule/bookingTypes";
import type {
  AvailableSlot,
  AvailableTrainer,
  BranchOption,
  CreateBookingInput,
  MemberOption,
  PageResult,
  TrainerOption,
} from "./types";

/** Cac PT con it nhat mot slot trong trong ngay (hoc vien, le tan, admin). */
export const listTrainersWithSlots = (date: string) =>
  apiFetch<AvailableTrainer[]>("/api/v1/trainer-schedules/trainers", { query: { date } });

/** Slot trong ca cua PT: dat xong duoc xac nhan ngay. */
export const listAvailableSlots = (trainerId: string, date: string) =>
  apiFetch<AvailableSlot[]>("/api/v1/trainer-schedules/available", { query: { trainerId, date } });

/** Slot ngoai gio con trong cua PT: dat xong phai cho PT xac nhan. */
export const listOffHoursSlots = (trainerId: string, date: string) =>
  apiFetch<AvailableSlot[]>("/api/v1/trainer-schedules/off-hours", { query: { trainerId, date } });

export const createBooking = (body: CreateBookingInput) =>
  apiFetch<Booking>("/api/v1/bookings", { method: "POST", body });

/** Chi nhanh dang hoat dong (Agent 1). Ai dang nhap cung goi duoc. */
export const listBranches = () => apiFetch<BranchOption[]>("/api/v1/branches");

/** Moi huan luyen vien dang hoat dong (Agent 1). Ai dang nhap cung goi duoc. */
export const listTrainers = () => apiFetch<TrainerOption[]>("/api/v1/trainers");

/** Tim hoi vien theo ten, so dien thoai hoac email (Agent 1). Chi le tan va admin goi duoc. */
export const searchMembers = (q: string) =>
  apiFetch<PageResult<MemberOption>>("/api/v1/users", { query: { role: "MEMBER", q } });
