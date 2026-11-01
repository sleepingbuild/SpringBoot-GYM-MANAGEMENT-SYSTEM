"use client";

import { useState } from "react";
import { PageHeader } from "@/components/ui/PageHeader";
import { PendingPayments } from "./PendingPayments";
import { SellForMember } from "./SellForMember";
import { Tabs } from "./Tabs";

type TabId = "pending" | "sell";

export function PaymentsDesk() {
  const [tab, setTab] = useState<TabId>("pending");
  return (
    <div>
      <PageHeader title="Thanh toán" description="Xác nhận đã thu tiền tại quầy và bán gói cho hội viên." />
      <Tabs
        label="Thanh toán"
        value={tab}
        onChange={setTab}
        tabs={[
          { id: "pending", label: "Chờ xác nhận" },
          { id: "sell", label: "Bán gói hộ hội viên" },
        ]}
      />
      {tab === "pending" && <PendingPayments />}
      {tab === "sell" && <SellForMember />}
    </div>
  );
}
