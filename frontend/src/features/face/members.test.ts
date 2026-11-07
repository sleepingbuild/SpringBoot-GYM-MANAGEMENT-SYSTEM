import { describe, expect, it } from "vitest";
import { isUuid, parseMemberPage } from "./members";

const A = { id: "11111111-1111-4111-8111-111111111111", fullName: "Nguyễn Văn A", phone: "0901234567", email: "a@gym.test" };

describe("parseMemberPage", () => {
  it("đọc PageResponse có content và totalElements", () => {
    const r = parseMemberPage({ content: [A], page: 0, size: 10, totalElements: 25, totalPages: 3 });
    expect(r.members).toEqual([A]);
    expect(r.totalElements).toBe(25);
  });

  it("nhận mảng thuần", () => {
    const r = parseMemberPage([A]);
    expect(r.members).toHaveLength(1);
    expect(r.totalElements).toBe(1);
  });

  it("bỏ phần tử sai dạng và chuẩn hóa trường rỗng thành null", () => {
    const r = parseMemberPage({
      content: [A, null, 5, { id: "x" }, { id: "2", fullName: "  " }, { id: "3", fullName: "B", phone: "", email: 7 }],
    });
    expect(r.members.map((m) => m.id)).toEqual([A.id, "3"]);
    expect(r.members[1]).toEqual({ id: "3", fullName: "B", phone: null, email: null });
  });

  it("trả rỗng với dữ liệu lạ", () => {
    expect(parseMemberPage(undefined)).toEqual({ members: [], totalElements: 0 });
    expect(parseMemberPage("abc")).toEqual({ members: [], totalElements: 0 });
    expect(parseMemberPage({ content: "x" })).toEqual({ members: [], totalElements: 0 });
  });

  it("không tin totalElements nhỏ hơn số phần tử thực", () => {
    expect(parseMemberPage({ content: [A], totalElements: 0 }).totalElements).toBe(1);
  });
});

describe("isUuid", () => {
  it("nhận UUID hợp lệ, bỏ khoảng trắng hai đầu", () => {
    expect(isUuid(` ${A.id} `)).toBe(true);
  });
  it("từ chối chuỗi khác", () => {
    expect(isUuid("")).toBe(false);
    expect(isUuid("1234")).toBe(false);
    expect(isUuid(A.id + "0")).toBe(false);
  });
});
