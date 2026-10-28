import { describe, expect, it } from "vitest";
import { toRequest } from "./toRequest";

describe("toRequest", () => {
  it("chuỗi trống thành null", () => {
    expect(toRequest({ dateOfBirth: "", weightKg: " ", heightCm: "", goal: "  " })).toEqual({
      dateOfBirth: null,
      weightKg: null,
      heightCm: null,
      goal: null,
    });
  });
  it("đổi số, chấp nhận dấu phẩy thập phân", () => {
    expect(toRequest({ dateOfBirth: "2000-05-01", weightKg: "68,5", heightCm: "172", goal: " Tăng cơ " })).toEqual({
      dateOfBirth: "2000-05-01",
      weightKg: 68.5,
      heightCm: 172,
      goal: "Tăng cơ",
    });
  });
  it("giá trị không phải số thành null", () => {
    expect(toRequest({ dateOfBirth: "", weightKg: "abc", heightCm: "", goal: "" }).weightKg).toBeNull();
  });
});
