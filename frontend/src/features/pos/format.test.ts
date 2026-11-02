import { describe, expect, it } from "vitest";
import { currentMonth, formatDateTime, isUuid, todayIso } from "./format";

describe("format", () => {
  it("formatDateTime không đổi múi giờ", () => {
    expect(formatDateTime("2026-10-28T21:15:28")).toBe("28/10/2026 21:15");
    expect(formatDateTime(null)).toBe("");
    expect(formatDateTime("2026-10-28")).toBe("");
  });

  it("todayIso và currentMonth theo giờ máy", () => {
    const d = new Date(2026, 0, 5, 23, 59);
    expect(todayIso(d)).toBe("2026-01-05");
    expect(currentMonth(d)).toBe("2026-01");
  });

  it("isUuid", () => {
    expect(isUuid("3F2504E0-4F89-41D3-9A0C-0305E82C3301")).toBe(true);
    expect(isUuid(" 3f2504e0-4f89-41d3-9a0c-0305e82c3301 ")).toBe(true);
    expect(isUuid("abc")).toBe(false);
    expect(isUuid("")).toBe(false);
  });
});
