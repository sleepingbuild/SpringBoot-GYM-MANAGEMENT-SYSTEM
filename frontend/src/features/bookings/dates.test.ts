import { describe, expect, it } from "vitest";
import { addDays, toIsoDate } from "./dates";

describe("toIsoDate", () => {
  it("dinh dang yyyy-MM-dd co so 0 dau", () => {
    expect(toIsoDate(new Date(2026, 9, 5))).toBe("2026-10-05");
    expect(toIsoDate(new Date(2026, 0, 1))).toBe("2026-01-01");
  });
});

describe("addDays", () => {
  it("cong ngay qua ranh gioi thang va nam", () => {
    expect(toIsoDate(addDays(new Date(2026, 9, 31), 1))).toBe("2026-11-01");
    expect(toIsoDate(addDays(new Date(2026, 11, 31), 1))).toBe("2027-01-01");
  });

  it("tru ngay khi n am va khong doi doi tuong goc", () => {
    const d = new Date(2026, 10, 1);
    expect(toIsoDate(addDays(d, -1))).toBe("2026-10-31");
    expect(toIsoDate(d)).toBe("2026-11-01");
  });
});
