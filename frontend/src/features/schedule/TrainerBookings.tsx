"use client";

import { useEffect, useMemo, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button, Spinner } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { PageHeader } from "@/components/ui/PageHeader";
import { errorMessage } from "@/lib/api/errors";
import { useToast } from "@/lib/toast";
import { cancelBooking, completeBooking, confirmBooking, listMyBookings, submitEarlyLeaveReason } from "./bookingsApi";
import type { Booking } from "./bookingTypes";

const STATUS_LABEL: Record<string, string> = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã huỷ",
  LATE_CANCELLED: "Huỷ muộn",
  NO_SHOW: "Học viên vắng",
  PT_NO_SHOW: "PT vắng",
};

const MIN_REASON = 5;

const isOpen = (b: Booking) => b.status === "PENDING" || b.status === "CONFIRMED";
const hhmm = (t: string) => t.slice(0, 5);
/** LocalDateTime dang yyyy-MM-ddTHH:mm:ss -> HH:mm. Khong cong tru mui gio. */
const timeOf = (dt: string | null) => (dt ? dt.slice(11, 16) : null);
const sortKey = (b: Booking) => `${b.bookingDate}T${b.startTime}`;

function dateLabel(iso: string) {
  return new Date(`${iso}T00:00:00`).toLocaleDateString("vi-VN", {
    weekday: "short",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

interface CardProps {
  booking: Booking;
  busy: boolean;
  draft: string;
  onDraft: (v: string) => void;
  onConfirm: () => void;
  onComplete: () => void;
  onCancel: () => void;
  onSubmitReason: () => void;
}

function BookingCard({ booking: b, busy, draft, onDraft, onConfirm, onComplete, onCancel, onSubmitReason }: CardProps) {
  const memberIn = timeOf(b.checkInTime);
  const ptIn = timeOf(b.ptCheckInTime);
  const ptOut = timeOf(b.ptCheckOutTime);
  const readyToComplete = Boolean(b.checkInTime && b.ptCheckInTime && b.ptCheckOutTime);
  const needsReason = b.status === "CONFIRMED" && Boolean(b.ptCheckOutTime) && b.fullDuration === false;

  return (
    <li className="panel p-4">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <p className="text-sm font-medium">
            {dateLabel(b.bookingDate)} · {hhmm(b.startTime)} - {hhmm(b.endTime)}
          </p>
          <p className="text-sm text-steel">Học viên: {b.memberName ?? "Không rõ"}</p>
        </div>
        <span className="rounded border px-2 py-0.5 text-xs">{STATUS_LABEL[b.status] ?? b.status}</span>
      </div>

      {b.status === "CONFIRMED" && (
        <div className="mt-2 space-y-1 text-sm text-steel">
          <p>Học viên: {memberIn ? `đã quét mặt vào lúc ${memberIn}` : "chưa quét mặt vào"}</p>
          <p>
            Bạn: {ptIn ? `vào ${ptIn}` : "chưa quét mặt vào"}
            {ptOut ? `, ra ${ptOut}` : ptIn ? ", chưa quét ra" : ""}
          </p>
          {b.ptEarlyLeaveReason && <p>Lý do rời sớm: {b.ptEarlyLeaveReason}</p>}
        </div>
      )}

      {needsReason && (
        <div className="mt-3 space-y-2 border-t pt-3">
          <p className="text-sm">Bạn quét ra trước giờ kết thúc. Nộp lý do để buổi được tính đủ thời lượng.</p>
          <Field
            label="Lý do rời buổi sớm"
            maxLength={255}
            value={draft}
            onChange={(e) => onDraft(e.target.value)}
            hint={`Từ ${MIN_REASON} đến 255 ký tự.`}
          />
          <Button type="button" loading={busy} onClick={onSubmitReason}>
            Nộp lý do
          </Button>
        </div>
      )}

      {isOpen(b) && (
        <div className="mt-3 flex flex-wrap items-center gap-3">
          {b.status === "PENDING" && (
            <Button type="button" loading={busy} onClick={onConfirm}>
              Xác nhận
            </Button>
          )}
          {b.status === "CONFIRMED" && (
            <Button type="button" loading={busy} disabled={!readyToComplete} onClick={onComplete}>
              Hoàn thành buổi
            </Button>
          )}
          <button type="button" className="text-sm underline disabled:opacity-50" disabled={busy} onClick={onCancel}>
            Huỷ buổi
          </button>
          {b.status === "CONFIRMED" && !readyToComplete && (
            <p className="text-sm text-steel">
              Chỉ hoàn thành được khi học viên và bạn đã quét mặt vào, và bạn đã quét ra.
            </p>
          )}
        </div>
      )}
    </li>
  );
}

export function TrainerBookings() {
  const toast = useToast();
  const [items, setItems] = useState<Booking[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [drafts, setDrafts] = useState<Record<string, string>>({});

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

  const { upcoming, past } = useMemo(() => {
    const all = items ?? [];
    return {
      upcoming: all.filter(isOpen).sort((a, b) => sortKey(a).localeCompare(sortKey(b))),
      past: all.filter((b) => !isOpen(b)).sort((a, b) => sortKey(b).localeCompare(sortKey(a))),
    };
  }, [items]);

  async function run(b: Booking, action: () => Promise<unknown>, okMessage: string) {
    setError(null);
    setBusyId(b.id);
    try {
      await action();
      toast.success(okMessage);
      reload();
    } catch (err) {
      setError(errorMessage(err));
      // Co the trang thai da doi o phia server (vi du request khac chot truoc): tai lai de thay dung.
      reload();
    } finally {
      setBusyId(null);
    }
  }

  function onCancel(b: Booking) {
    const ok = window.confirm(
      "Huỷ buổi này? Học viên không bị trừ buổi và bạn không được tính công cho buổi này.",
    );
    if (ok) void run(b, () => cancelBooking(b.id), "Đã huỷ buổi.");
  }

  function onSubmitReason(b: Booking) {
    const reason = (drafts[b.id] ?? "").trim();
    if (reason.length < MIN_REASON) {
      setError(`Lý do phải từ ${MIN_REASON} ký tự trở lên.`);
      return;
    }
    void run(b, () => submitEarlyLeaveReason(b.id, reason), "Đã ghi nhận lý do rời sớm.");
  }

  const renderCard = (b: Booking) => (
    <BookingCard
      key={b.id}
      booking={b}
      busy={busyId === b.id}
      draft={drafts[b.id] ?? ""}
      onDraft={(v) => setDrafts((d) => ({ ...d, [b.id]: v }))}
      onConfirm={() => void run(b, () => confirmBooking(b.id), "Đã xác nhận buổi tập.")}
      onComplete={() => void run(b, () => completeBooking(b.id), "Đã hoàn thành buổi tập.")}
      onCancel={() => onCancel(b)}
      onSubmitReason={() => onSubmitReason(b)}
    />
  );

  return (
    <div>
      <PageHeader
        title="Buổi dạy"
        description="Xác nhận buổi do học viên đặt, hoàn thành buổi sau khi cả hai đã quét mặt, và nộp lý do nếu rời buổi sớm."
      />

      {error && (
        <div className="mb-4">
          <Alert tone="error">{error}</Alert>
        </div>
      )}

      {loadError ? (
        <Alert tone="error">{loadError}</Alert>
      ) : items === null ? (
        <div className="flex justify-center py-16 text-steel" role="status" aria-label="Đang tải buổi dạy">
          <Spinner />
        </div>
      ) : items.length === 0 ? (
        <div className="panel p-8 text-center">
          <p className="text-sm font-medium">Bạn chưa có buổi dạy nào.</p>
          <p className="mt-1 text-sm text-steel">
            Khi học viên đặt lịch trong các ca bạn đã khai ở tab Ca làm việc, buổi sẽ hiện ở đây để bạn xác nhận.
          </p>
        </div>
      ) : (
        <div className="space-y-6">
          <section>
            <h2 className="mb-2 text-sm font-semibold">Cần xử lý và sắp tới ({upcoming.length})</h2>
            {upcoming.length === 0 ? (
              <p className="text-sm text-steel">Không có buổi nào đang chờ.</p>
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
