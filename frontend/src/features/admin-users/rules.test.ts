import { describe, expect, it } from "vitest";
import { toggleRole, validateStaffForm } from "./rules";

const valid = { fullName: "Nguyễn Văn A", email: "a@gym.local", phone: "", password: "Password123", roles: ["TRAINER" as const] };

describe("toggleRole", () => {
  it("thêm và bỏ một vai trò nhân viên", () => {
    expect(toggleRole(["TRAINER"], "SALES")).toEqual(["TRAINER", "SALES"]);
    expect(toggleRole(["TRAINER", "SALES"], "TRAINER")).toEqual(["SALES"]);
  });
  it("chọn MEMBER thì bỏ hết vai trò nhân viên", () => {
    expect(toggleRole(["TRAINER", "SALES"], "MEMBER")).toEqual(["MEMBER"]);
  });
  it("chọn vai trò nhân viên thì bỏ MEMBER", () => {
    expect(toggleRole(["MEMBER"], "RECEPTIONIST")).toEqual(["RECEPTIONIST"]);
  });
});

describe("validateStaffForm", () => {
  it("hợp lệ thì không có lỗi", () => expect(validateStaffForm(valid)).toEqual({}));
  it("báo lỗi từng ô", () => {
    const errors = validateStaffForm({ fullName: " ", email: "abc", phone: "1".repeat(21), password: "short", roles: [] });
    expect(Object.keys(errors).sort()).toEqual(["email", "fullName", "password", "phone", "roles"]);
  });
  it("không cho vai trò MEMBER", () => {
    expect(validateStaffForm({ ...valid, roles: ["MEMBER"] }).roles).toBeDefined();
  });
  it("mật khẩu quá dài bị chặn", () => {
    expect(validateStaffForm({ ...valid, password: "x".repeat(73) }).password).toBeDefined();
  });
});
