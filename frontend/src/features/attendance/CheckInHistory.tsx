"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button, Spinner } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { ApiError, errorMessage } from "@/lib/api/errors";
import { getMyCheckIns } from "./api";
import { checkInTypeLabel, formatDateTime, methodLabel } from "./format";
import type { CheckInHistoryItem } from "./types";

type State =
  | { status: "loading" }
  | { status: "ready"; items: CheckInHistoryItem[] }
  | { status: "forbidden" }
  | { status: "error"; message: string };

/** Lịch sử vào phòng tập của hội viên (M17): đủ bốn trạng thái đang tải, rỗng, lỗi API, không có quyền. */
export function CheckInHistory() {
  const [state, setState] = useState<State>({ status: "loading" });
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setState({ status: "loading" });
    getMyCheckIns(controller.signal)
      .then((items) => setState({ status: "ready", items }))
      .catch((e: unknown) => {
        if (e instanceof DOMException && e.name === "AbortError") return;
        if (e instanceof ApiError && e.kind === "forbidden") {
          setState({ status: "forbidden" });
          return;
        }
        setState({ status: "error", message: errorMessage(e) });
      });
    return () => controller.abort();
  }, [reloadKey]);

  if (state.status === "loading") {
    return (
      <div className="flex items-center gap-2 py-10 text-sm text-steel" role="status">
        <Spinner />
        Đang tải lịch sử check-in...
      </div>
    );
  }

  if (state.status === "forbidden") {
    return (
      <Alert tone="warning" title="Bạn không có quyền xem trang này">
        Lịch sử check-in chỉ dành cho tài khoản hội viên.
      </Alert>
    );
  }

  if (state.status === "error") {
    return (
      <div className="space-y-3">
        <Alert tone="error" title="Không tải được lịch sử check-in">
          {state.message}
        </Alert>
        <Button variant="secondary" onClick={() => setReloadKey((k) => k + 1)}>
          Thử lại
        </Button>
      </div>
    );
  }

  if (state.items.length === 0) {
    return (
      <EmptyState
        title="Chưa có lượt check-in nào"
        action={
          <Link href="/check-in" className="btn-secondary">
            Đến trang tự điểm danh
          </Link>
        }
      >
        Quét khuôn mặt tại quầy lễ tân hoặc ở trang tự điểm danh để ghi nhận lượt tập đầu tiên.
      </EmptyState>
    );
  }

  return (
    <div className="panel overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="border-b">
            <th className="px-4 py-3 font-semibold">Loại</th>
            <th className="px-4 py-3 font-semibold">Giờ vào</th>
            <th className="px-4 py-3 font-semibold">Giờ ra</th>
            <th className="px-4 py-3 font-semibold">Cách điểm danh</th>
          </tr>
        </thead>
        <tbody>
          {state.items.map((item) => (
            <tr key={`${item.type}-${item.id}`} className="border-b last:border-b-0">
              <td className="px-4 py-3">
                <div>{checkInTypeLabel(item.type)}</div>
                {item.trainerName && <div className="text-steel">PT {item.trainerName}</div>}
              </td>
              <td className="px-4 py-3">{formatDateTime(item.checkInTime)}</td>
              <td className="px-4 py-3">
                {item.checkOutTime ? formatDateTime(item.checkOutTime) : <span className="text-steel">Chưa quét ra</span>}
              </td>
              <td className="px-4 py-3">{methodLabel(item.method)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
