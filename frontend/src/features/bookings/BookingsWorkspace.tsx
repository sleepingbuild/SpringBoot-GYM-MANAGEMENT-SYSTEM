"use client";

import { useState } from "react";
import { useAuth } from "@/lib/auth/AuthContext";
import { hasAnyRole } from "@/lib/roles";
import { BookingCreate } from "./BookingCreate";
import { MemberBookings } from "./MemberBookings";

type TabKey = "mine" | "create";

const MEMBER_TABS: { key: TabKey; label: string }[] = [
  { key: "mine", label: "Buổi của tôi" },
  { key: "create", label: "Đặt lịch" },
];

/**
 * Hoi vien: tab "Buoi cua toi" va "Dat lich".
 * Le tan, admin: chi con man hinh "Dat ho hoi vien" (khong con lich theo ngay).
 */
export function BookingsWorkspace() {
  const { user } = useAuth();
  const [tab, setTab] = useState<TabKey>("mine");

  if (!user) return null;
  const isMember = hasAnyRole(user.roles, ["MEMBER"]);
  if (!isMember) return <BookingCreate staff onBooked={() => undefined} />;
  const tabs = MEMBER_TABS;

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
      {tab === "mine" ? <MemberBookings /> : <BookingCreate staff={false} onBooked={() => setTab("mine")} />}
    </div>
  );
}
