import type { ProductCategory } from "./types";

export interface ProductDraft {
  name: string;
  category: ProductCategory;
  price: string;
  stockQuantity: string;
  unit: string;
  active: boolean;
}

export type ProductDraftErrors = Partial<Record<keyof ProductDraft, string>>;

/** Giới hạn khớp ProductRequest.java: tên <= 150, đơn vị <= 20, giá DECIMAL(12,2), tồn kho 0..1.000.000. */
export function validateProductDraft(d: ProductDraft): ProductDraftErrors {
  const errors: ProductDraftErrors = {};

  if (d.name.trim() === "") errors.name = "Nhập tên sản phẩm.";
  else if (d.name.trim().length > 150) errors.name = "Tên tối đa 150 ký tự.";

  const priceText = d.price.trim();
  const price = Number(priceText);
  if (priceText === "" || !Number.isFinite(price) || price <= 0) {
    errors.price = "Giá phải lớn hơn 0.";
  } else if (!/^\d{1,10}(\.\d{1,2})?$/.test(priceText)) {
    errors.price = "Giá tối đa 10 chữ số nguyên và 2 chữ số thập phân.";
  }

  const stockText = d.stockQuantity.trim();
  const stock = Number(stockText);
  if (stockText === "" || !Number.isInteger(stock) || stock < 0) errors.stockQuantity = "Tồn kho phải là số nguyên không âm.";
  else if (stock > 1_000_000) errors.stockQuantity = "Tồn kho tối đa 1.000.000.";

  if (d.unit.trim().length > 20) errors.unit = "Đơn vị tối đa 20 ký tự.";

  return errors;
}
