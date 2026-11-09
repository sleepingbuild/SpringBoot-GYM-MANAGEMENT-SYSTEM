"use client";

import { useState } from "react";
import { useAuth } from "@/lib/auth/AuthContext";
import { hasAnyRole } from "@/lib/roles";
import { BookingCreate } from "./BookingCreate";
import { MemberBookings } from "./MemberBookings";
import { StaffBookings } from "./StaffBookings";

type TabKey = "mine" | "create";

const MEMBER_TABS: { key: TabKey; label: string }[] = [
  { key: "mine", label: "Buổi của tôi" },
  { key: "create", label: "Đặt lịch" },
];

const STAFF_TABS: { key: TabKey; label: string }[] = [
  { key: "mine", label: "Lịch theo ngày" },
  { key: "create", label: "Đặt hộ hội viên" },
];

/**
 * Hoi vien: tab "Buoi cua toi" va "Dat lich".
 * Le tan, admin: tab "Lich theo ngay" va "Dat ho hoi vien".
 */
export function BookingsWorkspace() {
  const { user } = useAuth();
  const [tab, setTab] = useState<TabKey>("mine");

  if (!user) return null;
  const isMember = hasAnyRole(user.roles, ["MEMBER"]);
  const tabs = isMember ? MEMBER_TABS : STAFF_TABS;

  return (
    <div>
      <div role="tablist" aria-label="Đặt lịch PT" className="mb-6 flex gap-4 border-b">
        {tabs.map((t) => (
          <button
            key={t.key}
            type="button"
            role="tab"
            aria-selected={tab === t.key}
            onClick={() => setTab(t.key)}
            className={
              tab === t.key
                ? "-mb-px border-b-2 border-current px-1 pb-2 text-sm font-semibold"
                : "px-1 pb-2 text-sm text-steel"
            }
          >
            {t.label}
          </button>
        ))}
      </div>
      {tab === "mine" ? (
        isMember ? <MemberBookings /> : <StaffBookings />
      ) : (
        <BookingCreate staff={!isMember} onBooked={() => setTab("mine")} />
      )}
    </div>
  );
}
