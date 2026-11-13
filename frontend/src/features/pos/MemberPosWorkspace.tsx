"use client";

import { useState } from "react";
import { PageHeader } from "@/components/ui/PageHeader";
import { MemberShop } from "./MemberShop";
import { MyOrders } from "./MyOrders";

const TABS = [
  { id: "shop", label: "Mua hàng" },
  { id: "orders", label: "Đơn của tôi" },
] as const;

type TabId = (typeof TABS)[number]["id"];

export function MemberPosWorkspace() {
  const [tab, setTab] = useState<TabId>("shop");
  return (
    <div>
      <PageHeader title="Cửa hàng" description="Mua đồ uống, thực phẩm bổ sung và phụ kiện. Thanh toán mô phỏng, ghi nhận ngay." />
      <div role="tablist" aria-label="Cửa hàng" className="mb-5 flex gap-2 border-b border-line">
        {TABS.map((t) => (
          <button
            key={t.id}
            role="tab"
            type="button"
            id={`shop-tab-${t.id}`}
            aria-selected={tab === t.id}
            aria-controls={`shop-panel-${t.id}`}
            onClick={() => setTab(t.id)}
            className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium ${tab === t.id ? "border-plate text-plate" : "border-transparent text-steel hover:text-ink"}`}
          >
            {t.label}
          </button>
        ))}
      </div>
      <div role="tabpanel" id={`shop-panel-${tab}`} aria-labelledby={`shop-tab-${tab}`}>
        {tab === "shop" && <MemberShop />}
        {tab === "orders" && <MyOrders />}
      </div>
    </div>
  );
}
