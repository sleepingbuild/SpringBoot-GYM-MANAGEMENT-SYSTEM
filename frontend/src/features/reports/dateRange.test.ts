import { describe, expect, it } from "vitest";
import { defaultRange, rangeError, toIsoDate } from "./dateRange";

describe("khoảng ngày báo cáo", () => {
  it("mặc định từ ngày 1 của tháng đến hôm nay", () => {
    expect(defaultRange(new Date(2026, 9, 24))).toEqual({ from: "2026-10-01", to: "2026-10-24" });
  });

  it("toIsoDate dùng ngày theo giờ máy, không lệch múi giờ", () => {
    expect(toIsoDate(new Date(2026, 0, 5))).toBe("2026-01-05");
  });

  it("hợp lệ khi from bằng to hoặc đúng 366 ngày", () => {
    expect(rangeError({ from: "2026-10-16", to: "2026-10-16" })).toBeNull();
    expect(rangeError({ from: "2025-10-16", to: "2026-10-16" })).toBeNull();
  });

  it("báo lỗi khi from sau to, quá 366 ngày hoặc thiếu ngày", () => {
    expect(rangeError({ from: "2026-10-17", to: "2026-10-16" })).toMatch(/trước hoặc bằng/);
    expect(rangeError({ from: "2025-10-15", to: "2026-10-16" })).toMatch(/tối đa 366/);
    expect(rangeError({ from: "", to: "2026-10-16" })).toMatch(/Chọn đủ/);
  });
});
