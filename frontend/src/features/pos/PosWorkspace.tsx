"use client";

import { useState } from "react";
import { PageHeader } from "@/components/ui/PageHeader";
import { OrderHistory } from "./OrderHistory";
import { ProductManager } from "./ProductManager";
import { SalesCounter } from "./SalesCounter";

const TABS = [
  { id: "sell", label: "Bán hàng" },
  { id: "products", label: "Sản phẩm" },
  { id: "orders", label: "Đơn hàng" },
] as const;

type TabId = (typeof TABS)[number]["id"];

export function PosWorkspace() {
  const [tab, setTab] = useState<TabId>("sell");
  return (
    <div>
      <PageHeader title="Bán lẻ (POS)" description="Bán đồ uống, thực phẩm bổ sung và phụ kiện tại quầy. Giá và tồn kho lấy từ hệ thống." />
      <div role="tablist" aria-label="Chức năng bán lẻ" className="mb-5 flex gap-2 border-b border-line">
        {TABS.map((t) => (
          <button
            key={t.id}
            role="tab"
            type="button"
            id={`pos-tab-${t.id}`}
            aria-selected={tab === t.id}
            aria-controls={`pos-panel-${t.id}`}
            onClick={() => setTab(t.id)}
            className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium ${tab === t.id ? "border-plate text-plate" : "border-transparent text-steel hover:text-ink"}`}
          >
            {t.label}
          </button>
        ))}
      </div>
      <div role="tabpanel" id={`pos-panel-${tab}`} aria-labelledby={`pos-tab-${tab}`}>
        {tab === "sell" && <SalesCounter />}
        {tab === "products" && <ProductManager />}
        {tab === "orders" && <OrderHistory />}
      </div>
    </div>
  );
}
