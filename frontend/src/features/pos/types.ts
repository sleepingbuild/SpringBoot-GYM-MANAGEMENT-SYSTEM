/** Khớp ProductResponse.java / PosOrderResponse.java (Agent 5). Tiền là số VND (BigDecimal phía backend). */
export type ProductCategory = "DRINK" | "SUPPLEMENT" | "ACCESSORY";

export const CATEGORY_LABEL: Record<ProductCategory, string> = {
  DRINK: "Đồ uống",
  SUPPLEMENT: "Thực phẩm bổ sung",
  ACCESSORY: "Phụ kiện",
};

export interface Product {
  id: string;
  name: string;
  category: ProductCategory;
  price: number;
  stockQuantity: number;
  unit: string | null;
  active: boolean;
}

/** ProductRequest.java: stockQuantity/active để trống khi sửa = giữ nguyên giá trị cũ. */
export interface ProductInput {
  name: string;
  category: ProductCategory;
  price: number;
  stockQuantity?: number;
  unit?: string | null;
  active?: boolean;
}

export type PaymentMethod = "CASH" | "LOCAL_CONFIRM";

export const PAYMENT_LABEL: Record<PaymentMethod, string> = {
  CASH: "Tiền mặt",
  LOCAL_CONFIRM: "Xác nhận tại quầy",
};

export type PosOrderStatus = "PENDING" | "COMPLETED" | "CANCELLED";

export const ORDER_STATUS_LABEL: Record<PosOrderStatus, string> = {
  PENDING: "Chờ xác nhận",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã huỷ",
};

export interface PosOrderItem {
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface PosOrder {
  id: string;
  memberId: string | null;
  staffId: string;
  branchId: string | null;
  totalAmount: number;
  paymentMethod: PaymentMethod | string;
  status: PosOrderStatus;
  paymentTransactionId: string | null;
  items: PosOrderItem[];
  createdAt: string;
}

/** PosOrderCreateRequest.java. Giá KHÔNG gửi lên: backend tự lấy từ DB. */
export interface CreateOrderInput {
  memberId?: string | null;
  branchId?: string | null;
  paymentMethod: PaymentMethod;
  items: { productId: string; quantity: number }[];
}
