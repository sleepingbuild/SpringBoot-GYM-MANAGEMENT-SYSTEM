"use client";

import { useEffect, useMemo, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Button";
import { PageHeader } from "@/components/ui/PageHeader";
import { errorMessage } from "@/lib/api/errors";
import { useToast } from "@/lib/toast";
import { cancelBooking, listMyBookings } from "@/features/schedule/bookingsApi";
import type { Booking } from "@/features/schedule/bookingTypes";
import { MEMBER_STATUS_LABEL, cancelDoneMessage, cancelNotice, isOpenBooking, splitMemberBookings } from "./memberBookingRules";

const hhmm = (t: string) => t.slice(0, 5);

function dateLabel(iso: string) {
  return new Date(`${iso}T00:00:00`).toLocaleDateString("vi-VN", {
    weekday: "short",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

export function MemberBookings() {
  const toast = useToast();
  const [items, setItems] = useState<Booking[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listMyBookings()
      .then((list) => {
        if (cancelled) return;
        setItems(list);
        setLoadError(null);
      })
      .catch((e) => !cancelled && setLoadError(errorMessage(e)));
    return () => {
      cancelled = true;
    };
  }, [reloadKey]);

  const reload = () => setReloadKey((k) => k + 1);
  const { upcoming, past } = useMemo(() => splitMemberBookings(items ?? []), [items]);

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
      reload();
    }
  }

  const renderCard = (b: Booking) => (
    <li key={b.id} className="panel p-4">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <p className="text-sm font-medium">
            {dateLabel(b.bookingDate)} · {hhmm(b.startTime)} - {hhmm(b.endTime)}
          </p>
          <p className="text-sm text-steel">Huấn luyện viên: {b.trainerName ?? "Không rõ"}</p>
          {b.notes && <p className="text-sm text-steel">Ghi chú: {b.notes}</p>}
        </div>
        <span className="rounded border px-2 py-0.5 text-xs">{MEMBER_STATUS_LABEL[b.status] ?? b.status}</span>
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
  );

  return (
    <div>
      <PageHeader
        title="Buổi tập của tôi"
        description="Các buổi tập với huấn luyện viên bạn đã đặt. Buổi chờ PT xác nhận huỷ miễn phí; buổi đã xác nhận mà huỷ sát giờ sẽ bị tính."
      />

      {error && (
        <div className="mb-4">
          <Alert tone="error">{error}</Alert>
        </div>
      )}

      {loadError ? (
        <Alert tone="error">{loadError}</Alert>
      ) : items === null ? (
        <div className="flex justify-center py-16 text-steel" role="status" aria-label="Đang tải buổi tập">
          <Spinner />
        </div>
      ) : items.length === 0 ? (
        <div className="panel p-8 text-center">
          <p className="text-sm font-medium">Bạn chưa đặt buổi tập nào với huấn luyện viên.</p>
          <p className="mt-1 text-sm text-steel">Chức năng đặt lịch mới sẽ có ở bản cập nhật tới.</p>
        </div>
      ) : (
        <div className="space-y-6">
          <section>
            <h2 className="mb-2 text-sm font-semibold">Sắp tới ({upcoming.length})</h2>
            {upcoming.length === 0 ? (
              <p className="text-sm text-steel">Không có buổi nào sắp tới.</p>
            ) : (
              <ul className="space-y-3">{upcoming.map(renderCard)}</ul>
            )}
          </section>
          <section>
            <h2 className="mb-2 text-sm font-semibold">Đã kết thúc ({past.length})</h2>
            {past.length === 0 ? (
              <p className="text-sm text-steel">Chưa có buổi nào kết thúc.</p>
            ) : (
              <ul className="space-y-3">{past.map(renderCard)}</ul>
            )}
          </section>
        </div>
      )}
    </div>
  );
}
