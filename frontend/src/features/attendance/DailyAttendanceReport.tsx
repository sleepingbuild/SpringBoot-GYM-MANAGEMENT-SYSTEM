"use client";

import { useEffect, useState } from "react";
import { EmptyState } from "@/components/ui/EmptyState";
import { getAttendanceByDate } from "./api";
import { toIsoDate } from "./format";
import { LoadGate, stateFromError } from "./loadState";
import type { LoadState } from "./loadState";
import { StaffAttendanceTable } from "./StaffAttendanceTable";
import type { StaffAttendanceItem } from "./types";

/** Báo cáo chấm công toàn bộ nhân sự theo ngày (Quản trị viên). */
export function DailyAttendanceReport() {
  const [date, setDate] = useState(() => toIsoDate(new Date()));
  const [state, setState] = useState<LoadState<StaffAttendanceItem[]>>({ status: "loading" });
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setState({ status: "loading" });
    getAttendanceByDate(date, controller.signal)
      .then((data) => setState({ status: "ready", data }))
      .catch((e: unknown) => {
        const next = stateFromError(e);
        if (next) setState(next);
      });
    return () => controller.abort();
  }, [date, reloadKey]);

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-2 text-sm">
        <label htmlFor="attendance-date" className="font-semibold">
          Ngày
        </label>
        <input
          id="attendance-date"
          type="date"
          value={date}
          onChange={(e) => {
            if (e.target.value) setDate(e.target.value);
          }}
          className="rounded-control border px-3 py-2"
        />
      </div>
      <LoadGate
        state={state}
        onRetry={() => setReloadKey((k) => k + 1)}
        loadingText="Đang tải báo cáo chấm công..."
        forbiddenText="Báo cáo chấm công theo ngày chỉ dành cho quản trị viên."
        errorTitle="Không tải được báo cáo chấm công"
      >
        {(rows) =>
          rows.length === 0 ? (
            <EmptyState title="Không có bản ghi chấm công trong ngày này">
              Chỉ hiển thị nhân sự đã có bản ghi chấm công. Thử chọn một ngày khác.
            </EmptyState>
          ) : (
            <StaffAttendanceTable rows={rows} mode="report" />
          )
        }
      </LoadGate>
    </div>
  );
}
