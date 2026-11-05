import { describe, expect, it } from "vitest";
import { defaultMonthRange, monthRangeError, toIsoMonth } from "./monthRange";

describe("khoảng tháng báo cáo lương", () => {
  it("mặc định chỉ tháng hiện tại", () => {
    expect(defaultMonthRange(new Date(2026, 9, 24))).toEqual({ from: "2026-10", to: "2026-10" });
  });

  it("toIsoMonth dùng tháng theo giờ máy, thêm số 0 phía trước", () => {
    expect(toIsoMonth(new Date(2026, 0, 31))).toBe("2026-01");
  });

  it("hợp lệ khi from bằng to hoặc đúng 12 tháng", () => {
    expect(monthRangeError({ from: "2026-10", to: "2026-10" })).toBeNull();
    expect(monthRangeError({ from: "2025-11", to: "2026-10" })).toBeNull();
  });

  it("báo lỗi khi from sau to, quá 12 tháng, thiếu hoặc sai định dạng", () => {
    expect(monthRangeError({ from: "2026-10", to: "2026-09" })).toMatch(/trước hoặc bằng/);
    expect(monthRangeError({ from: "2025-10", to: "2026-10" })).toMatch(/tối đa 12/);
    expect(monthRangeError({ from: "", to: "2026-10" })).toMatch(/Chọn đủ/);
    expect(monthRangeError({ from: "2026-13", to: "2026-10" })).toMatch(/Chọn đủ/);
  });
});
