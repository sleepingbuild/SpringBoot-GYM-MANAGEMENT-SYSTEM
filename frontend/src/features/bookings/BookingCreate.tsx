"use client";

import { useEffect, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { PageHeader } from "@/components/ui/PageHeader";
import { ApiError, errorMessage } from "@/lib/api/errors";
import { useToast } from "@/lib/toast";
import { createBooking, listAvailableSlots, listTrainersWithSlots } from "./api";
import { addDays, toIsoDate } from "./dates";
import type { AvailableSlot, AvailableTrainer } from "./types";

const hhmm = (t: string) => t.slice(0, 5);

export function BookingCreate({ onBooked }: { onBooked: () => void }) {
  const toast = useToast();
  const [date, setDate] = useState(() => toIsoDate(addDays(new Date(), 1)));
  const [trainers, setTrainers] = useState<AvailableTrainer[] | null>(null);
  const [trainersError, setTrainersError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [slots, setSlots] = useState<AvailableSlot[] | null>(null);
  const [slotsError, setSlotsError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [busyStart, setBusyStart] = useState<string | null>(null);

  useEffect(() => {
    if (!date) return;
    let cancelled = false;
    setTrainers(null);
    setTrainersError(null);
    listTrainersWithSlots(date)
      .then((list) => !cancelled && setTrainers(list))
      .catch((e) => !cancelled && setTrainersError(errorMessage(e)));
    return () => {
      cancelled = true;
    };
  }, [date, reloadKey]);

  useEffect(() => {
    if (!selectedId || !date) return;
    let cancelled = false;
    setSlots(null);
    setSlotsError(null);
    listAvailableSlots(selectedId, date)
      .then((list) => !cancelled && setSlots(list))
      .catch((e) => !cancelled && setSlotsError(errorMessage(e)));
    return () => {
      cancelled = true;
    };
  }, [selectedId, date, reloadKey]);

  function changeDate(value: string) {
    setDate(value);
    setSelectedId(null);
    setSlots(null);
    setError(null);
  }

  async function book(slot: AvailableSlot) {
    const trainer = trainers?.find((t) => t.trainerId === slot.trainerId);
    const who = trainer?.trainerName ?? "huấn luyện viên";
    if (!window.confirm(`Đặt buổi ${hhmm(slot.startTime)} - ${hhmm(slot.endTime)} ngày ${slot.date} với ${who}?`)) return;
    setError(null);
    setBusyStart(slot.startTime);
    try {
      await createBooking({
        trainerId: slot.trainerId,
        bookingDate: slot.date,
        startTime: slot.startTime,
        endTime: slot.endTime,
      });
      toast.success("Đã đặt lịch. Buổi sẽ được xác nhận khi huấn luyện viên đồng ý.");
      onBooked();
    } catch (err) {
      setError(errorMessage(err));
      // Co nguoi khac dat truoc: tai lai danh sach slot de thay dung cho con trong.
      if (err instanceof ApiError && err.errorCode === "BOOKING_SLOT_TAKEN") {
        setReloadKey((k) => k + 1);
      }
    } finally {
      setBusyStart(null);
    }
  }

  return (
    <div>
      <PageHeader
        title="Đặt lịch với huấn luyện viên"
        description="Chọn ngày, chọn huấn luyện viên còn chỗ trống rồi chọn khung giờ. Buổi sẽ ở trạng thái chờ xác nhận."
      />

      <div className="panel mb-6 max-w-xs p-5">
        <Field
          label="Ngày tập"
          type="date"
          min={toIsoDate(new Date())}
          value={date}
          onChange={(e) => changeDate(e.target.value)}
        />
      </div>

      {error && (
        <div className="mb-4">
          <Alert tone="error">{error}</Alert>
        </div>
      )}

      {trainersError ? (
        <Alert tone="error">{trainersError}</Alert>
      ) : trainers === null ? (
        <div className="flex justify-center py-12 text-steel" role="status" aria-label="Đang tải huấn luyện viên">
          <Spinner />
        </div>
      ) : trainers.length === 0 ? (
        <div className="panel p-8 text-center">
          <p className="text-sm font-medium">Không có huấn luyện viên nào còn chỗ trống trong ngày này.</p>
          <p className="mt-1 text-sm text-steel">Thử chọn một ngày khác.</p>
        </div>
      ) : (
        <div className="space-y-6">
          <section>
            <h2 className="mb-2 text-sm font-semibold">Huấn luyện viên còn chỗ ({trainers.length})</h2>
            <div className="flex flex-wrap gap-3">
              {trainers.map((t) => (
                <button
                  key={t.trainerId}
                  type="button"
                  aria-pressed={selectedId === t.trainerId}
                  onClick={() => {
                    setSelectedId(t.trainerId);
                    setError(null);
                  }}
                  className={
                    selectedId === t.trainerId
                      ? "panel border-current px-4 py-3 text-left text-sm font-semibold"
                      : "panel px-4 py-3 text-left text-sm"
                  }
                >
                  <span className="block">{t.trainerName ?? "Huấn luyện viên"}</span>
                  <span className="block text-xs font-normal text-steel">{t.availableSlotCount} khung giờ trống</span>
                </button>
              ))}
            </div>
          </section>

          {selectedId && (
            <section>
              <h2 className="mb-2 text-sm font-semibold">Khung giờ trống</h2>
              {slotsError ? (
                <Alert tone="error">{slotsError}</Alert>
              ) : slots === null ? (
                <div className="flex justify-center py-8 text-steel" role="status" aria-label="Đang tải khung giờ">
                  <Spinner />
                </div>
              ) : slots.length === 0 ? (
                <p className="text-sm text-steel">Huấn luyện viên này vừa hết chỗ. Chọn người khác hoặc ngày khác.</p>
              ) : (
                <div className="flex flex-wrap gap-2">
                  {slots.map((s) => (
                    <button
                      key={s.startTime}
                      type="button"
                      disabled={busyStart !== null}
                      onClick={() => void book(s)}
                      className="rounded border px-3 py-2 text-sm disabled:opacity-50"
                    >
                      {hhmm(s.startTime)} - {hhmm(s.endTime)}
                    </button>
                  ))}
                </div>
              )}
            </section>
          )}
        </div>
      )}
    </div>
  );
}
