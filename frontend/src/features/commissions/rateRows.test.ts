import { describe, expect, it } from "vitest";
import { toRateRows } from "./rateRows";
import type { Trainer } from "./types";

const t = (id: string, fullName: string): Trainer => ({ id, fullName, avatarUrl: null }) as Trainer;

describe("toRateRows", () => {
  it("ghép từng PT với mức lương, chưa đặt thì amount null", () => {
    const rows = toRateRows([t("1", "An"), t("2", "Bình")], [{ ok: true, amount: 150000 }, { ok: true, amount: null }]);
    expect(rows).toEqual([
      { trainerId: "1", name: "An", amount: 150000, failed: false },
      { trainerId: "2", name: "Bình", amount: null, failed: false },
    ]);
  });

  it("lỗi tải một PT chỉ đánh dấu failed cho PT đó", () => {
    const rows = toRateRows([t("1", "An"), t("2", "Bình")], [{ ok: false }, { ok: true, amount: 100000 }]);
    expect(rows[0]).toMatchObject({ amount: null, failed: true });
    expect(rows[1]).toMatchObject({ amount: 100000, failed: false });
  });
});
