import { describe, expect, it } from "vitest";
import { buildCreateInput, missingMemberHint } from "./createBookingRules";
import type { AvailableSlot } from "./types";

const slot: AvailableSlot = {
  trainerId: "t-1",
  date: "2026-11-08",
  startTime: "14:00:00",
  endTime: "15:00:00",
};

describe("buildCreateInput", () => {
  it("hoi vien tu dat: khong gui memberId du co truyen vao", () => {
    const input = buildCreateInput({ slot, staff: false, memberId: "m-1", branchId: "", notes: "" });
    expect(input).toEqual({
      trainerId: "t-1",
      bookingDate: "2026-11-08",
      startTime: "14:00:00",
      endTime: "15:00:00",
    });
    expect("memberId" in input).toBe(false);
  });

  it("le tan dat ho: gui memberId da chon", () => {
    const input = buildCreateInput({ slot, staff: true, memberId: "m-1", branchId: "", notes: "" });
    expect(input.memberId).toBe("m-1");
  });

  it("chi nhanh rong thi khong gui, co chon thi gui", () => {
    expect(buildCreateInput({ slot, staff: false, memberId: null, branchId: "", notes: "" }).branchId).toBeUndefined();
    expect(buildCreateInput({ slot, staff: false, memberId: null, branchId: "b-1", notes: "" }).branchId).toBe("b-1");
  });

  it("ghi chu duoc cat khoang trang, rong thi bo qua", () => {
    expect(buildCreateInput({ slot, staff: false, memberId: null, branchId: "", notes: "  tap vai  " }).notes).toBe("tap vai");
    expect(buildCreateInput({ slot, staff: false, memberId: null, branchId: "", notes: "   " }).notes).toBeUndefined();
  });
});

describe("missingMemberHint", () => {
  it("le tan chua chon hoi vien thi co cau nhac", () => {
    expect(missingMemberHint(true, null)).toBeTruthy();
  });

  it("da chon hoi vien hoac la hoi vien tu dat thi khong nhac", () => {
    expect(missingMemberHint(true, "m-1")).toBeNull();
    expect(missingMemberHint(false, null)).toBeNull();
  });
});
