import { describe, expect, it } from "vitest";
import { classifyChange, splitChangeOptions } from "./changeOptions";
import type { PackageItem } from "./types";

function pkg(id: string, price: number, active = true): PackageItem {
  return {
    id,
    name: id,
    description: null,
    price,
    durationDays: 30,
    sessionCount: null,
    packageType: "TIME_BASED",
    peakType: "FULL_TIME",
    maxSessionsPerWeek: null,
    active,
  };
}

describe("classifyChange", () => {
  it("giá bằng nhau tính là nâng cấp (backend cho phép giá mới >= giá cũ)", () => {
    expect(classifyChange(500000, 500000)).toBe("upgrade");
    expect(classifyChange(500000, 900000)).toBe("upgrade");
    expect(classifyChange(500000, 499999)).toBe("downgrade");
  });
});

describe("splitChangeOptions", () => {
  const all = [pkg("a", 500000), pkg("b", 1200000), pkg("c", 300000), pkg("d", 3600000), pkg("e", 100000, false)];

  it("chia nhóm, bỏ gói hiện tại và gói đã ngừng bán", () => {
    const { upgrades, downgrades } = splitChangeOptions("a", 500000, all);
    expect(upgrades.map((p) => p.id)).toEqual(["b", "d"]);
    expect(downgrades.map((p) => p.id)).toEqual(["c"]);
  });

  it("nâng cấp xếp giá tăng dần, hạ cấp xếp giá giảm dần", () => {
    const list = [pkg("x", 900000), pkg("y", 700000), pkg("z", 100000), pkg("w", 400000)];
    const { upgrades, downgrades } = splitChangeOptions("cur", 600000, list);
    expect(upgrades.map((p) => p.id)).toEqual(["y", "x"]);
    expect(downgrades.map((p) => p.id)).toEqual(["w", "z"]);
  });

  it("không có gói nào để đổi thì hai nhóm rỗng", () => {
    expect(splitChangeOptions("a", 500000, [pkg("a", 500000)])).toEqual({ upgrades: [], downgrades: [] });
  });
});
