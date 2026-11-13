import { describe, expect, it } from "vitest";
import type { Booking, BookingStatus } from "@/features/schedule/bookingTypes";
import { addDays, computeMemberStats } from "./memberStats";

function booking(status: BookingStatus, bookingDate: string, startTime = "08:00:00"): Booking {
  return {
    id: `${status}-${bookingDate}-${startTime}`, memberId: "m", memberName: null, trainerId: "t", trainerName: "PT", branchId: null,
    bookingDate, startTime, endTime: "09:00:00", status,
    checkInTime: null, checkInMethod: null, checkOutTime: null, checkOutMethod: null,
    ptCheckInTime: null, ptCheckInMethod: null, ptCheckOutTime: null, ptCheckOutMethod: null,
    ptEarlyLeaveReason: null, fullDuration: null, notes: null,
  };
}

describe("addDays", () => {
  it("qua tháng và năm", () => {
    expect(addDays("2026-11-13", -29)).toBe("2026-10-15");
    expect(addDays("2026-01-02", -3)).toBe("2025-12-30");
    expect(addDays("2026-02-27", 2)).toBe("2026-03-01");
  });
});

describe("computeMemberStats", () => {
  const today = "2026-11-13";

  it("danh sách rỗng", () => {
    expect(computeMemberStats([], today)).toEqual({ completed: 0, upcoming: 0, completedLast30Days: 0, chargedWithoutTraining: 0, ptNoShow: 0, nextBooking: null });
  });

  it("đếm theo trạng thái", () => {
    const stats = computeMemberStats(
      [
        booking("COMPLETED", "2026-11-12"), booking("COMPLETED", "2026-09-01"),
        booking("LATE_CANCELLED", "2026-11-01"), booking("NO_SHOW", "2026-11-02"), booking("CANCELLED", "2026-11-03"),
        booking("PT_NO_SHOW", "2026-11-04"),
      ],
      today,
    );
    expect(stats.completed).toBe(2);
    expect(stats.completedLast30Days).toBe(1);
    expect(stats.chargedWithoutTraining).toBe(2);
    expect(stats.ptNoShow).toBe(1);
  });

  it("buổi sắp tới: chỉ buổi còn hiệu lực từ hôm nay, chọn buổi gần nhất", () => {
    const stats = computeMemberStats(
      [booking("CONFIRMED", "2026-11-20"), booking("PENDING", "2026-11-14", "07:00:00"), booking("CONFIRMED", "2026-11-10"), booking("CANCELLED", "2026-11-13")],
      today,
    );
    expect(stats.upcoming).toBe(2);
    expect(stats.nextBooking?.bookingDate).toBe("2026-11-14");
  });

  it("buổi hôm nay cũng tính là sắp tới", () => {
    expect(computeMemberStats([booking("CONFIRMED", today)], today).upcoming).toBe(1);
  });

  it("ngày đầu của cửa sổ 30 ngày được tính, ngày trước đó thì không", () => {
    const stats = computeMemberStats([booking("COMPLETED", "2026-10-15"), booking("COMPLETED", "2026-10-14")], today);
    expect(stats.completedLast30Days).toBe(1);
  });
});
