import type { Product } from "./types";

/** Khớp @Max(1000) của PosOrderCreateRequest.Item.quantity ở backend. */
export const MAX_LINE_QUANTITY = 1000;

export interface CartLine {
  productId: string;
  name: string;
  unitPrice: number;
  quantity: number;
  /** Tồn kho tại lúc nạp danh sách; chỉ để chặn nhập quá số lượng ở giao diện. Backend vẫn kiểm tra lại. */
  maxQuantity: number;
}

/** Thêm 1 sản phẩm vào giỏ, không vượt tồn kho. Hết hàng thì giữ nguyên giỏ. */
export function addToCart(lines: CartLine[], product: Product): CartLine[] {
  if (product.stockQuantity <= 0) return lines;
  const existing = lines.find((l) => l.productId === product.id);
  if (!existing) {
    return [
      ...lines,
      { productId: product.id, name: product.name, unitPrice: product.price, quantity: 1, maxQuantity: Math.min(product.stockQuantity, MAX_LINE_QUANTITY) },
    ];
  }
  return lines.map((l) =>
    l.productId === product.id ? { ...l, quantity: Math.min(l.quantity + 1, l.maxQuantity) } : l,
  );
}

/** Đặt số lượng cho 1 dòng; <= 0 (hoặc không phải số) thì bỏ dòng, lớn hơn tồn kho thì chặn ở tồn kho. */
export function setQuantity(lines: CartLine[], productId: string, quantity: number): CartLine[] {
  if (!Number.isFinite(quantity) || quantity <= 0) return removeLine(lines, productId);
  const whole = Math.floor(quantity);
  return lines.map((l) => (l.productId === productId ? { ...l, quantity: Math.min(whole, l.maxQuantity) } : l));
}

export const removeLine = (lines: CartLine[], productId: string): CartLine[] =>
  lines.filter((l) => l.productId !== productId);

export const cartTotal = (lines: CartLine[]): number => lines.reduce((sum, l) => sum + l.unitPrice * l.quantity, 0);

export const cartCount = (lines: CartLine[]): number => lines.reduce((sum, l) => sum + l.quantity, 0);

export const toOrderItems = (lines: CartLine[]) => lines.map((l) => ({ productId: l.productId, quantity: l.quantity }));

/** Body POST /pos/my-orders: chỉ có items. Cố ý không có memberId/paymentMethod/giá (backend tự lấy). */
export const toOwnOrderInput = (lines: CartLine[]) => ({ items: toOrderItems(lines) });
