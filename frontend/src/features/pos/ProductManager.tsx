"use client";

import { useCallback, useEffect, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button, Spinner } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Field } from "@/components/ui/Field";
import { errorMessage } from "@/lib/api/errors";
import type { PageResponse } from "@/lib/types";
import { useToast } from "@/lib/toast";
import { createProduct, deactivateProduct, listAllProducts, updateProduct } from "./api";
import { formatVnd } from "./format";
import { Pager } from "./Pager";
import { CATEGORY_LABEL, type Product, type ProductCategory, type ProductInput } from "./types";

interface Draft {
  name: string;
  category: ProductCategory;
  price: string;
  stockQuantity: string;
  unit: string;
  active: boolean;
}

const EMPTY: Draft = { name: "", category: "DRINK", price: "", stockQuantity: "0", unit: "", active: true };

const toDraft = (p: Product): Draft => ({
  name: p.name,
  category: p.category,
  price: String(p.price),
  stockQuantity: String(p.stockQuantity),
  unit: p.unit ?? "",
  active: p.active,
});

/** Kiểm tra trước khi gửi; backend vẫn kiểm tra lại. Trả null nếu hợp lệ. */
function validate(d: Draft): Partial<Record<keyof Draft, string>> {
  const errors: Partial<Record<keyof Draft, string>> = {};
  if (d.name.trim() === "") errors.name = "Nhập tên sản phẩm.";
  const price = Number(d.price);
  if (d.price.trim() === "" || !Number.isFinite(price) || price <= 0) errors.price = "Giá phải lớn hơn 0.";
  const stock = Number(d.stockQuantity);
  if (d.stockQuantity.trim() === "" || !Number.isInteger(stock) || stock < 0) errors.stockQuantity = "Tồn kho phải là số nguyên không âm.";
  return errors;
}

