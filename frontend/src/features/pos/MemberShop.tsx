"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button, Spinner } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Field } from "@/components/ui/Field";
import { ApiError, errorMessage } from "@/lib/api/errors";
import { useToast } from "@/lib/toast";
import { createOwnOrder, listActiveProducts } from "./api";
import { type CartLine, addToCart, cartCount, cartTotal, removeLine, setQuantity, toOwnOrderInput } from "./cart";
import { formatVnd } from "./format";
import { CATEGORY_LABEL, type PosOrder, type Product, type ProductCategory } from "./types";

type Filter = "ALL" | ProductCategory;

/** Hội viên tự chọn hàng và mua. Thanh toán mô phỏng: hệ thống ghi nhận thành công ngay, không qua cổng thật. */
export function MemberShop() {
  const toast = useToast();
  const [products, setProducts] = useState<Product[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [filter, setFilter] = useState<Filter>("ALL");
  const [search, setSearch] = useState("");
  const [cart, setCart] = useState<CartLine[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [lastOrder, setLastOrder] = useState<PosOrder | null>(null);
  // Chặn bấm đúp: state cập nhật bất đồng bộ nên cần cờ đồng bộ để không tạo hai đơn.
  const inFlight = useRef(false);

  const load = useCallback(async () => {
    setLoadError(null);
    try {
      const page = await listActiveProducts(0, 100);
      setProducts([...page.content].sort((a, b) => a.name.localeCompare(b.name, "vi")));
    } catch (e) {
      setLoadError(errorMessage(e));
      setProducts([]);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const visible = useMemo(() => {
    const q = search.trim().toLowerCase();
    return (products ?? []).filter(
      (p) => (filter === "ALL" || p.category === filter) && (q === "" || p.name.toLowerCase().includes(q)),
    );
  }, [products, filter, search]);

  const total = cartTotal(cart);

  async function checkout() {
    if (cart.length === 0 || inFlight.current) return;
    inFlight.current = true;
    setSubmitting(true);
    setFormError(null);
    try {
      const order = await createOwnOrder(toOwnOrderInput(cart));
      setLastOrder(order);
      setCart([]);
      toast.success("Mua hàng thành công.");
      await load(); // tồn kho đã đổi
    } catch (e) {
      setFormError(errorMessage(e));
      if (e instanceof ApiError && (e.is("PRODUCT_OUT_OF_STOCK") || e.is("PRODUCT_NOT_FOUND"))) await load();
    } finally {
      inFlight.current = false;
      setSubmitting(false);
    }
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_22rem]">
      <section aria-label="Danh sách sản phẩm">
        <div className="mb-4 flex flex-wrap items-end gap-3">
          <Field className="w-full sm:w-64" label="Tìm sản phẩm" value={search} onChange={(e) => setSearch(e.target.value)} />
          <div className="flex flex-wrap gap-2" role="group" aria-label="Lọc theo danh mục">
            {(["ALL", "DRINK", "SUPPLEMENT", "ACCESSORY"] as Filter[]).map((f) => (
              <button key={f} type="button" onClick={() => setFilter(f)} aria-pressed={filter === f} className={filter === f ? "btn-primary" : "btn-secondary"}>
                {f === "ALL" ? "Tất cả" : CATEGORY_LABEL[f]}
              </button>
            ))}
          </div>
        </div>

        {loadError && (
          <div className="mb-4">
            <Alert tone="error" title="Không tải được sản phẩm">
              {loadError}
            </Alert>
            <Button className="mt-2" variant="secondary" onClick={() => void load()}>
              Thử lại
            </Button>
          </div>
        )}

        {products === null ? (
          <div className="flex justify-center py-10 text-steel">
            <Spinner />
          </div>
        ) : visible.length === 0 ? (
          <EmptyState title="Không có sản phẩm phù hợp">Hiện chưa có sản phẩm nào đang bán, hoặc bộ lọc đang loại hết.</EmptyState>
        ) : (
          <ul className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            {visible.map((p) => {
              const out = p.stockQuantity <= 0;
              return (
                <li key={p.id} className="panel flex flex-col justify-between p-4">
                  <div>
                    <p className="font-medium">{p.name}</p>
                    <p className="text-xs text-steel">{CATEGORY_LABEL[p.category]}</p>
                    <p className="mt-2 text-lg font-semibold">{formatVnd(p.price)}</p>
                    <p className={`text-xs ${out ? "text-rubber" : "text-steel"}`}>{out ? "Hết hàng" : `Còn ${p.stockQuantity}${p.unit ? ` ${p.unit}` : ""}`}</p>
                  </div>
                  <Button className="mt-3" variant="secondary" disabled={out} onClick={() => setCart((c) => addToCart(c, p))}>
                    Thêm vào giỏ
                  </Button>
                </li>
              );
            })}
          </ul>
        )}
      </section>

      <aside className="panel h-fit p-4" aria-label="Giỏ hàng">
        <h2 className="text-base">Giỏ hàng ({cartCount(cart)})</h2>

        {cart.length === 0 ? (
          <p className="mt-3 text-sm text-steel">Giỏ hàng trống. Chọn sản phẩm bên trái.</p>
        ) : (
          <ul className="mt-3 space-y-3">
            {cart.map((l) => (
              <li key={l.productId} className="text-sm">
                <div className="flex justify-between gap-2">
                  <span className="font-medium">{l.name}</span>
                  <span>{formatVnd(l.unitPrice * l.quantity)}</span>
                </div>
                <div className="mt-1 flex items-center gap-2">
                  <button type="button" className="btn-secondary min-h-[2rem] px-2" aria-label={`Giảm ${l.name}`} onClick={() => setCart((c) => setQuantity(c, l.productId, l.quantity - 1))}>
                    −
                  </button>
                  <input aria-label={`Số lượng ${l.name}`} className="input w-16 text-center" inputMode="numeric" value={l.quantity} onChange={(e) => setCart((c) => setQuantity(c, l.productId, Number(e.target.value)))} />
                  <button type="button" className="btn-secondary min-h-[2rem] px-2" aria-label={`Tăng ${l.name}`} onClick={() => setCart((c) => setQuantity(c, l.productId, l.quantity + 1))}>
                    +
                  </button>
                  <button type="button" className="ml-auto text-xs text-rubber underline" onClick={() => setCart((c) => removeLine(c, l.productId))}>
                    Bỏ
                  </button>
                </div>
                {l.quantity >= l.maxQuantity && <p className="mt-1 text-xs text-steel">Đã tới số lượng tồn kho.</p>}
              </li>
            ))}
          </ul>
        )}

        <div className="mt-4 space-y-3 border-t border-line pt-4">
          <div className="flex items-baseline justify-between">
            <span className="text-sm text-steel">Tổng cộng</span>
            <span className="text-xl font-semibold">{formatVnd(total)}</span>
          </div>
          <p className="text-xs text-steel">Thanh toán mô phỏng: hệ thống ghi nhận thành công ngay, không qua cổng thanh toán thật. Giá lấy từ hệ thống tại lúc mua.</p>
          {formError && <Alert tone="error">{formError}</Alert>}
          <Button className="w-full" loading={submitting} disabled={cart.length === 0} onClick={checkout}>
            Thanh toán và mua
          </Button>
        </div>

        {lastOrder && (
          <div className="mt-4">
            <Alert tone="success" title="Đơn vừa mua">
              Mã {lastOrder.id.slice(0, 8)} · {formatVnd(lastOrder.totalAmount)} · {lastOrder.items.length} dòng
            </Alert>
          </div>
        )}
      </aside>
    </div>
  );
}
