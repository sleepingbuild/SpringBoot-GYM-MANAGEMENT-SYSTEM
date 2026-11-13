"use client";

import { useEffect, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Button";
import { getMyMembership } from "@/features/membership/api";
import type { MemberPackage } from "@/features/membership/types";
import { listMyBookings } from "@/features/schedule/bookingsApi";
import { errorMessage } from "@/lib/api/errors";
import { computeMemberStats, type MemberStats } from "./memberStats";

interface Loaded {
  stats: MemberStats;
  membership: MemberPackage | null;
}

function todayLocal(): string {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

const dateLabel = (iso: string) => new Date(`${iso}T00:00:00`).toLocaleDateString("vi-VN", { weekday: "short", day: "2-digit", month: "2-digit", year: "numeric" });

function Stat({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div className="panel p-4">
      <div className="text-sm text-steel">{label}</div>
      <div className="mt-1 text-2xl font-semibold">{value}</div>
      {hint && <div className="mt-1 text-xs text-steel">{hint}</div>}
    </div>
  );
}

/** Thống kê cá nhân của hội viên, tính từ gói đang dùng và danh sách buổi của chính mình. */
export function MemberStatsPanel() {
  const [data, setData] = useState<Loaded | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    Promise.all([getMyMembership({ signal: controller.signal }), listMyBookings()])
      .then(([membership, bookings]) => {
        if (controller.signal.aborted) return;
        setData({ membership, stats: computeMemberStats(bookings, todayLocal()) });
      })
      .catch((e) => !controller.signal.aborted && setError(errorMessage(e)));
    return () => controller.abort();
  }, []);

  if (error) return <Alert tone="error">{error}</Alert>;
  if (!data) {
    return (
      <div className="flex justify-center py-8 text-steel" role="status" aria-label="Đang tải thống kê">
        <Spinner />
      </div>
    );
  }

  const { stats, membership } = data;
  const next = stats.nextBooking;
  return (
    <section aria-label="Thống kê cá nhân" className="mt-8">
      <h2 className="mb-3 text-lg font-semibold">Thống kê cá nhân</h2>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        <Stat
          label="Số buổi còn lại"
          value={!membership ? "—" : membership.remainingSessions === null ? "Không giới hạn" : String(membership.remainingSessions)}
          hint={!membership ? "Bạn chưa có gói đang hoạt động." : `${membership.packageName}${membership.endDate ? `, hết hạn ${dateLabel(membership.endDate)}` : ""}`}
        />
        <Stat label="Buổi đã hoàn thành" value={String(stats.completed)} hint={`${stats.completedLast30Days} buổi trong 30 ngày gần nhất`} />
        <Stat label="Buổi sắp tới" value={String(stats.upcoming)} hint={next ? `Gần nhất: ${dateLabel(next.bookingDate)} lúc ${next.startTime.slice(0, 5)}` : "Chưa có buổi nào"} />
        <Stat label="Buổi bị tính khi chưa tập" value={String(stats.chargedWithoutTraining)} hint="Huỷ sát giờ hoặc vắng mặt" />
        <Stat label="PT vắng (không tính buổi)" value={String(stats.ptNoShow)} />
      </div>
    </section>
  );
}
