"use client";

import { useState } from "react";
import { ExpiringMembers } from "./ExpiringMembers";
import { LeadsPage } from "./LeadsPage";

const TABS = [
  { id: "leads", label: "Khách tiềm năng" },
  { id: "expiring", label: "Hội viên sắp hết hạn" },
] as const;

type TabId = (typeof TABS)[number]["id"];

/** Hai việc của Sales trên một trang: chăm khách tiềm năng và nhắc hội viên sắp hết hạn gia hạn. */
export function LeadsHome() {
  const [tab, setTab] = useState<TabId>("leads");

  return (
    <div>
      <div role="tablist" aria-label="Công việc của Sales" className="mb-4 flex flex-wrap gap-1 border-b border-line">
        {TABS.map((t) => (
          <button
            key={t.id}
            type="button"
            role="tab"
            id={`leads-tab-${t.id}`}
            aria-selected={tab === t.id}
            aria-controls={`leads-panel-${t.id}`}
            onClick={() => setTab(t.id)}
            className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium ${tab === t.id ? "border-plate text-plate" : "border-transparent text-steel hover:text-ink"}`}
          >
            {t.label}
          </button>
        ))}
      </div>
      <div role="tabpanel" id={`leads-panel-${tab}`} aria-labelledby={`leads-tab-${tab}`}>
        {tab === "leads" ? <LeadsPage /> : <ExpiringMembers />}
      </div>
    </div>
  );
}
