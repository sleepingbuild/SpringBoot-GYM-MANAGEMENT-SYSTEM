"use client";

import { useEffect, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { PageHeader } from "@/components/ui/PageHeader";
import { ApiError } from "@/lib/api/errors";
import { useToast } from "@/lib/toast";
import { createBooking, listAvailableSlots, listBranches, listTrainersWithSlots } from "./api";
import { bookingErrorMessage } from "./bookingErrors";
import { buildCreateInput, missingMemberHint } from "./createBookingRules";
import { addDays, toIsoDate } from "./dates";
import { MemberPicker } from "./MemberPicker";
import type { AvailableSlot, AvailableTrainer, BranchOption, MemberOption } from "./types";

const hhmm = (t: string) => t.slice(0, 5);
const INPUT_CLASS = "w-full rounded border px-3 py-2 text-sm";

/**
 * Chon ngay, huan luyen vien, khung gio roi dat.
 * staff = true (le tan, admin): phai chon hoi vien truoc va dat ho.
 */
export function BookingCreate({ staff = false, onBooked }: { staff?: boolean; onBooked: () => void }) {
  const toast = useToast();
  const [member, setMember] = useState<MemberOption | null>(null);
  const [branches, setBranches] = useState<BranchOption[]>([]);
  const [branchId, setBranchId] = useState("");
  const [notes, setNotes] = useState("");
  const [date, setDate] = useState(() => toIsoDate(addDays(new Date(), 1)));
  const [trainers, setTrainers] = useState<AvailableTrainer[] | null>(null);
  const [trainersError, setTrainersError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [slots, setSlots] = useState<AvailableSlot[] | null>(null);
  const [slotsError, setSlotsError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [busyStart, setBusyStart] = useState<string | null>(null);

  // Chi nhanh la tuy chon: neu tai loi thi bo qua, van dat duoc.
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
    setTrainers(null);
    setTrainersError(null);
    listTrainersWithSlots(date)
      .then((list) => !cancelled && setTrainers(list))
      .catch((e) => !cancelled && setTrainersError(bookingErrorMessage(e)));
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
      .catch((e) => !cancelled && setSlotsError(bookingErrorMessage(e)));
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

  const memberHint = missingMemberHint(staff, member?.id ?? null);

  async function book(slot: AvailableSlot) {
    if (memberHint) return;
    const trainer = trainers?.find((t) => t.trainerId === slot.trainerId);
    const who = trainer?.trainerName ?? "huấn luyện viên";
    const forWhom = staff && member ? ` cho ${member.fullName ?? "hội viên"}` : "";
    if (!window.confirm(`Đặt buổi ${hhmm(slot.startTime)} - ${hhmm(slot.endTime)} ngày ${slot.date} với ${who}${forWhom}?`)) {
      return;
    }
    setError(null);
    setBusyStart(slot.startTime);
    try {
      await createBooking(buildCreateInput({ slot, staff, memberId: member?.id ?? null, branchId, notes }));
      toast.success(
        staff
          ? "Đã đặt lịch hộ hội viên. Buổi sẽ được xác nhận khi huấn luyện viên đồng ý."
          : "Đã đặt lịch. Buổi sẽ được xác nhận khi huấn luyện viên đồng ý.",
      );
      onBooked();
    } catch (err) {
      setError(bookingErrorMessage(err));
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
        title={staff ? "Đặt lịch hộ hội viên" : "Đặt lịch với huấn luyện viên"}
        description={
          staff
            ? "Chọn hội viên, ngày và huấn luyện viên còn chỗ trống rồi chọn khung giờ. Buổi sẽ ở trạng thái chờ xác nhận."
            : "Chọn ngày, chọn huấn luyện viên còn chỗ trống rồi chọn khung giờ. Buổi sẽ ở trạng thái chờ xác nhận."
        }
      />

      {staff && <MemberPicker value={member} onChange={setMember} />}

      <div className="panel mb-6 max-w-md space-y-4 p-5">
        <Field
          label="Ngày tập"
          type="date"
          min={toIsoDate(new Date())}
          value={date}
          onChange={(e) => changeDate(e.target.value)}
        />
        {branches.length > 0 && (
          <div>
            <label htmlFor="booking-branch" className="mb-1 block text-sm font-medium">
              Chi nhánh (không bắt buộc)
            </label>
            <select
              id="booking-branch"
              className={INPUT_CLASS}
              value={branchId}
              onChange={(e) => setBranchId(e.target.value)}
            >
              <option value="">Không chọn</option>
              {branches.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name}
                </option>
              ))}
            </select>
          </div>
        )}
        <div>
          <label htmlFor="booking-notes" className="mb-1 block text-sm font-medium">
            Ghi chú (không bắt buộc)
          </label>
          <textarea
            id="booking-notes"
            className={INPUT_CLASS}
            rows={2}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
          />
        </div>
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
              {memberHint && <p className="mb-2 text-sm text-steel">{memberHint}</p>}
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
                      disabled={busyStart !== null || memberHint !== null}
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
