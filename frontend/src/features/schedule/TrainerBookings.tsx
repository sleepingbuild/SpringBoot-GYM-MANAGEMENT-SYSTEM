"use client";

import { useEffect, useMemo, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Badge } from "@/components/ui/Badge";
import { Button, Spinner } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { PageHeader } from "@/components/ui/PageHeader";
import { errorMessage } from "@/lib/api/errors";
import { useToast } from "@/lib/toast";
import { bookingStatusTone } from "@/features/bookings/statusTone";
import { cancelBooking, completeBooking, confirmBooking, listMyBookings } from "./bookingsApi";
import type { Booking } from "./bookingTypes";
import {
  MIN_COMPLETE_MINUTES,
  MIN_REASON_LENGTH,
  completeBlocker,
  completeWarning,
  completionPlan,
} from "./trainerBookingRules";

const STATUS_LABEL: Record<string, string> = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã huỷ",
  LATE_CANCELLED: "Huỷ muộn",
  NO_SHOW: "Học viên vắng",
  PT_NO_SHOW: "PT vắng",
};

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
  now: Date;
  busy: boolean;
  draft: string;
  onDraft: (v: string) => void;
  onConfirm: () => void;
  onComplete: () => void;
  onCancel: () => void;
}

function BookingCard({ booking: b, now, busy, draft, onDraft, onConfirm, onComplete, onCancel }: CardProps) {
  const memberIn = timeOf(b.checkInTime);
  const ptIn = timeOf(b.ptCheckInTime);
  const ptOut = timeOf(b.ptCheckOutTime);
  const plan = completionPlan(b, now);
  const showTimes = b.status === "CONFIRMED" || b.status === "COMPLETED";
  const memberScanned = Boolean(b.checkInTime);

  return (
    <li className="panel p-4">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <p className="text-sm font-semibold">
            {dateLabel(b.bookingDate)} · {hhmm(b.startTime)} - {hhmm(b.endTime)}
          </p>
          <p className="text-sm text-steel">Học viên: {b.memberName ?? "Không rõ"}</p>
        </div>
        <Badge tone={bookingStatusTone(b.status)}>{STATUS_LABEL[b.status] ?? b.status}</Badge>
      </div>

      {showTimes && (
        <div className="mt-2 space-y-1 text-sm text-steel">
          <p>Học viên: {memberIn ? `đã quét mặt vào lúc ${memberIn}` : "chưa quét mặt vào"}</p>
          <p>
            Bạn: vào {ptIn ?? "–"}, ra {ptOut ?? "–"}
            {b.ptCheckOutMethod === "MANUAL" ? " (ghi lúc bạn bấm hoàn thành)" : ""}
          </p>
          {b.ptEarlyLeaveReason && <p>Lý do rời sớm: {b.ptEarlyLeaveReason}</p>}
        </div>
      )}

      {b.status === "NO_SHOW" && (
        <p className="mt-2 text-sm text-steel">
          Hệ thống tự ghi nhận học viên vắng khi hết thời gian chờ kể từ giờ bắt đầu mà học viên chưa quét mặt.
        </p>
      )}

      {b.status === "CONFIRMED" && (plan.reasonRequired || plan.reasonSuggested) && (
        <div className="mt-3 space-y-2 border-t border-line pt-3">
          <p className="text-sm">
            {plan.reasonRequired
              ? `Buổi mới được ${Math.max(plan.minutes, 0)} phút, chưa đủ ${MIN_COMPLETE_MINUTES} phút. Nhập lý do để hoàn thành.`
              : "Buổi chưa đủ thời lượng theo lịch. Nhập lý do để buổi được tính công (không bắt buộc)."}
          </p>
          <Field
            label={plan.reasonRequired ? "Lý do (bắt buộc)" : "Lý do (không bắt buộc)"}
            maxLength={255}
            value={draft}
            onChange={(e) => onDraft(e.target.value)}
            hint={`Từ ${MIN_REASON_LENGTH} đến 255 ký tự.`}
          />
        </div>
      )}

      {isOpen(b) && (
        <div className="mt-3 flex flex-wrap items-center gap-3">
          {b.status === "PENDING" && (
            <Button loading={busy} onClick={onConfirm}>
              Xác nhận
            </Button>
          )}
          {b.status === "CONFIRMED" && (
            <Button loading={busy} disabled={!memberScanned} onClick={onComplete}>
              Hoàn thành buổi
            </Button>
          )}
          <Button variant="danger" disabled={busy} onClick={onCancel}>
            Huỷ buổi
          </Button>
          {b.status === "CONFIRMED" && !memberScanned && (
            <p className="text-sm text-steel">Chỉ hoàn thành được khi học viên đã quét mặt vào.</p>
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
  // Cap nhat "bay gio" moi 30 giay de so phut da day va yeu cau ly do luon dung khi PT mo man hinh lau.
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 30000);
    return () => clearInterval(id);
  }, []);

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

  function onComplete(b: Booking) {
    const plan = completionPlan(b, new Date());
    const draft = drafts[b.id] ?? "";
    const blocker = completeBlocker(plan, draft);
    if (blocker) {
      setError(blocker);
      return;
    }
    const warning = completeWarning(plan, draft);
    if (warning && !window.confirm(warning)) return;
    const reason = draft.trim();
    void run(b, () => completeBooking(b.id, reason || undefined), "Đã hoàn thành buổi tập.");
  }

  const renderCard = (b: Booking) => (
    <BookingCard
      key={b.id}
      booking={b}
      now={now}
      busy={busyId === b.id}
      draft={drafts[b.id] ?? ""}
      onDraft={(v) => setDrafts((d) => ({ ...d, [b.id]: v }))}
      onConfirm={() => void run(b, () => confirmBooking(b.id), "Đã xác nhận buổi tập.")}
      onComplete={() => onComplete(b)}
      onCancel={() => onCancel(b)}
    />
  );

  return (
    <div>
      <PageHeader
        icon="🏋️"
        title="Buổi dạy"
        description="Xác nhận buổi do học viên đặt ngoài giờ, và hoàn thành buổi sau khi học viên đã quét mặt. Không cần quét mặt ra: buổi dưới 45 phút thì nhập lý do."
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
            Khi học viên đặt lịch, buổi sẽ hiện ở đây. Buổi trong ca của bạn được xác nhận ngay; buổi đặt ngoài giờ
            chờ bạn xác nhận.
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
