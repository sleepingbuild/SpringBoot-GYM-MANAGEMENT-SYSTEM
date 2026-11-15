import { apiFetch } from "@/lib/api/client";
import type { PageResponse } from "@/lib/types";
import type { CreateOrderInput, CreateOwnOrderInput, PosOrder, Product, ProductInput } from "./types";

const PRODUCTS = "/api/v1/products";
const ORDERS = "/api/v1/pos/orders";
const MY_ORDERS = "/api/v1/pos/my-orders";

/** Sản phẩm đang bán (mọi người đã đăng nhập xem được). */
export const listActiveProducts = (page = 0, size = 100) =>
  apiFetch<PageResponse<Product>>(PRODUCTS, { query: { page, size } });

/** Gồm cả sản phẩm đã ngừng bán (Lễ tân/Admin). */
export const listAllProducts = (page = 0, size = 20) =>
  apiFetch<PageResponse<Product>>(`${PRODUCTS}/admin`, { query: { page, size } });

export const createProduct = (body: ProductInput) => apiFetch<Product>(PRODUCTS, { method: "POST", body });

export const updateProduct = (id: string, body: ProductInput) =>
  apiFetch<Product>(`${PRODUCTS}/${id}`, { method: "PUT", body });

/** Xoá mềm: is_active = false. Muốn bán lại thì sửa sản phẩm và bật lại. */
export const deactivateProduct = (id: string) => apiFetch<unknown>(`${PRODUCTS}/${id}`, { method: "DELETE" });

export const createOrder = (body: CreateOrderInput) => apiFetch<PosOrder>(ORDERS, { method: "POST", body });

export const cancelOrder = (id: string) => apiFetch<PosOrder>(`${ORDERS}/${id}/cancel`, { method: "POST" });

/** from/to dạng yyyy-MM-dd; bỏ trống cả hai = backend mặc định hôm nay. */
export const listOrders = (params: { from?: string; to?: string; branchId?: string; page?: number; size?: number }) =>
  apiFetch<PageResponse<PosOrder>>(ORDERS, { query: { page: 0, size: 20, ...params } });

/** Hội viên tự mua: thanh toán mô phỏng thành công ngay, trừ kho trong cùng request. */
export const createOwnOrder = (body: CreateOwnOrderInput) => apiFetch<PosOrder>(MY_ORDERS, { method: "POST", body });

/** Đơn của chính hội viên, mới nhất trước. */
export const listMyOrders = (page = 0, size = 20) =>
  apiFetch<PageResponse<PosOrder>>(MY_ORDERS, { query: { page, size } });
