"use client";

import { useEffect, useMemo, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { PageHeader } from "@/components/ui/PageHeader";
import { errorMessage } from "@/lib/api/errors";
import { useToast } from "@/lib/toast";
import { cancelBooking } from "@/features/schedule/bookingsApi";
import type { Booking } from "@/features/schedule/bookingTypes";
import { listBookingsByDate, listBranches } from "./api";
import { toIsoDate } from "./dates";
import { cancelDoneMessage, cancelNotice, isOpenBooking } from "./memberBookingRules";
import { STAFF_STATUS_LABEL, countByStatus, sortByStartTime } from "./staffBookingRules";
import type { BranchOption } from "./types";

const hhmm = (t: string) => t.slice(0, 5);

export function StaffBookings() {
  const toast = useToast();
  const [date, setDate] = useState(() => toIsoDate(new Date()));
  const [branches, setBranches] = useState<BranchOption[]>([]);
  const [branchId, setBranchId] = useState("");
  const [items, setItems] = useState<Booking[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  // Bo loc chi nhanh la tuy chon: tai loi thi an bo loc, van xem duoc moi chi nhanh.
  useEffect(() => {
    let cancelled = false;
    listBranches()
      .then((list) => !cancelled && setBranches(list))
      .catch(() => !cancelled && setBranches([]));
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!date) return;
    let cancelled = false;
    setItems(null);
    setLoadError(null);
    listBookingsByDate(date, branchId || undefined)
      .then((list) => !cancelled && setItems(list))
      .catch((e) => !cancelled && setLoadError(errorMessage(e)));
    return () => {
      cancelled = true;
    };
  }, [date, branchId, reloadKey]);

  // Server khong sap theo gio khi loc chi nhanh, nen luon sap o day.
  const sorted = useMemo(() => sortByStartTime(items ?? []), [items]);
  const summary = useMemo(() => countByStatus(items ?? []), [items]);

  async function onCancel(b: Booking) {
    if (!window.confirm(cancelNotice(b.status))) return;
    setError(null);
    setBusyId(b.id);
    try {
      const updated = await cancelBooking(b.id);
      toast.success(cancelDoneMessage(updated.status));
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusyId(null);
      // Tai lai ca khi loi: trang thai co the da doi o phia server.
      setReloadKey((k) => k + 1);
    }
  }

  return (
    <div>
      <PageHeader
        title="Lịch buổi tập theo ngày"
        description="Các buổi tập với huấn luyện viên trong ngày đã chọn, có thể lọc theo chi nhánh. Lễ tân và quản trị viên có thể huỷ buổi hộ học viên."
      />

      <div className="panel mb-6 flex max-w-md flex-wrap items-end gap-4 p-5">
        <div>
          <Field label="Ngày" type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        </div>
        {branches.length > 0 && (
          <div>
            <label htmlFor="staff-branch" className="mb-1 block text-sm font-medium">
              Chi nhánh
            </label>
            <select
              id="staff-branch"
              className="rounded border px-3 py-2 text-sm"
              value={branchId}
              onChange={(e) => setBranchId(e.target.value)}
            >
              <option value="">Tất cả chi nhánh</option>
              {branches.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      {error && (
        <div className="mb-4">
          <Alert tone="error">{error}</Alert>
        </div>
      )}

      {loadError ? (
        <Alert tone="error">{loadError}</Alert>
      ) : items === null ? (
        <div className="flex justify-center py-16 text-steel" role="status" aria-label="Đang tải lịch buổi tập">
          <Spinner />
        </div>
      ) : sorted.length === 0 ? (
        <div className="panel p-8 text-center">
          <p className="text-sm font-medium">Không có buổi tập nào trong ngày này.</p>
          <p className="mt-1 text-sm text-steel">
            {branchId ? "Thử chọn chi nhánh khác hoặc một ngày khác." : "Chọn một ngày khác để xem lịch."}
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          <p className="text-sm text-steel">
            {sorted.length} buổi:{" "}
            {summary.map((s) => `${s.count} ${STAFF_STATUS_LABEL[s.status].toLowerCase()}`).join(", ")}
          </p>
          <ul className="space-y-3">
            {sorted.map((b) => (
              <li key={b.id} className="panel p-4">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div>
                    <p className="text-sm font-medium">
                      {hhmm(b.startTime)} - {hhmm(b.endTime)}
                    </p>
                    <p className="text-sm text-steel">Học viên: {b.memberName ?? "Không rõ"}</p>
                    <p className="text-sm text-steel">Huấn luyện viên: {b.trainerName ?? "Không rõ"}</p>
                  </div>
                  <span className="rounded border px-2 py-0.5 text-xs">{STAFF_STATUS_LABEL[b.status] ?? b.status}</span>
                </div>
                {isOpenBooking(b) && (
                  <div className="mt-3">
                    <button
                      type="button"
                      className="text-sm underline disabled:opacity-50"
                      disabled={busyId === b.id}
                      onClick={() => void onCancel(b)}
                    >
                      Huỷ buổi
                    </button>
                  </div>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
