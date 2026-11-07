"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { EmptyState } from "@/components/ui/EmptyState";
import { getMyAttendance } from "./api";
import { LoadGate, stateFromError } from "./loadState";
import type { LoadState } from "./loadState";
import { StaffAttendanceTable } from "./StaffAttendanceTable";
import type { StaffAttendanceItem } from "./types";

/** Bảng chấm công của chính mình (Lễ tân, Sales). */
export function MyAttendance() {
  const [state, setState] = useState<LoadState<StaffAttendanceItem[]>>({ status: "loading" });
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setState({ status: "loading" });
    getMyAttendance(controller.signal)
      .then((data) => setState({ status: "ready", data }))
      .catch((e: unknown) => {
        const next = stateFromError(e);
        if (next) setState(next);
      });
    return () => controller.abort();
  }, [reloadKey]);

  return (
    <LoadGate
      state={state}
      onRetry={() => setReloadKey((k) => k + 1)}
      loadingText="Đang tải bảng chấm công..."
      forbiddenText="Bảng chấm công cá nhân chỉ dành cho lễ tân và nhân viên kinh doanh."
      errorTitle="Không tải được bảng chấm công"
    >
      {(rows) =>
        rows.length === 0 ? (
          <EmptyState
            title="Chưa có bản ghi chấm công nào"
            action={
              <Link href="/check-in" className="btn-secondary">
                Đến trang tự điểm danh
              </Link>
            }
          >
            Quét khuôn mặt khi bắt đầu ca để ghi nhận giờ vào.
          </EmptyState>
        ) : (
          <>
            <StaffAttendanceTable rows={rows} mode="mine" />
            <p className="mt-3 text-sm text-steel">Trạng thái &quot;về sớm&quot; chỉ xác định được sau khi bạn quét ra.</p>
          </>
        )
      }
    </LoadGate>
  );
}
