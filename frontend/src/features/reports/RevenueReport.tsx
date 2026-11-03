"use client";

import { useState } from "react";
import { getRevenue } from "./api";
import { BarChart } from "./BarChart";
import { defaultRange } from "./dateRange";
import { DateRangeBar } from "./DateRangeBar";
import { formatCompact, formatMoney, formatPeriod } from "./format";
import { ReportBody } from "./ReportBody";
import { StatCard } from "./StatCard";
import type { DateRange, RevenueGroupBy } from "./types";
import { useReport } from "./useReport";

export function RevenueReport() {
  const [range, setRange] = useState<DateRange>(() => defaultRange());
  const [groupBy, setGroupBy] = useState<RevenueGroupBy>("DAY");
  const state = useReport(`${range.from}|${range.to}|${groupBy}`, (signal) => getRevenue(range, groupBy, signal));

  return (
    <div>
      <DateRangeBar applied={range} onApply={setRange}>
        <div>
          <label htmlFor="revenue-group" className="mb-1 block text-sm font-medium">
            Nhóm theo
          </label>
          <select id="revenue-group" className="input" value={groupBy} onChange={(e) => setGroupBy(e.target.value as RevenueGroupBy)}>
            <option value="DAY">Ngày</option>
            <option value="MONTH">Tháng</option>
          </select>
        </div>
      </DateRangeBar>

      <ReportBody state={state} onRetry={state.reload} label="báo cáo doanh thu">
        {(data) => {
          const empty = data.total === 0;
          return (
            <div className="space-y-4">
              <div className="grid gap-3 sm:grid-cols-3">
                <StatCard label="Tổng doanh thu" value={formatMoney(data.total)} />
                <StatCard label="Từ gói tập" value={formatMoney(data.membershipTotal)} />
                <StatCard label="Từ bán lẻ (POS)" value={formatMoney(data.posTotal)} />
              </div>
              <p className="text-xs text-steel">
                Tính theo tiền đã thu của các giao dịch thanh toán thành công, ghi nhận vào ngày giao dịch chuyển sang thành công. Đơn bán lẻ đã huỷ không bị trừ vì hệ thống chưa ghi nhận hoàn tiền.
              </p>
              {empty ? (
                <div className="panel px-6 py-10 text-center text-sm text-steel">Không có giao dịch thanh toán thành công trong khoảng ngày này. Thử chọn khoảng dài hơn.</div>
              ) : (
                <div className="panel p-4">
                  <BarChart
                    ariaLabel={`Doanh thu theo ${data.groupBy === "DAY" ? "ngày" : "tháng"} từ ${data.from} đến ${data.to}`}
                    items={data.items.map((i) => ({ label: formatPeriod(i.period), values: [i.membership, i.pos] }))}
                    series={[
                      { name: "Gói tập", fillClass: "fill-plate" },
                      { name: "Bán lẻ", fillClass: "fill-amber" },
                    ]}
                    formatValue={formatMoney}
                    formatAxis={formatCompact}
                  />
                </div>
              )}
              <details className="panel p-4">
                <summary className="cursor-pointer text-sm font-medium">Xem bảng số liệu</summary>
                <div className="mt-3 overflow-x-auto">
                  <table className="w-full min-w-[28rem] text-left text-sm">
                    <thead className="border-b border-line text-xs text-steel">
                      <tr>
                        <th className="py-2 font-medium">Kỳ</th>
                        <th className="py-2 text-right font-medium">Gói tập</th>
                        <th className="py-2 text-right font-medium">Bán lẻ</th>
                        <th className="py-2 text-right font-medium">Tổng</th>
                      </tr>
                    </thead>
                    <tbody>
                      {data.items.map((i) => (
                        <tr key={i.period} className="border-b border-line last:border-0">
                          <td className="py-2">{formatPeriod(i.period)}</td>
                          <td className="py-2 text-right">{formatMoney(i.membership)}</td>
                          <td className="py-2 text-right">{formatMoney(i.pos)}</td>
                          <td className="py-2 text-right font-medium">{formatMoney(i.total)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </details>
            </div>
          );
        }}
      </ReportBody>
    </div>
  );
}
