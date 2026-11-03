"use client";

import { useState } from "react";
import { EmptyState } from "@/components/ui/EmptyState";
import { Field } from "@/components/ui/Field";
import { getTopPackages } from "./api";
import { defaultRange } from "./dateRange";
import { DateRangeBar } from "./DateRangeBar";
import { formatMoney, formatNumber } from "./format";
import { ReportBody } from "./ReportBody";
import type { DateRange } from "./types";
import { useReport } from "./useReport";

const DEFAULT_LIMIT = 10;

export function TopPackagesReport() {
  const [applied, setApplied] = useState<{ range: DateRange; limit: number }>({ range: defaultRange(), limit: DEFAULT_LIMIT });
  const [limitInput, setLimitInput] = useState(String(DEFAULT_LIMIT));
  const [limitError, setLimitError] = useState<string | null>(null);
  const state = useReport(`${applied.range.from}|${applied.range.to}|${applied.limit}`, (signal) => getTopPackages(applied.range, applied.limit, signal));

  function onApply(range: DateRange) {
    const n = Number(limitInput);
    if (!Number.isInteger(n) || n < 1 || n > 50) {
      setLimitError("Nhập số gói từ 1 đến 50.");
      return;
    }
    setLimitError(null);
    setApplied({ range, limit: n });
  }

  return (
    <div>
      <DateRangeBar applied={applied.range} onApply={onApply}>
        <Field label="Số gói hiển thị" inputMode="numeric" value={limitInput} onChange={(e) => setLimitInput(e.target.value)} error={limitError} className="w-40" />
      </DateRangeBar>

      <ReportBody state={state} onRetry={state.reload} label="báo cáo gói bán chạy">
        {(data) => {
          if (data.items.length === 0) {
            return <EmptyState title="Chưa có gói nào được bán trong khoảng này">Chọn khoảng ngày dài hơn hoặc kiểm tra các giao dịch thanh toán gói tập.</EmptyState>;
          }
          const max = Math.max(...data.items.map((i) => i.registrations), 1);
          return (
            <div className="space-y-4">
              <ol className="panel divide-y divide-line">
                {data.items.map((item, index) => (
                  <li key={item.packageId} className="p-4">
                    <div className="flex flex-wrap items-baseline justify-between gap-2">
                      <p className="font-medium">
                        <span className="mr-2 text-steel">{index + 1}.</span>
                        {item.packageName}
                      </p>
                      <p className="text-sm text-steel">
                        <span className="font-semibold text-ink">{formatNumber(item.registrations)} lượt</span> · {formatMoney(item.revenue)}
                      </p>
                    </div>
                    <div className="mt-2 h-2 overflow-hidden rounded-control bg-chalk" aria-hidden="true">
                      <div className="h-full bg-plate" style={{ width: `${(item.registrations / max) * 100}%` }} />
                    </div>
                  </li>
                ))}
              </ol>
              <p className="text-xs text-steel">Mỗi giao dịch gói tập thanh toán thành công tính là một lượt đăng ký. Xếp theo số lượt, rồi theo doanh thu.</p>
            </div>
          );
        }}
      </ReportBody>
    </div>
  );
}
