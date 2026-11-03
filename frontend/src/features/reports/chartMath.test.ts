import { describe, expect, it } from "vitest";
import { integerAxisMax, niceMax } from "./chartMath";

describe("trục biểu đồ", () => {
  it("làm tròn trần lên số đẹp", () => {
    expect(niceMax(0)).toBe(1);
    expect(niceMax(3_775_000)).toBe(5_000_000);
    expect(niceMax(1_700_000)).toBe(2_000_000);
    expect(niceMax(100)).toBe(100);
  });

  it("trục số đếm là bội của 4 và tối thiểu 4", () => {
    expect(integerAxisMax(0)).toBe(4);
    expect(integerAxisMax(2)).toBe(4);
    expect(integerAxisMax(7)).toBe(12);
    expect(integerAxisMax(12)).toBe(20);
    for (const v of [1, 3, 9, 17, 33, 120]) {
      const max = integerAxisMax(v);
      expect(max % 4).toBe(0);
      expect(max).toBeGreaterThanOrEqual(v);
    }
  });
});
