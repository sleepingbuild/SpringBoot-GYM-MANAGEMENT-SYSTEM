import { describe, expect, it } from "vitest";
import { outcomeText } from "./types";

describe("outcomeText", () => {
  it("buổi thường chỉ hiện nhãn loại buổi", () => {
    expect(outcomeText({ outcome: "COMPLETED", shortSessionWithReason: false })).toBe("Dạy hoàn thành");
  });

  it("buổi ra sớm có lý do được ghi chú thêm", () => {
    expect(outcomeText({ outcome: "COMPLETED", shortSessionWithReason: true })).toBe("Dạy hoàn thành (ra sớm, có lý do)");
  });
});
