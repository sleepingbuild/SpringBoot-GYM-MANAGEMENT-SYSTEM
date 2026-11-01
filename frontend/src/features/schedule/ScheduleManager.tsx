"use client";

import { useEffect, useMemo, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button, Spinner } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { PageHeader } from "@/components/ui/PageHeader";
import { errorMessage } from "@/lib/api/errors";
import { useToast } from "@/lib/toast";
import { createSchedule, deleteSchedule, listMySchedules, updateSchedule } from "./api";
import type { TrainerSchedule } from "./types";

const RANGE_DAYS = 30;

const pad = (n: number) => String(n).padStart(2, "0");
const toIsoDate = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const addDays = (d: Date, n: number) => {
  const x = new Date(d);
  x.setDate(x.getDate() + n);
  return x;
};
/** Backend tra HH:mm:ss; chi hien HH:mm. Khong cong tru mui gio. */
const hhmm = (t: string) => t.slice(0, 5);

function dayLabel(isoDate: string) {
  return new Date(`${isoDate}T00:00:00`).toLocaleDateString("vi-VN", {
    weekday: "long",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

export function ScheduleManager() {
  const toast = useToast();
  const [items, setItems] = useState<TrainerSchedule[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  const [workDate, setWorkDate] = useState("");
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const today = new Date();
    listMySchedules(toIsoDate(today), toIsoDate(addDays(today, RANGE_DAYS)))
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

  const groups = useMemo(() => {
    const sorted = [...(items ?? [])].sort((a, b) =>
      a.workDate === b.workDate ? a.startTime.localeCompare(b.startTime) : a.workDate.localeCompare(b.workDate),
    );
    const map = new Map<string, TrainerSchedule[]>();
    for (const s of sorted) map.set(s.workDate, [...(map.get(s.workDate) ?? []), s]);
    return Array.from(map.entries());
  }, [items]);

  async function onAdd(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (!workDate || !startTime || !endTime) {
      setError("Chọn đủ ngày, giờ bắt đầu và giờ kết thúc.");
      return;
    }
    if (endTime <= startTime) {
      setError("Giờ kết thúc phải sau giờ bắt đầu.");
      return;
    }
    setSaving(true);
    try {
      await createSchedule({ workDate, startTime, endTime });
      toast.success("Đã thêm ca làm việc.");
      setStartTime("");
      setEndTime("");
      reload();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  async function onToggle(s: TrainerSchedule) {
    setError(null);
    setBusyId(s.id);
    try {
      await updateSchedule(s.id, {
        workDate: s.workDate,
        startTime: s.startTime,
        endTime: s.endTime,
        active: !s.active,
      });
      toast.success(s.active ? "Đã tạm tắt ca làm việc." : "Đã bật lại ca làm việc.");
      reload();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusyId(null);
    }
  }

  async function onDelete(s: TrainerSchedule) {
    if (!window.confirm(`Xoá ca ${hhmm(s.startTime)} - ${hhmm(s.endTime)} ngày ${s.workDate}?`)) return;
    setError(null);
    setBusyId(s.id);
    try {
      await deleteSchedule(s.id);
      toast.success("Đã xoá ca làm việc.");
      reload();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div>
      <PageHeader
        title="Lịch dạy"
        description={`Khai các ca bạn có thể nhận học viên. Hiển thị ${RANGE_DAYS} ngày tới.`}
      />

      <form onSubmit={onAdd} className="panel mb-6 max-w-xl space-y-4 p-5" noValidate>
        <p className="text-sm font-medium">Thêm ca làm việc</p>
        {error && <Alert tone="error">{error}</Alert>}
        <div className="grid gap-4 sm:grid-cols-3">
          <Field label="Ngày" type="date" value={workDate} onChange={(e) => setWorkDate(e.target.value)} />
          <Field label="Từ giờ" type="time" value={startTime} onChange={(e) => setStartTime(e.target.value)} />
          <Field label="Đến giờ" type="time" value={endTime} onChange={(e) => setEndTime(e.target.value)} />
        </div>
        <Button type="submit" loading={saving}>
          Thêm ca
        </Button>
      </form>

      {loadError ? (
        <Alert tone="error">{loadError}</Alert>
      ) : items === null ? (
        <div className="flex justify-center py-16 text-steel" role="status" aria-label="Đang tải lịch dạy">
          <Spinner />
        </div>
      ) : groups.length === 0 ? (
        <div className="panel p-8 text-center">
          <p className="text-sm font-medium">Bạn chưa khai ca làm việc nào trong {RANGE_DAYS} ngày tới.</p>
          <p className="mt-1 text-sm text-steel">Dùng biểu mẫu phía trên để thêm ca đầu tiên. Học viên chỉ đặt được trong các ca này.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {groups.map(([date, list]) => (
            <section key={date} className="panel p-4">
              <h2 className="mb-2 text-sm font-semibold capitalize">{dayLabel(date)}</h2>
              <ul className="divide-y">
                {list.map((s) => (
                  <li key={s.id} className="flex flex-wrap items-center justify-between gap-2 py-2">
                    <div className="text-sm">
                      <span className="font-medium">
                        {hhmm(s.startTime)} - {hhmm(s.endTime)}
                      </span>
                      {!s.active && <span className="ml-2 text-steel">(đang tắt)</span>}
                    </div>
                    <div className="flex gap-3 text-sm">
                      <button
                        type="button"
                        className="underline disabled:opacity-50"
                        disabled={busyId === s.id}
                        onClick={() => onToggle(s)}
                      >
                        {s.active ? "Tạm tắt" : "Bật lại"}
                      </button>
                      <button
                        type="button"
                        className="underline disabled:opacity-50"
                        disabled={busyId === s.id}
                        onClick={() => onDelete(s)}
                      >
                        Xoá
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            </section>
          ))}
        </div>
      )}
    </div>
  );
}
