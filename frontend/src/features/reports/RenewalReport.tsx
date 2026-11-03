"use client";

import { useState } from "react";
import { Field } from "@/components/ui/Field";
import { EmptyState } from "@/components/ui/EmptyState";
import { getRenewalRate } from "./api";
import { defaultRange } from "./dateRange";
import { DateRangeBar } from "./DateRangeBar";
import { formatNumber, formatPercent } from "./format";
import { ReportBody } from "./ReportBody";
import { StatCard } from "./StatCard";
import type { DateRange } from "./types";
import { useReport } from "./useReport";

const DEFAULT_WINDOW = 30;

export function RenewalReport() {
  const [applied, setApplied] = useState<{ range: DateRange; windowDays: number }>({ range: defaultRange(), windowDays: DEFAULT_WINDOW });
  const [windowInput, setWindowInput] = useState(String(DEFAULT_WINDOW));
  const [windowError, setWindowError] = useState<string | null>(null);
  const state = useReport(`${applied.range.from}|${applied.range.to}|${applied.windowDays}`, (signal) =>
    getRenewalRate(applied.range, applied.windowDays, signal),
  );

  function onApply(range: DateRange) {
    const w = Number(windowInput);
    if (!Number.isInteger(w) || w < 1 || w > 365) {
      setWindowError("Nhập số ngày từ 1 đến 365.");
      return;
    }
    setWindowError(null);
    setApplied({ range, windowDays: w });
  }

  return (
    <div>
      <DateRangeBar applied={applied.range} onApply={onApply}>
        <Field
          label="Tính gia hạn trong (ngày)"
          inputMode="numeric"
          value={windowInput}
          onChange={(e) => setWindowInput(e.target.value)}
          error={windowError}
          hint="Số ngày sau khi hết hạn vẫn tính là gia hạn."
          className="w-48"
        />
      </DateRangeBar>

      <ReportBody state={state} onRetry={state.reload} label="báo cáo gia hạn">
        {(data) => {
          const rate = data.rate ?? null;
          return (
            <div className="space-y-4">
              <div className="grid gap-3 sm:grid-cols-3">
                <StatCard label="Gói hết hạn trong kỳ" value={formatNumber(data.expiredCount)} />
                <StatCard label="Gói đã gia hạn" value={formatNumber(data.renewedCount)} />
                <StatCard label="Tỷ lệ gia hạn" value={rate === null ? "Chưa có" : formatPercent(rate)} />
              </div>
              {rate === null ? (
                <EmptyState title="Không có gói nào hết hạn trong khoảng này">Chọn khoảng ngày khác để xem tỷ lệ gia hạn.</EmptyState>
              ) : (
                <div className="panel p-4">
                  <div className="h-3 overflow-hidden rounded-control bg-chalk" role="img" aria-label={`Tỷ lệ gia hạn ${formatPercent(rate)}`}>
                    <div className="h-full bg-plate" style={{ width: `${Math.min(100, Math.max(0, rate * 100))}%` }} />
                  </div>
                </div>
              )}
              <p className="text-xs text-steel">
                Gói hết hạn trong kỳ là gói có ngày hết hạn nằm trong khoảng đã chọn và đã qua. Gói được tính là đã gia hạn nếu hội viên có gói mới bắt đầu không muộn quá {data.windowDays} ngày sau ngày hết hạn (gồm cả gia hạn sớm). Gói chưa thanh toán, đã huỷ, đang chờ kích hoạt và gói không có ngày hết hạn không được tính. Đếm theo gói, không theo hội viên.
              </p>
            </div>
          );
        }}
      </ReportBody>
    </div>
  );
}
