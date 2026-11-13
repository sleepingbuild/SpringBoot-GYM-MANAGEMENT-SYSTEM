import { describe, expect, it } from "vitest";
import { homePathFor } from "./home";

describe("homePathFor", () => {
  it("mỗi vai trò có trang đầu riêng", () => {
    expect(homePathFor(["SUPER_ADMIN"])).toBe("/admin/packages");
    expect(homePathFor(["RECEPTIONIST"])).toBe("/bookings");
    expect(homePathFor(["SALES"])).toBe("/leads");
    expect(homePathFor(["TRAINER"])).toBe("/schedule");
    expect(homePathFor(["MEMBER"])).toBe("/my-membership");
  });
  it("nhiều vai trò thì vai trò cao hơn được ưu tiên", () => {
    expect(homePathFor(["MEMBER", "SUPER_ADMIN"])).toBe("/admin/packages");
  });
  it("không có vai trò nhận diện được thì về hồ sơ", () => expect(homePathFor([])).toBe("/profile"));
});
