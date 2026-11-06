"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button, Spinner } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Field } from "@/components/ui/Field";
import { ApiError, errorMessage } from "@/lib/api/errors";
import { useToast } from "@/lib/toast";
import { createOrder, listActiveProducts } from "./api";
import { type CartLine, addToCart, cartCount, cartTotal, removeLine, setQuantity, toOrderItems } from "./cart";
import { MemberPicker } from "@/features/membership/MemberPicker";
import type { MemberSummary } from "@/features/membership/types";
import { formatVnd } from "./format";
import { CATEGORY_LABEL, PAYMENT_LABEL, type PaymentMethod, type PosOrder, type Product, type ProductCategory } from "./types";

type Filter = "ALL" | ProductCategory;

export function SalesCounter() {
  const toast = useToast();
  const [products, setProducts] = useState<Product[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [filter, setFilter] = useState<Filter>("ALL");
  const [search, setSearch] = useState("");
  const [cart, setCart] = useState<CartLine[]>([]);
  const [member, setMember] = useState<MemberSummary | null>(null);
  const [method, setMethod] = useState<PaymentMethod>("CASH");
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [lastOrder, setLastOrder] = useState<PosOrder | null>(null);

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
    if (cart.length === 0) return;
    setSubmitting(true);
    setFormError(null);
    try {
      const order = await createOrder({
        memberId: member ? member.id : null,
        paymentMethod: method,
        items: toOrderItems(cart),
      });
      setLastOrder(order);
      setCart([]);
      setMember(null);
      toast.success("Đã tạo đơn và thu tiền.");
      await load(); // tồn kho đã đổi
    } catch (e) {
      setFormError(errorMessage(e));
      if (e instanceof ApiError && e.is("PRODUCT_OUT_OF_STOCK")) await load();
    } finally {
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
              <button
                key={f}
                type="button"
                onClick={() => setFilter(f)}
                aria-pressed={filter === f}
                className={filter === f ? "btn-primary" : "btn-secondary"}
              >
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
          </div>
        )}

        {products === null ? (
          <div className="flex justify-center py-10 text-steel">
            <Spinner />
          </div>
        ) : visible.length === 0 ? (
          <EmptyState title="Không có sản phẩm phù hợp">
            Chưa có sản phẩm nào đang bán, hoặc bộ lọc đang loại hết. Thêm sản phẩm ở thẻ Sản phẩm.
          </EmptyState>
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
                    <p className={`text-xs ${out ? "text-rubber" : "text-steel"}`}>
                      {out ? "Hết hàng" : `Còn ${p.stockQuantity}${p.unit ? ` ${p.unit}` : ""}`}
                    </p>
                  </div>
                  <Button className="mt-3" variant="secondary" disabled={out} onClick={() => setCart((c) => addToCart(c, p))}>
                    Thêm vào đơn
                  </Button>
                </li>
              );
            })}
          </ul>
        )}
      </section>

      <aside className="panel h-fit p-4" aria-label="Đơn hàng hiện tại">
        <h2 className="text-base">Đơn hiện tại ({cartCount(cart)})</h2>

        {cart.length === 0 ? (
          <p className="mt-3 text-sm text-steel">Chưa có sản phẩm. Chọn sản phẩm bên trái.</p>
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
                  <input
                    aria-label={`Số lượng ${l.name}`}
                    className="input w-16 text-center"
                    inputMode="numeric"
                    value={l.quantity}
                    onChange={(e) => setCart((c) => setQuantity(c, l.productId, Number(e.target.value)))}
                  />
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
          <div>
            <p className="mb-1 text-sm font-medium">Hội viên (không bắt buộc)</p>
            <MemberPicker value={member} onChange={setMember} changeLabel="Bỏ chọn" />
            {!member && <p className="mt-1 text-xs text-steel">Không chọn hội viên thì đơn tính cho khách vãng lai.</p>}
          </div>
          <div>
            <label htmlFor="pos-method" className="mb-1 block text-sm font-medium">
              Hình thức thanh toán
            </label>
            <select id="pos-method" className="input" value={method} onChange={(e) => setMethod(e.target.value as PaymentMethod)}>
              {(Object.keys(PAYMENT_LABEL) as PaymentMethod[]).map((m) => (
                <option key={m} value={m}>
                  {PAYMENT_LABEL[m]}
                </option>
              ))}
            </select>
          </div>
          <div className="flex items-baseline justify-between">
            <span className="text-sm text-steel">Tổng cộng</span>
            <span className="text-xl font-semibold">{formatVnd(total)}</span>
          </div>
          {formError && <Alert tone="error">{formError}</Alert>}
          <Button className="w-full" loading={submitting} disabled={cart.length === 0} onClick={checkout}>
            Thu tiền và tạo đơn
          </Button>
        </div>

        {lastOrder && (
          <div className="mt-4">
            <Alert tone="success" title="Đơn vừa tạo">
              Mã {lastOrder.id.slice(0, 8)} · {formatVnd(lastOrder.totalAmount)} · {lastOrder.items.length} dòng
            </Alert>
          </div>
        )}
      </aside>
    </div>
  );
}
