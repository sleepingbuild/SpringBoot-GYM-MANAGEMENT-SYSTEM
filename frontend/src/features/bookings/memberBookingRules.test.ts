import { describe, expect, it } from "vitest";
import type { Booking, BookingStatus } from "@/features/schedule/bookingTypes";
import { cancelDoneMessage, cancelNotice, splitMemberBookings } from "./memberBookingRules";

function booking(id: string, status: BookingStatus, bookingDate: string, startTime: string): Booking {
  return {
    id,
    memberId: "m",
    memberName: "Hoc vien",
    trainerId: "t",
    trainerName: "PT",
    branchId: null,
    bookingDate,
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

describe("splitMemberBookings", () => {
  it("chi dua PENDING va CONFIRMED vao upcoming, xep theo thoi gian tang dan", () => {
    const { upcoming } = splitMemberBookings([
      booking("c", "CONFIRMED", "2026-11-02", "09:00:00"),
      booking("a", "PENDING", "2026-11-01", "18:00:00"),
      booking("b", "CONFIRMED", "2026-11-01", "07:00:00"),
      booking("x", "COMPLETED", "2026-10-01", "07:00:00"),
    ]);
    expect(upcoming.map((b) => b.id)).toEqual(["b", "a", "c"]);
  });

  it("dua moi trang thai cuoi vao past, buoi moi nhat truoc", () => {
    const { upcoming, past } = splitMemberBookings([
      booking("1", "COMPLETED", "2026-10-01", "08:00:00"),
      booking("2", "LATE_CANCELLED", "2026-10-05", "08:00:00"),
      booking("3", "CANCELLED", "2026-10-03", "08:00:00"),
      booking("4", "NO_SHOW", "2026-10-05", "14:00:00"),
      booking("5", "PT_NO_SHOW", "2026-09-30", "08:00:00"),
    ]);
    expect(upcoming).toHaveLength(0);
    expect(past.map((b) => b.id)).toEqual(["4", "2", "3", "1", "5"]);
  });

  it("khong thay doi mang dau vao", () => {
    const input = [booking("b", "CONFIRMED", "2026-11-02", "09:00:00"), booking("a", "PENDING", "2026-11-01", "09:00:00")];
    splitMemberBookings(input);
    expect(input.map((b) => b.id)).toEqual(["b", "a"]);
  });
});

describe("cancelNotice", () => {
  it("noi huy PENDING la mien phi", () => {
    expect(cancelNotice("PENDING")).toContain("miễn phí");
  });

  it("canh bao huy sat gio khi da CONFIRMED", () => {
    expect(cancelNotice("CONFIRMED")).toContain("tính là đã dùng một buổi");
  });
});

describe("cancelDoneMessage", () => {
  it("bao buoi van bi tinh khi backend tra LATE_CANCELLED", () => {
    expect(cancelDoneMessage("LATE_CANCELLED")).toContain("vẫn bị tính");
  });

  it("thong bao ngan gon khi huy thuong", () => {
    expect(cancelDoneMessage("CANCELLED")).toBe("Đã huỷ buổi.");
  });
});
