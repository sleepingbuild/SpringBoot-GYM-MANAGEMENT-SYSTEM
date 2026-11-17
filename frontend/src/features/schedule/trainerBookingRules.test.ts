import { describe, expect, it } from "vitest";
import type { Booking } from "./bookingTypes";
import { completeBlocker, completeWarning, completionPlan, workedMinutes } from "./trainerBookingRules";

function booking(over: Partial<Booking> = {}): Booking {
  return {
    id: "b1",
    memberId: "m1",
    memberName: "An",
    trainerId: "t1",
    trainerName: "Binh",
    branchId: null,
    bookingDate: "2026-11-10",
    startTime: "09:00:00",
    endTime: "10:00:00",
    status: "CONFIRMED",
    checkInTime: "2026-11-10T09:01:00",
    checkInMethod: "FACE",
    checkOutTime: null,
    checkOutMethod: null,
    ptCheckInTime: "2026-11-10T09:00:00",
    ptCheckInMethod: "FACE",
    ptCheckOutTime: null,
    ptCheckOutMethod: null,
    ptEarlyLeaveReason: null,
    fullDuration: null,
    notes: null,
    ...over,
  };
}

const at = (hhmm: string) => new Date(`2026-11-10T${hhmm}:00`);

describe("workedMinutes", () => {
  it("tinh tu luc PT quet vao den bay gio khi chua quet ra", () => {
    expect(workedMinutes(booking(), at("09:50"))).toBe(50);
  });

  it("chua quet vao thi tinh tu gio bat dau theo lich", () => {
    expect(workedMinutes(booking({ ptCheckInTime: null }), at("09:20"))).toBe(20);
  });

  it("da quet ra thi lay gio ra, khong lay bay gio", () => {
    expect(workedMinutes(booking({ ptCheckOutTime: "2026-11-10T09:40:00" }), at("10:30"))).toBe(40);
  });
});

describe("completionPlan", () => {
  it("du gio: khong can ly do", () => {
    const plan = completionPlan(booking(), at("10:00"));
    expect(plan).toEqual({ minutes: 60, reasonRequired: false, reasonSuggested: false });
  });

  it("duoi 45 phut va chua co ly do: bat buoc nhap ly do", () => {
    const plan = completionPlan(booking(), at("09:30"));
    expect(plan.reasonRequired).toBe(true);
    expect(plan.reasonSuggested).toBe(false);
  });

  it("duoi 45 phut nhung da co ly do luu truoc: khong bat buoc nua", () => {
    const plan = completionPlan(booking({ ptEarlyLeaveReason: "Hoc vien xin ve som" }), at("09:30"));
    expect(plan.reasonRequired).toBe(false);
  });

  it("tu 45 phut nhung chua den gio ket thuc: de nghi nhap ly do (khong bat buoc)", () => {
    const plan = completionPlan(booking(), at("09:50"));
    expect(plan.reasonRequired).toBe(false);
    expect(plan.reasonSuggested).toBe(true);
  });
});

describe("completeBlocker", () => {
  it("bat buoc ly do ma de trong thi chan", () => {
    const plan = completionPlan(booking(), at("09:30"));
    expect(completeBlocker(plan, "   ")).toContain("cần nhập lý do");
  });

  it("ly do qua ngan thi chan, ke ca khi khong bat buoc", () => {
    const plan = completionPlan(booking(), at("10:00"));
    expect(completeBlocker(plan, "ab")).toContain("5 ký tự");
  });

  it("ly do hop le hoac khong can thi cho qua", () => {
    expect(completeBlocker(completionPlan(booking(), at("09:30")), "Hoc vien xin ve som")).toBeNull();
    expect(completeBlocker(completionPlan(booking(), at("10:00")), "")).toBeNull();
  });
});

describe("completeWarning", () => {
  it("chua du gio theo lich, khong ly do: canh bao khong tinh cong", () => {
    const plan = completionPlan(booking(), at("09:50"));
    expect(completeWarning(plan, "")).toContain("không được tính công");
  });

  it("da nhap ly do hoac da du gio thi khong canh bao", () => {
    expect(completeWarning(completionPlan(booking(), at("09:50")), "Hoc vien xin ve som")).toBeNull();
    expect(completeWarning(completionPlan(booking(), at("10:00")), "")).toBeNull();
  });
});
