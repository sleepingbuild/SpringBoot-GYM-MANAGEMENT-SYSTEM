"use client";

import { useState } from "react";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { ReportBody } from "@/features/reports/ReportBody";
import { useReport } from "@/features/reports/useReport";
import { listExpiringMembers } from "./api";
import { formatDate, formatDaysLeft } from "./format";

const DAY_OPTIONS = [7, 14, 30, 60, 90];

/** Hội viên có gói ACTIVE sắp hết hạn, để Sales liên hệ gia hạn. */
export function ExpiringMembers() {
  const [days, setDays] = useState(7);
  const state = useReport(String(days), (signal) => listExpiringMembers(days, signal));

  return (
    <div>
      <PageHeader
        title="Hội viên sắp hết hạn"
        description="Hội viên có gói đang hoạt động sắp hết hạn, sắp theo ngày hết hạn. Những người đã đăng ký gói tiếp theo không có trong danh sách."
      />

      <div className="panel mb-4 flex flex-wrap items-end gap-3 p-4">
        <div>
          <label htmlFor="expiring-days" className="mb-1 block text-sm font-medium">
            Hết hạn trong
          </label>
          <select id="expiring-days" className="input" value={days} onChange={(e) => setDays(Number(e.target.value))}>
            {DAY_OPTIONS.map((d) => (
              <option key={d} value={d}>
                {d} ngày tới
              </option>
            ))}
          </select>
        </div>
      </div>

      <ReportBody state={state} onRetry={state.reload} label="danh sách hội viên sắp hết hạn">
        {(members) =>
          members.length === 0 ? (
            <EmptyState title={`Không có hội viên nào hết hạn trong ${days} ngày tới`}>
              Thử chọn khoảng thời gian dài hơn.
            </EmptyState>
          ) : (
            <div className="panel overflow-x-auto">
              <table className="w-full min-w-[44rem] text-left text-sm">
                <caption className="sr-only">Hội viên sắp hết hạn</caption>
                <thead className="border-b border-line text-xs text-steel">
                  <tr>
                    <th scope="col" className="px-4 py-3 font-medium">Hội viên</th>
                    <th scope="col" className="px-4 py-3 font-medium">Gói</th>
                    <th scope="col" className="px-4 py-3 font-medium">Hết hạn</th>
                    <th scope="col" className="px-4 py-3 font-medium">Buổi còn lại</th>
                  </tr>
                </thead>
                <tbody>
                  {members.map((m) => (
                    <tr key={m.userId} className="border-b border-line align-top last:border-0">
                      <td className="px-4 py-3">
                        <p className="font-medium">{m.fullName}</p>
                        <p className="text-steel">{m.phone || "Chưa có số điện thoại"}</p>
                        <p className="text-xs text-steel">{m.email}</p>
                      </td>
                      <td className="px-4 py-3">{m.packageName}</td>
                      <td className="whitespace-nowrap px-4 py-3">
                        <p>{formatDate(m.endDate)}</p>
                        <p className={m.daysLeft <= 3 ? "text-xs font-medium text-rubber" : "text-xs text-steel"}>{formatDaysLeft(m.daysLeft)}</p>
                      </td>
                      <td className="px-4 py-3">{m.remainingSessions ?? "Không giới hạn"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )
        }
      </ReportBody>
    </div>
  );
}
