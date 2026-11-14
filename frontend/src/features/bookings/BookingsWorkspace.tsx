"use client";

import { useState } from "react";
import { useAuth } from "@/lib/auth/AuthContext";
import { hasAnyRole } from "@/lib/roles";
import { BookingCreate } from "./BookingCreate";
import { MemberBookings } from "./MemberBookings";

type TabKey = "mine" | "create" | "offHours";

const MEMBER_TABS: { key: TabKey; label: string }[] = [
  { key: "mine", label: "Buổi của tôi" },
  { key: "create", label: "Đặt lịch" },
  { key: "offHours", label: "Đặt ngoài giờ" },
];

const STAFF_TABS: { key: TabKey; label: string }[] = [
  { key: "create", label: "Đặt hộ hội viên" },
  { key: "offHours", label: "Đặt hộ ngoài giờ" },
];

/**
 * Hoi vien: "Buoi cua toi", "Dat lich" (trong ca, xac nhan ngay), "Dat ngoai gio" (cho PT xac nhan).
 * Le tan, admin: chi con dat ho (trong ca va ngoai gio).
 */
export function BookingsWorkspace() {
  const { user } = useAuth();
  const [tab, setTab] = useState<TabKey | null>(null);

  if (!user) return null;
  const isMember = hasAnyRole(user.roles, ["MEMBER"]);
  const tabs = isMember ? MEMBER_TABS : STAFF_TABS;
  const active: TabKey = tab ?? tabs[0].key;

  return (
    <div>
      <div role="tablist" aria-label="Đặt lịch PT" className="mb-6 flex gap-4 border-b">
        {tabs.map((t) => (
          <button
            key={t.key}
            type="button"
            role="tab"
            aria-selected={active === t.key}
            onClick={() => setTab(t.key)}
            className={
              active === t.key
                ? "-mb-px border-b-2 border-current px-1 pb-2 text-sm font-semibold"
                : "px-1 pb-2 text-sm text-steel"
            }
          >
            {t.label}
          </button>
        ))}
      </div>
      {active === "mine" ? (
        <MemberBookings />
      ) : (
        // key theo tab de doi giua "trong ca" va "ngoai gio" thi form dat lai tu dau.
        <BookingCreate
          key={active}
          staff={!isMember}
          offHours={active === "offHours"}
          onBooked={() => {
            // Hoi vien dat xong thi ve "Buoi cua toi"; le tan o lai de dat tiep cho nguoi khac.
            if (isMember) setTab("mine");
          }}
        />
      )}
    </div>
  );
}
