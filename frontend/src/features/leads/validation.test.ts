import { describe, expect, it } from "vitest";
import { normalizePhone, phoneError, validateConvert, validateLead } from "./validation";

describe("chuẩn hoá số điện thoại", () => {
  it("đổi +84 và 84 đầu số thành 0, bỏ ký tự phân cách", () => {
    expect(normalizePhone("+84 912-345-678")).toBe("0912345678");
    expect(normalizePhone("84912345678")).toBe("0912345678");
    expect(normalizePhone("(0912) 345.678")).toBe("0912345678");
  });

  it("báo lỗi số sai và chấp nhận số đúng", () => {
    expect(phoneError("")).toMatch(/Nhập/);
    expect(phoneError("12345")).toMatch(/không hợp lệ/);
    expect(phoneError("0912345678")).toBeNull();
    expect(phoneError("+84912345678")).toBeNull();
  });
});

describe("kiểm tra form lead", () => {
  const ok = { fullName: "Nguyễn Văn A", phone: "0912345678", source: "Facebook", notes: "" };

  it("form hợp lệ không có lỗi", () => {
    expect(validateLead(ok)).toEqual({});
  });

  it("bắt buộc họ tên và số điện thoại, giới hạn độ dài", () => {
    const errors = validateLead({ fullName: " ", phone: "", source: "x".repeat(51), notes: "y".repeat(501) });
    expect(Object.keys(errors).sort()).toEqual(["fullName", "notes", "phone", "source"]);
  });
});

describe("kiểm tra form chuyển hội viên", () => {
  it("email bắt buộc và đúng dạng", () => {
    expect(validateConvert({ email: "", password: "" }).email).toBeDefined();
    expect(validateConvert({ email: "abc", password: "" }).email).toBeDefined();
    expect(validateConvert({ email: "a@b.vn", password: "" })).toEqual({});
  });

  it("mật khẩu để trống là hợp lệ, có thì tối thiểu 8 ký tự", () => {
    expect(validateConvert({ email: "a@b.vn", password: "" }).password).toBeUndefined();
    expect(validateConvert({ email: "a@b.vn", password: "1234567" }).password).toBeDefined();
    expect(validateConvert({ email: "a@b.vn", password: "12345678" }).password).toBeUndefined();
  });
});
