import { describe, expect, it } from "vitest";
import { type ProductDraft, validateProductDraft } from "./productValidation";

const ok: ProductDraft = { name: "Whey", category: "SUPPLEMENT", price: "850000", stockQuantity: "8", unit: "hộp", active: true };

describe("validateProductDraft", () => {
  it("bản nháp hợp lệ không có lỗi", () => {
    expect(validateProductDraft(ok)).toEqual({});
  });

  it("tên trống hoặc quá 150 ký tự", () => {
    expect(validateProductDraft({ ...ok, name: "  " }).name).toBeDefined();
    expect(validateProductDraft({ ...ok, name: "x".repeat(151) }).name).toBeDefined();
    expect(validateProductDraft({ ...ok, name: "x".repeat(150) }).name).toBeUndefined();
  });

  it("giá phải dương, tối đa 10 chữ số nguyên và 2 thập phân", () => {
    for (const bad of ["", "0", "-5", "abc", "12345678901", "10.123"]) {
      expect(validateProductDraft({ ...ok, price: bad }).price, bad).toBeDefined();
    }
    for (const good of ["1", "10000.5", "9999999999.99"]) {
      expect(validateProductDraft({ ...ok, price: good }).price, good).toBeUndefined();
    }
  });

  it("tồn kho nguyên, không âm, tối đa 1.000.000", () => {
    for (const bad of ["", "-1", "1.5", "x", "1000001"]) {
      expect(validateProductDraft({ ...ok, stockQuantity: bad }).stockQuantity, bad).toBeDefined();
    }
    for (const good of ["0", "1000000"]) {
      expect(validateProductDraft({ ...ok, stockQuantity: good }).stockQuantity, good).toBeUndefined();
    }
  });

  it("đơn vị tối đa 20 ký tự", () => {
    expect(validateProductDraft({ ...ok, unit: "y".repeat(21) }).unit).toBeDefined();
    expect(validateProductDraft({ ...ok, unit: "" }).unit).toBeUndefined();
  });
});
