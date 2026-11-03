import { describe, expect, it } from "vitest";
import { formatIsoDate, formatMoney, formatPercent, formatPeriod } from "./format";

describe("định dạng báo cáo", () => {
  it("tiền có dấu chấm ngăn cách và ký hiệu đồng", () => {
    const text = formatMoney(1200000);
    expect(text).toContain("1.200.000");
    expect(text).toContain("₫");
  });

  it("phần trăm từ tỷ lệ 0 đến 1", () => {
    expect(formatPercent(0.5)).toContain("50");
    expect(formatPercent(0.25)).toContain("25");
  });

  it("đổi ngày ISO mà không qua Date", () => {
    expect(formatIsoDate("2026-10-24")).toBe("24/10/2026");
    expect(formatPeriod("2026-10-24")).toBe("24/10");
    expect(formatPeriod("2026-10")).toBe("10/2026");
    expect(formatPeriod("khác")).toBe("khác");
  });
});
