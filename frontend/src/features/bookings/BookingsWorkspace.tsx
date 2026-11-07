"use client";

import { useState } from "react";
import { useAuth } from "@/lib/auth/AuthContext";
import { hasAnyRole } from "@/lib/roles";
import { BookingCreate } from "./BookingCreate";
import { MemberBookings } from "./MemberBookings";
import { StaffBookings } from "./StaffBookings";

const TABS = [
  { key: "mine", label: "Buổi của tôi" },
  { key: "create", label: "Đặt lịch" },
] as const;

type TabKey = (typeof TABS)[number]["key"];

/** Hoi vien: hai tab (buoi cua toi, dat lich). Le tan va admin: lich theo ngay. */
export function BookingsWorkspace() {
  const { user } = useAuth();
  const [tab, setTab] = useState<TabKey>("mine");

  if (!user) return null;
  if (!hasAnyRole(user.roles, ["MEMBER"])) return <StaffBookings />;

  return (
    <div>
      <div role="tablist" aria-label="Đặt lịch PT" className="mb-6 flex gap-4 border-b">
        {TABS.map((t) => (
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
      {tab === "mine" ? <MemberBookings /> : <BookingCreate onBooked={() => setTab("mine")} />}
    </div>
  );
}