export function ProductManager() {
  const toast = useToast();
  const [data, setData] = useState<PageResponse<Product> | null>(null);
  const [page, setPage] = useState(0);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [editing, setEditing] = useState<Product | null>(null);
  const [draft, setDraft] = useState<Draft>(EMPTY);
  const [errors, setErrors] = useState<Partial<Record<keyof Draft, string>>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async (p: number) => {
    setLoadError(null);
    try {
      setData(await listAllProducts(p, 20));
    } catch (e) {
      setLoadError(errorMessage(e));
      setData({ content: [], page: p, size: 20, totalElements: 0, totalPages: 0 });
    }
  }, []);

  useEffect(() => {
    void load(page);
  }, [load, page]);

  function startCreate() {
    setEditing(null);
    setDraft(EMPTY);
    setErrors({});
    setFormError(null);
  }

  function startEdit(p: Product) {
    setEditing(p);
    setDraft(toDraft(p));
    setErrors({});
    setFormError(null);
  }

  async function save() {
    const found = validate(draft);
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    const body: ProductInput = {
      name: draft.name.trim(),
      category: draft.category,
      price: Number(draft.price),
      stockQuantity: Number(draft.stockQuantity),
      unit: draft.unit.trim() === "" ? null : draft.unit.trim(),
      active: draft.active,
    };
    setSaving(true);
    setFormError(null);
    try {
      if (editing) await updateProduct(editing.id, body);
      else await createProduct(body);
      toast.success(editing ? "Đã cập nhật sản phẩm." : "Đã thêm sản phẩm.");
      startCreate();
      await load(page);
    } catch (e) {
      setFormError(errorMessage(e));
    } finally {
      setSaving(false);
    }
  }

  async function deactivate(p: Product) {
    if (!window.confirm(`Ngừng bán "${p.name}"? Đơn cũ vẫn giữ nguyên.`)) return;
    try {
      await deactivateProduct(p.id);
      toast.success("Đã ngừng bán sản phẩm.");
      await load(page);
    } catch (e) {
      toast.error(errorMessage(e));
    }
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_20rem]">
      <section aria-label="Danh sách sản phẩm">
        {loadError && (
          <div className="mb-4">
            <Alert tone="error" title="Không tải được sản phẩm">
              {loadError}
            </Alert>
          </div>
        )}
        {data === null ? (
          <div className="flex justify-center py-10 text-steel">
            <Spinner />
          </div>
        ) : data.content.length === 0 ? (
          <EmptyState title="Chưa có sản phẩm">Thêm sản phẩm đầu tiên ở khung bên phải.</EmptyState>
        ) : (
          <div className="panel overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="border-b border-line text-left text-steel">
                <tr>
                  <th className="px-3 py-2">Tên</th>
                  <th className="px-3 py-2">Danh mục</th>
                  <th className="px-3 py-2 text-right">Giá</th>
                  <th className="px-3 py-2 text-right">Tồn kho</th>
                  <th className="px-3 py-2">Trạng thái</th>
                  <th className="px-3 py-2" />
                </tr>
              </thead>
              <tbody>
                {data.content.map((p) => (
                  <tr key={p.id} className="border-b border-line last:border-0">
                    <td className="px-3 py-2 font-medium">{p.name}</td>
                    <td className="px-3 py-2">{CATEGORY_LABEL[p.category]}</td>
                    <td className="px-3 py-2 text-right">{formatVnd(p.price)}</td>
                    <td className="px-3 py-2 text-right">
                      {p.stockQuantity}
                      {p.unit ? ` ${p.unit}` : ""}
                    </td>
                    <td className="px-3 py-2">{p.active ? "Đang bán" : "Ngừng bán"}</td>
                    <td className="space-x-3 whitespace-nowrap px-3 py-2 text-right">
                      <button type="button" className="underline" onClick={() => startEdit(p)}>
                        Sửa
                      </button>
                      {p.active && (
                        <button type="button" className="text-rubber underline" onClick={() => deactivate(p)}>
                          Ngừng bán
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {data && <Pager page={data.page} totalPages={data.totalPages} onPage={setPage} />}
      </section>

      <aside className="panel h-fit space-y-3 p-4" aria-label="Biểu mẫu sản phẩm">
        <h2 className="text-base">{editing ? "Sửa sản phẩm" : "Thêm sản phẩm"}</h2>
        <Field label="Tên" required value={draft.name} error={errors.name} onChange={(e) => setDraft({ ...draft, name: e.target.value })} />
        <div>
          <label htmlFor="prod-cat" className="mb-1 block text-sm font-medium">
            Danh mục
          </label>
          <select id="prod-cat" className="input" value={draft.category} onChange={(e) => setDraft({ ...draft, category: e.target.value as ProductCategory })}>
            {(Object.keys(CATEGORY_LABEL) as ProductCategory[]).map((c) => (
              <option key={c} value={c}>
                {CATEGORY_LABEL[c]}
              </option>
            ))}
          </select>
        </div>
        <Field label="Giá (VND)" required inputMode="numeric" value={draft.price} error={errors.price} onChange={(e) => setDraft({ ...draft, price: e.target.value })} />
        <Field label="Tồn kho" required inputMode="numeric" value={draft.stockQuantity} error={errors.stockQuantity} onChange={(e) => setDraft({ ...draft, stockQuantity: e.target.value })} />
        <Field label="Đơn vị" value={draft.unit} hint="Ví dụ: chai, hộp, cái." onChange={(e) => setDraft({ ...draft, unit: e.target.value })} />
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={draft.active} onChange={(e) => setDraft({ ...draft, active: e.target.checked })} />
          Đang bán
        </label>
        {formError && <Alert tone="error">{formError}</Alert>}
        <div className="flex gap-2">
          <Button loading={saving} onClick={save}>
            {editing ? "Lưu thay đổi" : "Thêm sản phẩm"}
          </Button>
          {editing && (
            <Button variant="secondary" onClick={startCreate}>
              Bỏ sửa
            </Button>
          )}
        </div>
      </aside>
    </div>
  );
}
