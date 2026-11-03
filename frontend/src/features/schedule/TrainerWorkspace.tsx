"use client";

import { useState } from "react";
import { ScheduleManager } from "./ScheduleManager";
import { TrainerBookings } from "./TrainerBookings";

const TABS = [
  { key: "bookings", label: "Buổi dạy" },
  { key: "schedule", label: "Ca làm việc" },
] as const;

type TabKey = (typeof TABS)[number]["key"];

export function TrainerWorkspace() {
  const [tab, setTab] = useState<TabKey>("bookings");

  return (
    <div>
      <div role="tablist" aria-label="Lịch dạy" className="mb-6 flex gap-4 border-b">
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
      {tab === "bookings" ? <TrainerBookings /> : <ScheduleManager />}
    </div>
  );
}
