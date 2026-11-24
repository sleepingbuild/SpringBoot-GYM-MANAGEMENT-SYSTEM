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
      <div role="tablist" aria-label="Lịch dạy" className="mb-6 flex flex-wrap gap-2">
        {TABS.map((t) => (
          <button
            key={t.key}
            type="button"
            role="tab"
            aria-selected={tab === t.key}
            onClick={() => setTab(t.key)}
            className={tab === t.key ? "pill pill-active" : "pill"}
          >
            {t.label}
          </button>
        ))}
      </div>
      {tab === "bookings" ? <TrainerBookings /> : <ScheduleManager />}
    </div>
  );
}
