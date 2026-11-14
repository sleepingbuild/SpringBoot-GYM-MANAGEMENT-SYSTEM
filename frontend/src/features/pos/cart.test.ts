import { describe, expect, it } from "vitest";
import { MAX_LINE_QUANTITY, addToCart, cartCount, cartTotal, removeLine, setQuantity, toOrderItems, toOwnOrderInput } from "./cart";
import type { Product } from "./types";

const water: Product = { id: "p1", name: "Nước suối", category: "DRINK", price: 10000, stockQuantity: 3, unit: "chai", active: true };
const whey: Product = { id: "p2", name: "Whey", category: "SUPPLEMENT", price: 500000, stockQuantity: 5, unit: "hộp", active: true };

describe("cart", () => {
  it("thêm sản phẩm mới với số lượng 1", () => {
    const lines = addToCart([], water);
    expect(lines).toHaveLength(1);
    expect(lines[0]).toMatchObject({ productId: "p1", quantity: 1, unitPrice: 10000, maxQuantity: 3 });
  });

  it("thêm lần nữa thì tăng số lượng, dừng ở tồn kho", () => {
    let lines = addToCart([], water);
    for (let i = 0; i < 10; i++) lines = addToCart(lines, water);
    expect(lines[0].quantity).toBe(3);
  });

  it("sản phẩm hết hàng không vào giỏ", () => {
    expect(addToCart([], { ...water, stockQuantity: 0 })).toEqual([]);
  });

  it("tính tổng và số lượng", () => {
    let lines = addToCart([], water);
    lines = setQuantity(lines, "p1", 3);
    lines = addToCart(lines, whey);
    expect(cartTotal(lines)).toBe(3 * 10000 + 500000);
    expect(cartCount(lines)).toBe(4);
  });

  it("setQuantity: không vượt tồn kho, số lẻ làm tròn xuống", () => {
    const lines = addToCart([], whey);
    expect(setQuantity(lines, "p2", 99)[0].quantity).toBe(5);
    expect(setQuantity(lines, "p2", 2.9)[0].quantity).toBe(2);
  });

  it("setQuantity <= 0 hoặc NaN thì bỏ dòng", () => {
    const lines = addToCart([], whey);
    expect(setQuantity(lines, "p2", 0)).toEqual([]);
    expect(setQuantity(lines, "p2", Number.NaN)).toEqual([]);
  });

  it("removeLine và toOrderItems không gửi giá", () => {
    const lines = addToCart(addToCart([], water), whey);
    expect(removeLine(lines, "p1")).toHaveLength(1);
    expect(toOrderItems(lines)).toEqual([
      { productId: "p1", quantity: 1 },
      { productId: "p2", quantity: 1 },
    ]);
  });

  it("mỗi dòng không vượt giới hạn 1000 của backend dù tồn kho lớn hơn", () => {
    const bulk: Product = { ...water, id: "p9", stockQuantity: 50000 };
    const lines = addToCart([], bulk);
    expect(lines[0].maxQuantity).toBe(MAX_LINE_QUANTITY);
    expect(setQuantity(lines, "p9", 5000)[0].quantity).toBe(MAX_LINE_QUANTITY);
  });

  it("không làm thay đổi giỏ gốc", () => {
    const lines = addToCart([], water);
    const next = addToCart(lines, water);
    expect(lines[0].quantity).toBe(1);
    expect(next[0].quantity).toBe(2);
  });
});

describe("toOwnOrderInput", () => {
  it("chỉ gửi items (productId, quantity): không memberId, không paymentMethod, không giá", () => {
    const lines = [{ productId: "p1", name: "Nước", unitPrice: 10000, quantity: 2, maxQuantity: 5 }];
    const body = toOwnOrderInput(lines);
    expect(body).toEqual({ items: [{ productId: "p1", quantity: 2 }] });
    expect(Object.keys(body)).toEqual(["items"]);
  });
});
