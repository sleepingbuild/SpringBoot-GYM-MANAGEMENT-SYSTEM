import { describe, expect, it } from "vitest";
import type { Booking, BookingStatus } from "@/features/schedule/bookingTypes";
import { countByStatus, sortByStartTime } from "./staffBookingRules";

function booking(id: string, status: BookingStatus, startTime: string): Booking {
  return {
    id,
    memberId: "m",
    memberName: "Hoc vien",
    trainerId: "t",
    trainerName: "PT",
    branchId: null,
    bookingDate: "2026-11-02",
    startTime,
    endTime: "23:00:00",
    status,
    checkInTime: null,
    checkInMethod: null,
    checkOutTime: null,
    checkOutMethod: null,
    ptCheckInTime: null,
    ptCheckInMethod: null,
    ptCheckOutTime: null,
    ptCheckOutMethod: null,
    ptEarlyLeaveReason: null,
    fullDuration: null,
    notes: null,
  };
}

describe("sortByStartTime", () => {
  it("xep theo gio bat dau tang dan va khong doi mang dau vao", () => {
    const input = [
      booking("c", "CONFIRMED", "14:00:00"),
      booking("a", "PENDING", "08:00:00"),
      booking("b", "CONFIRMED", "09:30:00"),
    ];
    expect(sortByStartTime(input).map((b) => b.id)).toEqual(["a", "b", "c"]);
    expect(input.map((b) => b.id)).toEqual(["c", "a", "b"]);
  });
});

describe("countByStatus", () => {
  it("dem theo trang thai, bo trang thai khong co buoi, theo thu tu co dinh", () => {
    const result = countByStatus([
      booking("1", "CANCELLED", "08:00:00"),
      booking("2", "CONFIRMED", "09:00:00"),
      booking("3", "CONFIRMED", "10:00:00"),
      booking("4", "PENDING", "11:00:00"),
    ]);
    expect(result).toEqual([
      { status: "PENDING", count: 1 },
      { status: "CONFIRMED", count: 2 },
      { status: "CANCELLED", count: 1 },
    ]);
  });

  it("tra mang rong khi khong co buoi nao", () => {
    expect(countByStatus([])).toEqual([]);
  });
});
