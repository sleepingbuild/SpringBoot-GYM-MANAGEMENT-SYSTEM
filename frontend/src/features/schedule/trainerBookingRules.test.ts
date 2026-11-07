import { describe, expect, it } from "vitest";
import type { Booking } from "./bookingTypes";
import { completeWarning, needsEarlyLeaveReason } from "./trainerBookingRules";

function booking(overrides: Partial<Booking>): Booking {
  return {
    id: "b1",
    memberId: "m",
    memberName: "Hoc vien",
    trainerId: "t",
    trainerName: "PT",
    branchId: null,
    bookingDate: "2026-11-02",
    startTime: "08:00:00",
    endTime: "09:00:00",
    status: "CONFIRMED",
    checkInTime: "2026-11-02T08:02:00",
    checkInMethod: "FACE",
    checkOutTime: null,
    checkOutMethod: null,
    ptCheckInTime: "2026-11-02T08:00:00",
    ptCheckInMethod: "FACE",
    ptCheckOutTime: "2026-11-02T08:40:00",
    ptCheckOutMethod: "FACE",
    ptEarlyLeaveReason: null,
    fullDuration: false,
    notes: null,
    ...overrides,
  };
}

describe("needsEarlyLeaveReason", () => {
  it("dung khi CONFIRMED, PT da quet ra va chua du thoi luong", () => {
    expect(needsEarlyLeaveReason(booking({}))).toBe(true);
  });

  it("sai khi PT chua quet ra", () => {
    expect(needsEarlyLeaveReason(booking({ ptCheckOutTime: null, fullDuration: null }))).toBe(false);
  });

  it("sai khi da du thoi luong (quet ra dung gio hoac da nop ly do)", () => {
    expect(needsEarlyLeaveReason(booking({ fullDuration: true }))).toBe(false);
  });

  it("sai khi buoi khong con CONFIRMED", () => {
    expect(needsEarlyLeaveReason(booking({ status: "COMPLETED" }))).toBe(false);
    expect(needsEarlyLeaveReason(booking({ status: "PENDING" }))).toBe(false);
  });
});

describe("completeWarning", () => {
  it("canh bao khi quet ra som va chua nop ly do", () => {
    expect(completeWarning(booking({}))).toContain("không được tính công");
  });

  it("khong canh bao khi buoi du thoi luong", () => {
    expect(completeWarning(booking({ fullDuration: true }))).toBeNull();
  });

  it("khong canh bao khi PT chua quet ra", () => {
    expect(completeWarning(booking({ ptCheckOutTime: null, fullDuration: null }))).toBeNull();
  });
});
