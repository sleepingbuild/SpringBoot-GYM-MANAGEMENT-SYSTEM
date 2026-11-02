"use client";

import { useState } from "react";
import { PageHeader } from "@/components/ui/PageHeader";
import { useAuth } from "@/lib/auth/AuthContext";
import { AdminCommissions } from "./AdminCommissions";
import { MyCommissions } from "./MyCommissions";
import { PayRateManager } from "./PayRateManager";

type TabId = "list" | "rates";

/** Super Admin: tra cứu lương và đặt mức lương mỗi buổi. Trainer: chỉ xem lương của mình. */
export function CommissionsWorkspace() {
  const { user } = useAuth();
  const [tab, setTab] = useState<TabId>("list");
  const isAdmin = Boolean(user?.roles.includes("SUPER_ADMIN"));

  if (!isAdmin) return <MyCommissions />;

  const tabs: { id: TabId; label: string }[] = [
    { id: "list", label: "Tra cứu lương" },
    { id: "rates", label: "Mức lương mỗi buổi" },
  ];

  return (
    <div>
      <PageHeader title="Lương theo buổi" description="Tra cứu, đánh dấu chi trả lương PT và đặt mức lương mỗi buổi." />
      <div role="tablist" aria-label="Quản lý lương PT" className="mb-5 flex gap-2 border-b border-line">
        {tabs.map((t) => (
          <button
            key={t.id}
            role="tab"
            type="button"
            aria-selected={tab === t.id}
            onClick={() => setTab(t.id)}
            className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium ${tab === t.id ? "border-plate text-plate" : "border-transparent text-steel hover:text-ink"}`}
          >
            {t.label}
          </button>
        ))}
      </div>
      {tab === "list" ? <AdminCommissions /> : <PayRateManager />}
    </div>
  );
}
