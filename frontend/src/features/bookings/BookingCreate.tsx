"use client";

import { useEffect, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Badge } from "@/components/ui/Badge";
import { Spinner } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { PageHeader } from "@/components/ui/PageHeader";
import { ApiError } from "@/lib/api/errors";
import { useToast } from "@/lib/toast";
import { getMyMembership } from "@/features/membership/api";
import type { MemberPackage } from "@/features/membership/types";
import {
  createBooking,
  listAvailableSlots,
  listBranches,
  listOffHoursSlots,
  listTrainers,
  listTrainersWithSlots,
} from "./api";
import { bookingErrorMessage } from "./bookingErrors";
import { buildCreateInput, missingMemberHint } from "./createBookingRules";
import { addDays, toIsoDate } from "./dates";
import { MemberPicker } from "./MemberPicker";
import { bookedMessage, slotLabel, toTrainerChoices } from "./offHoursRules";
import { SessionBanner } from "./SessionBanner";
import { sessionSummary } from "./sessionRules";
import type { AvailableSlot, BranchOption, MemberOption, TrainerChoice } from "./types";

/** O khung gio: ba trang thai (thuong, hover, khoa). */
const SLOT_CLASS =
  "rounded-control border border-line-strong px-3 py-2 text-sm transition-colors hover:border-plate hover:bg-plate-tint disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:border-line-strong disabled:hover:bg-transparent";

/**
 * Chon ngay, huan luyen vien, khung gio roi dat.
 * staff = true (le tan, admin): phai chon hoi vien truoc va dat ho.
 * offHours = true: dat khung ngoai gio (cuoi ngay), buoi o trang thai cho PT xac nhan.
 * offHours = false: dat trong ca lam viec cua PT, buoi duoc xac nhan ngay.
 */
