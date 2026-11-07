import { describe, expect, it } from "vitest";
import { EMPTY_PACKAGE_FORM, type PackageFormValues, toFormValues, toPackageRequest, validatePackageForm } from "./packageFormLogic";
import type { PackageItem } from "./types";

const valid: PackageFormValues = {
  ...EMPTY_PACKAGE_FORM,
  name: "Gói 1 tháng",
  price: "500000",
  durationDays: "30",
};

describe("validatePackageForm", () => {
  it("form hợp lệ không có lỗi", () => {
    expect(validatePackageForm(valid)).toEqual({});
  });

  it("bắt buộc tên và giá", () => {
    const errors = validatePackageForm({ ...valid, name: "   ", price: "" });
    expect(errors.name).toBeDefined();
    expect(errors.price).toBe("Nhập giá gói.");
  });

  it("giá phải lớn hơn 0 và là số", () => {
    expect(validatePackageForm({ ...valid, price: "0" }).price).toBe("Giá gói phải lớn hơn 0.");
    expect(validatePackageForm({ ...valid, price: "-5" }).price).toBe("Giá gói phải lớn hơn 0.");
    expect(validatePackageForm({ ...valid, price: "abc" }).price).toBe("Giá gói phải lớn hơn 0.");
  });

  it("số ngày và số buổi phải là số nguyên dương nếu có", () => {
    expect(validatePackageForm({ ...valid, durationDays: "1.5" }).durationDays).toBeDefined();
    expect(validatePackageForm({ ...valid, durationDays: "0" }).durationDays).toBeDefined();
    expect(validatePackageForm({ ...valid, sessionCount: "-2" }).sessionCount).toBeDefined();
  });

  it("giới hạn PT mỗi tuần: trống là không giới hạn, 0 là không cho đặt, âm là sai", () => {
    expect(validatePackageForm({ ...valid, maxSessionsPerWeek: "" }).maxSessionsPerWeek).toBeUndefined();
    expect(validatePackageForm({ ...valid, maxSessionsPerWeek: "0" }).maxSessionsPerWeek).toBeUndefined();
    expect(validatePackageForm({ ...valid, maxSessionsPerWeek: "-1" }).maxSessionsPerWeek).toBeDefined();
    expect(validatePackageForm({ ...valid, maxSessionsPerWeek: "2.5" }).maxSessionsPerWeek).toBeDefined();
  });

  it("gói theo thời hạn thiếu số ngày bị chặn", () => {
    expect(validatePackageForm({ ...valid, durationDays: "" }).durationDays).toBe("Gói theo thời hạn cần số ngày sử dụng.");
  });

  it("gói theo buổi và gói PT thiếu số buổi bị chặn", () => {
    const session = validatePackageForm({ ...valid, packageType: "SESSION_BASED", durationDays: "", sessionCount: "" });
    expect(session.sessionCount).toBeDefined();
    const pt = validatePackageForm({ ...valid, packageType: "PT_1ON1", sessionCount: "" });
    expect(pt.sessionCount).toBeDefined();
    expect(validatePackageForm({ ...valid, packageType: "PT_1ON1", sessionCount: "12" })).toEqual({});
  });

  it("giới hạn độ dài theo cột trong database", () => {
    expect(validatePackageForm({ ...valid, name: "a".repeat(151) }).name).toBeDefined();
    expect(validatePackageForm({ ...valid, description: "a".repeat(501) }).description).toBeDefined();
  });
});

describe("toPackageRequest / toFormValues", () => {
  it("chuyển chuỗi rỗng thành null và chuỗi số thành số", () => {
    const req = toPackageRequest({ ...valid, name: "  Gói A  ", description: "  ", sessionCount: "", maxSessionsPerWeek: "3" });
    expect(req).toEqual({
      name: "Gói A",
      description: null,
      price: 500000,
      durationDays: 30,
      sessionCount: null,
      packageType: "TIME_BASED",
      peakType: "FULL_TIME",
      maxSessionsPerWeek: 3,
      active: true,
    });
  });

  it("giữ số 0 của giới hạn PT (khác với để trống)", () => {
    expect(toPackageRequest({ ...valid, maxSessionsPerWeek: "0" }).maxSessionsPerWeek).toBe(0);
  });

  it("đưa gói có sẵn lên form rồi đưa về request không làm mất dữ liệu", () => {
    const item: PackageItem = {
      id: "p1",
      name: "Gói PT",
      description: "12 buổi",
      price: 3600000,
      durationDays: 90,
      sessionCount: 12,
      packageType: "PT_1ON1",
      peakType: "FULL_TIME",
      maxSessionsPerWeek: 3,
      active: false,
    };
    const values = toFormValues(item);
    expect(validatePackageForm(values)).toEqual({});
    expect(toPackageRequest(values)).toEqual({
      name: "Gói PT",
      description: "12 buổi",
      price: 3600000,
      durationDays: 90,
      sessionCount: 12,
      packageType: "PT_1ON1",
      peakType: "FULL_TIME",
      maxSessionsPerWeek: 3,
      active: false,
    });
  });
});
