import { describe, expect, it } from "vitest";
import { safeNext } from "./safeNext";

describe("safeNext", () => {
  it("giữ đường dẫn nội bộ", () => expect(safeNext("/profile")).toBe("/profile"));
  it("không có thì dùng mặc định", () => expect(safeNext(null)).toBe("/"));
  it("chặn địa chỉ ngoài", () => {
    expect(safeNext("https://evil.example")).toBe("/");
    expect(safeNext("//evil.example")).toBe("/");
    expect(safeNext("/\\evil.example")).toBe("/");
  });
  it("không quay lại trang đăng nhập", () => expect(safeNext("/login?next=/x")).toBe("/"));
});
