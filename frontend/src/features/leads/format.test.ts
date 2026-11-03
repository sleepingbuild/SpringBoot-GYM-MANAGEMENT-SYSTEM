import { describe, expect, it } from "vitest";
import { formatDateTime } from "./format";

describe("formatDateTime", () => {
  it("đổi LocalDateTime sang dd/MM/yyyy HH:mm mà không đổi múi giờ", () => {
    expect(formatDateTime("2026-10-24T20:29:36.663")).toBe("24/10/2026 20:29");
  });
  it("chuỗi rỗng hoặc lạ thì giữ nguyên an toàn", () => {
    expect(formatDateTime(null)).toBe("");
    expect(formatDateTime("không rõ")).toBe("không rõ");
  });
});
