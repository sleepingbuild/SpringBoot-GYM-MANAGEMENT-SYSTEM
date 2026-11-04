import { describe, expect, it } from "vitest";
import { formatDate, formatDaysLeft, formatDateTime } from "./format";

describe("formatDateTime", () => {
  it("đổi LocalDateTime sang dd/MM/yyyy HH:mm mà không đổi múi giờ", () => {
    expect(formatDateTime("2026-10-24T20:29:36.663")).toBe("24/10/2026 20:29");
  });
  it("chuỗi rỗng hoặc lạ thì giữ nguyên an toàn", () => {
    expect(formatDateTime(null)).toBe("");
    expect(formatDateTime("không rõ")).toBe("không rõ");
  });
});

describe("formatDate", () => {
  it("đổi LocalDate sang dd/MM/yyyy mà không đổi múi giờ", () => {
    expect(formatDate("2026-11-09")).toBe("09/11/2026");
  });
  it("rỗng hoặc lạ thì giữ nguyên an toàn", () => {
    expect(formatDate(undefined)).toBe("");
    expect(formatDate("không rõ")).toBe("không rõ");
  });
});

describe("formatDaysLeft", () => {
  it("nói rõ hôm nay, 1 ngày và nhiều ngày", () => {
    expect(formatDaysLeft(0)).toBe("Hết hạn hôm nay");
    expect(formatDaysLeft(1)).toBe("Còn 1 ngày");
    expect(formatDaysLeft(7)).toBe("Còn 7 ngày");
  });
});
