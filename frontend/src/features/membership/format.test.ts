import { describe, expect, it } from "vitest";
import { formatDate, formatDateTime, formatVnd, packageSummary, shortId, weeklyLimitText } from "./format";

describe("formatVnd", () => {
  it("nhóm hàng nghìn bằng dấu chấm", () => {
    expect(formatVnd(500000)).toBe("500.000 ₫");
    expect(formatVnd(3600000)).toBe("3.600.000 ₫");
    expect(formatVnd(1234567890)).toBe("1.234.567.890 ₫");
  });
  it("số nhỏ và số 0 không có dấu chấm", () => {
    expect(formatVnd(0)).toBe("0 ₫");
    expect(formatVnd(999)).toBe("999 ₫");
  });
  it("làm tròn phần thập phân", () => {
    expect(formatVnd(45000.4)).toBe("45.000 ₫");
  });
});

describe("formatDate / formatDateTime", () => {
  it("đổi yyyy-MM-dd sang dd/MM/yyyy mà không qua Date", () => {
    expect(formatDate("2026-10-27")).toBe("27/10/2026");
  });
  it("giữ nguyên giờ Việt Nam backend trả, không cộng trừ múi giờ", () => {
    expect(formatDateTime("2026-10-27T17:05:09.123")).toBe("17:05 27/10/2026");
    expect(formatDateTime("2026-10-27T00:00:00")).toBe("00:00 27/10/2026");
  });
  it("rỗng hoặc lạ thì không báo lỗi", () => {
    expect(formatDate(null)).toBe("—");
    expect(formatDateTime(undefined)).toBe("—");
    expect(formatDate("không phải ngày")).toBe("không phải ngày");
  });
});

describe("packageSummary / weeklyLimitText / shortId", () => {
  it("ghép số buổi và số ngày", () => {
    expect(packageSummary({ sessionCount: 12, durationDays: 90 })).toBe("12 buổi · 90 ngày");
    expect(packageSummary({ sessionCount: null, durationDays: 30 })).toBe("30 ngày");
    expect(packageSummary({ sessionCount: null, durationDays: null })).toBe("Không giới hạn");
  });
  it("giải thích giới hạn PT mỗi tuần", () => {
    expect(weeklyLimitText(null)).toBe("Không giới hạn");
    expect(weeklyLimitText(0)).toBe("Không đặt được buổi PT");
    expect(weeklyLimitText(3)).toBe("Tối đa 3 buổi PT mỗi tuần");
  });
  it("rút gọn mã", () => {
    expect(shortId("0123abcd-0000-0000-0000-000000000000")).toBe("0123abcd");
    expect(shortId(null)).toBe("—");
  });
});
