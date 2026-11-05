"use client";

import { useState } from "react";
import { EmptyState } from "@/components/ui/EmptyState";
import { getCommissionsSummary } from "./api";
import { formatMoney, formatNumber, formatPeriod } from "./format";
import { defaultMonthRange } from "./monthRange";
import { MonthRangeBar } from "./MonthRangeBar";
import { ReportBody } from "./ReportBody";
import { StatCard } from "./StatCard";
import type { MonthRange } from "./types";
import { useReport } from "./useReport";

export function CommissionsReport() {
  const [applied, setApplied] = useState<MonthRange>(defaultMonthRange());
  const state = useReport(`${applied.from}|${applied.to}`, (signal) => getCommissionsSummary(applied, signal));

  return (
    <div>
      <MonthRangeBar applied={applied} onApply={setApplied} />

      <ReportBody state={state} onRetry={state.reload} label="báo cáo lương huấn luyện viên">
        {(data) => (
          <div className="space-y-4">
            <div className="grid gap-3 sm:grid-cols-3">
              <StatCard label="Tổng lương theo buổi" value={formatMoney(data.totalAmount)} note={`${formatNumber(data.totalSessions)} buổi`} />
              <StatCard label="Chờ trả" value={formatMoney(data.pendingAmount)} note={`${formatNumber(data.pendingCount)} buổi`} />
              <StatCard label="Đã trả" value={formatMoney(data.paidAmount)} note={`${formatNumber(data.paidCount)} buổi`} />
            </div>

            {data.totalSessions === 0 ? (
              <EmptyState title="Chưa có lương theo buổi nào trong khoảng này">Chọn khoảng tháng khác hoặc kiểm tra các buổi tập đã chốt.</EmptyState>
            ) : (
              <div className="panel overflow-x-auto">
                <table className="w-full text-sm">
                  <caption className="sr-only">Lương huấn luyện viên theo tháng</caption>
                  <thead>
                    <tr className="border-b border-line text-left text-xs text-steel">
                      <th scope="col" className="p-3 font-medium">Tháng</th>
                      <th scope="col" className="p-3 text-right font-medium">Số buổi</th>
                      <th scope="col" className="p-3 text-right font-medium">Tổng tiền</th>
                      <th scope="col" className="p-3 text-right font-medium">Chờ trả</th>
                      <th scope="col" className="p-3 text-right font-medium">Đã trả</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-line">
                    {data.months.map((m) => (
                      <tr key={m.month}>
                        <th scope="row" className="p-3 text-left font-medium">{formatPeriod(m.month)}</th>
                        <td className="p-3 text-right">{formatNumber(m.totalSessions)}</td>
                        <td className="p-3 text-right">{formatMoney(m.totalAmount)}</td>
                        <td className="p-3 text-right">{formatMoney(m.pendingAmount)}</td>
                        <td className="p-3 text-right">{formatMoney(m.paidAmount)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <p className="text-xs text-steel">
              Mỗi buổi đã chốt (hoàn thành, hội viên vắng mặt hoặc huỷ sát giờ) ghi một khoản lương, tính vào tháng ghi nhận khoản đó. Chờ trả là khoản chưa đánh dấu đã trả. Số tiền do module lương theo buổi tính theo mức lương đã cài cho từng huấn luyện viên.
            </p>
          </div>
        )}
      </ReportBody>
    </div>
  );
}
