import { describe, expect, it } from "vitest";
import { sumCommissions } from "./summary";
import type { Commission } from "./types";

const row = (amount: number, status: Commission["status"]): Commission => ({
  id: String(Math.random()),
  staffId: "s",
  bookingId: "b",
  outcome: "COMPLETED",
  amount,
  status,
  paidAt: null,
  createdAt: "2026-10-01T10:00:00",
});

describe("sumCommissions", () => {
  it("rỗng thì toàn số 0", () => {
    expect(sumCommissions([])).toEqual({ count: 0, total: 0, pending: 0, paid: 0 });
  });

  it("tách chờ chi trả và đã chi trả", () => {
    const t = sumCommissions([row(150000, "PENDING"), row(150000, "PAID"), row(100000, "PENDING")]);
    expect(t).toEqual({ count: 3, total: 400000, pending: 250000, paid: 150000 });
  });
});
