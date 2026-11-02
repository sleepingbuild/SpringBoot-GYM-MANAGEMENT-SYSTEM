"use client";

import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { PageHeader } from "@/components/ui/PageHeader";
import { RenewalReport } from "./RenewalReport";
import { RevenueReport } from "./RevenueReport";
import { TopPackagesReport } from "./TopPackagesReport";
import { TrafficReport } from "./TrafficReport";

const TABS = [
  { id: "revenue", label: "Doanh thu" },
  { id: "renewal", label: "Tỷ lệ gia hạn" },
  { id: "traffic", label: "Lưu lượng buổi tập" },
  { id: "packages", label: "Gói bán chạy" },
] as const;

type TabId = (typeof TABS)[number]["id"];

export function ReportsPage() {
  const [tab, setTab] = useState<TabId>("revenue");

  return (
    <div>
      <PageHeader title="Báo cáo" description="Số liệu kinh doanh cho quản trị viên. Chọn khoảng ngày rồi bấm Xem báo cáo." />

      <div role="tablist" aria-label="Loại báo cáo" className="mb-4 flex flex-wrap gap-1 border-b border-line">
        {TABS.map((t) => (
          <button
            key={t.id}
            id={`tab-${t.id}`}
            role="tab"
            type="button"
            aria-selected={tab === t.id}
            aria-controls={`panel-${t.id}`}
            onClick={() => setTab(t.id)}
            className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium ${tab === t.id ? "border-plate text-plate" : "border-transparent text-steel hover:text-ink"}`}
          >
            {t.label}
          </button>
        ))}
      </div>

      <div id={`panel-${tab}`} role="tabpanel" aria-labelledby={`tab-${tab}`}>
        {tab === "revenue" && <RevenueReport />}
        {tab === "renewal" && <RenewalReport />}
        {tab === "traffic" && <TrafficReport />}
        {tab === "packages" && <TopPackagesReport />}
      </div>

      <div className="mt-6">
        <Alert tone="info" title="Sắp có">
          Báo cáo lương huấn luyện viên theo tháng sẽ được thêm khi phần lương theo buổi hoàn thiện.
        </Alert>
      </div>
    </div>
  );
}
