"use client";

import { useState } from "react";
import { getCheckinTraffic } from "./api";
import { BarChart } from "./BarChart";
import { defaultRange } from "./dateRange";
import { DateRangeBar } from "./DateRangeBar";
import { formatNumber, formatPeriod } from "./format";
import { ReportBody } from "./ReportBody";
import { StatCard } from "./StatCard";
import type { DateRange } from "./types";
import { useReport } from "./useReport";

export function TrafficReport() {
  const [range, setRange] = useState<DateRange>(() => defaultRange());
  const state = useReport(`${range.from}|${range.to}`, (signal) => getCheckinTraffic(range, signal));
  const axis = (n: number) => formatNumber(Math.round(n));
  const unit = (n: number) => `${formatNumber(n)} lượt`;

  return (
    <div>
      <DateRangeBar applied={range} onApply={setRange} />
      <ReportBody state={state} onRetry={state.reload} label="báo cáo lưu lượng">
        {(data) => (
          <div className="space-y-4">
            <StatCard label="Tổng lượt vào buổi tập có PT" value={formatNumber(data.total)} />
            <p className="text-xs text-steel">
              Chỉ tính lượt học viên quét mặt vào buổi tập có huấn luyện viên; mỗi lịch đặt tính một lượt. Hiện hệ thống chưa ghi nhận lượt vào phòng tập chung.
            </p>
            {data.total === 0 ? (
              <div className="panel px-6 py-10 text-center text-sm text-steel">Không có lượt check-in nào trong khoảng ngày này.</div>
            ) : (
              <>
                <section className="panel p-4">
                  <h3 className="mb-2 text-base font-semibold">Theo ngày</h3>
                  <BarChart
                    ariaLabel={`Số lượt vào buổi tập theo ngày từ ${data.from} đến ${data.to}`}
                    items={data.byDay.map((d) => ({ label: formatPeriod(d.date), values: [d.count] }))}
                    series={[{ name: "Lượt", fillClass: "fill-plate" }]}
                    formatValue={unit}
                    formatAxis={axis}
                    integerAxis
                  />
                </section>
                <section className="panel p-4">
                  <h3 className="mb-2 text-base font-semibold">Theo giờ trong ngày</h3>
                  <BarChart
                    ariaLabel="Số lượt vào buổi tập theo giờ trong ngày"
                    items={data.byHour.map((h) => ({ label: `${h.hour}h`, values: [h.count] }))}
                    series={[{ name: "Lượt", fillClass: "fill-moss" }]}
                    formatValue={unit}
                    formatAxis={axis}
                    labelEvery={2}
                    integerAxis
                  />
                </section>
              </>
            )}
          </div>
        )}
      </ReportBody>
    </div>
  );
}