export function BookingCreate({
  staff = false,
  offHours = false,
  onBooked,
}: {
  staff?: boolean;
  offHours?: boolean;
  onBooked: () => void;
}) {
  const toast = useToast();
  const [member, setMember] = useState<MemberOption | null>(null);
  const [branches, setBranches] = useState<BranchOption[]>([]);
  const [branchId, setBranchId] = useState("");
  const [notes, setNotes] = useState("");
  const [date, setDate] = useState(() => toIsoDate(addDays(new Date(), 1)));
  const [trainers, setTrainers] = useState<TrainerChoice[] | null>(null);
  const [trainersError, setTrainersError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [slots, setSlots] = useState<AvailableSlot[] | null>(null);
  const [slotsError, setSlotsError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [busyStart, setBusyStart] = useState<string | null>(null);

  // Hội viên tự đặt: xem số buổi còn lại (GET /member-packages/me). Lỗi tải thì bỏ qua, không chặn đặt.
  const [membership, setMembership] = useState<MemberPackage | null | undefined>(undefined);
  useEffect(() => {
    if (staff) return;
    let cancelled = false;
    getMyMembership()
      .then((m) => !cancelled && setMembership(m))
      .catch(() => !cancelled && setMembership(undefined));
    return () => {
      cancelled = true;
    };
  }, [staff, reloadKey]);
  const summary = !staff && membership !== undefined ? sessionSummary(membership) : null;
  const sessionBlocked = summary?.blocked ?? false;

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
    // Dat trong ca: chi PT con slot trong trong ngay. Dat ngoai gio: moi PT dang hoat dong.
    const load: Promise<TrainerChoice[]> = offHours
      ? listTrainers().then(toTrainerChoices)
      : listTrainersWithSlots(date);
    load
      .then((list) => !cancelled && setTrainers(list))
      .catch((e) => !cancelled && setTrainersError(bookingErrorMessage(e)));
    return () => {
      cancelled = true;
    };
  }, [date, offHours, reloadKey]);

  useEffect(() => {
    if (!selectedId || !date) return;
    let cancelled = false;
    setSlots(null);
    setSlotsError(null);
    const load: Promise<AvailableSlot[]> = offHours
      ? listOffHoursSlots(selectedId, date)
      : listAvailableSlots(selectedId, date);
    load
      .then((list) => !cancelled && setSlots(list))
      .catch((e) => !cancelled && setSlotsError(bookingErrorMessage(e)));
    return () => {
      cancelled = true;
    };
  }, [selectedId, date, offHours, reloadKey]);

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
    const needsApproval = offHours ? " Buổi sẽ chờ huấn luyện viên xác nhận." : "";
    if (
      !window.confirm(
        `Đặt buổi ${slotLabel(slot.startTime, slot.endTime)} ngày ${slot.date} với ${who}${forWhom}?${needsApproval}`,
      )
    ) {
      return;
    }
    setError(null);
    setBusyStart(slot.startTime);
    try {
      const created = await createBooking(buildCreateInput({ slot, staff, memberId: member?.id ?? null, branchId, notes }));
      toast.success(bookedMessage(created.status, staff));
      setNotes("");
      // Khung vua dat khong con trong: tai lai danh sach de no bien mat.
      setReloadKey((k) => k + 1);
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

  const title = offHours
    ? staff
      ? "Đặt ngoài giờ hộ hội viên"
      : "Đặt lịch ngoài giờ"
    : staff
      ? "Đặt lịch hộ hội viên"
      : "Đặt lịch với huấn luyện viên";

  const description = offHours
    ? `${staff ? "Chọn hội viên, ngày" : "Chọn ngày"}, huấn luyện viên và khung giờ ngoài giờ làm việc (cuối ngày). Buổi sẽ chờ huấn luyện viên xác nhận.`
    : `${staff ? "Chọn hội viên, ngày" : "Chọn ngày"} và huấn luyện viên còn chỗ trống rồi chọn khung giờ. Đặt trong ca làm việc của huấn luyện viên thì buổi được xác nhận ngay.`;

  const slotsPanel = (
    <section className="panel p-5">
      <h2 className="mb-3 text-sm font-semibold">{offHours ? "Khung giờ ngoài giờ còn trống" : "Khung giờ trống"}</h2>
      {memberHint && <p className="mb-2 text-sm text-steel">{memberHint}</p>}
      {!selectedId ? (
        <p className="text-sm text-steel">Chọn huấn luyện viên để xem khung giờ trống.</p>
      ) : slotsError ? (
        <Alert tone="error">{slotsError}</Alert>
      ) : slots === null ? (
        <div className="flex justify-center py-8 text-steel" role="status" aria-label="Đang tải khung giờ">
          <Spinner />
        </div>
      ) : slots.length === 0 ? (
        <p className="text-sm text-steel">
          {offHours
            ? "Huấn luyện viên này không còn khung giờ ngoài giờ nào trống trong ngày. Chọn người khác hoặc ngày khác."
            : "Huấn luyện viên này vừa hết chỗ. Chọn người khác hoặc ngày khác."}
        </p>
      ) : (
        <div className="flex flex-wrap gap-2">
          {slots.map((s) => (
            <button
              key={s.startTime}
              type="button"
              disabled={busyStart !== null || memberHint !== null || sessionBlocked}
              onClick={() => void book(s)}
              className={SLOT_CLASS}
            >
              {slotLabel(s.startTime, s.endTime)}
            </button>
          ))}
        </div>
      )}
    </section>
  );

  return (
    <div>
      <PageHeader icon={offHours ? "🌙" : "📅"} title={title} description={description} />

      {summary && <SessionBanner summary={summary} />}

      {error && (
        <div className="mb-4">
          <Alert tone="error">{error}</Alert>
        </div>
      )}

      <div className="page-split">
        {/* Cot chinh: hoi vien (le tan), ngay, chi nhanh, ghi chu, chon PT */}
        <div className="space-y-6">
          {staff && <MemberPicker value={member} onChange={setMember} />}

          <div className="panel space-y-4 p-5">
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
                <select id="booking-branch" className="input" value={branchId} onChange={(e) => setBranchId(e.target.value)}>
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
              <textarea id="booking-notes" className="input" rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} />
            </div>
          </div>

          {trainersError ? (
            <Alert tone="error">{trainersError}</Alert>
          ) : trainers === null ? (
            <div className="flex justify-center py-12 text-steel" role="status" aria-label="Đang tải huấn luyện viên">
              <Spinner />
            </div>
          ) : trainers.length === 0 ? (
            <div className="panel p-8 text-center">
              <p className="text-sm font-medium">
                {offHours
                  ? "Chưa có huấn luyện viên nào đang hoạt động."
                  : "Không có huấn luyện viên nào còn chỗ trống trong ngày này."}
              </p>
              {!offHours && <p className="mt-1 text-sm text-steel">Thử chọn một ngày khác, hoặc đặt ngoài giờ.</p>}
            </div>
          ) : (
            <section>
              <h2 className="mb-2 text-sm font-semibold">
                {offHours ? "Huấn luyện viên" : `Huấn luyện viên còn chỗ (${trainers.length})`}
              </h2>
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
                        ? "panel border-plate bg-plate-tint px-4 py-3 text-left text-sm font-semibold"
                        : "panel px-4 py-3 text-left text-sm transition-colors hover:border-line-strong hover:bg-raised"
                    }
                  >
                    <span className="block">{t.trainerName ?? "Huấn luyện viên"}</span>
                    {t.availableSlotCount !== undefined && (
                      <Badge tone="success" className="mt-1">
                        {t.availableSlotCount} khung giờ trống
                      </Badge>
                    )}
                  </button>
                ))}
              </div>
            </section>
          )}
        </div>

        {/* Cot phu: khung gio trong cua PT da chon */}
        {slotsPanel}
      </div>
    </div>
  );
}
