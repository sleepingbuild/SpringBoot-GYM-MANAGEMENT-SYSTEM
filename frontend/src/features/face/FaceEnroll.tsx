"use client";

import { useState } from "react";
import { EmptyState } from "@/components/ui/EmptyState";
import { EnrollPanel } from "./EnrollPanel";
import type { MemberSummary } from "./enrollTypes";
import { MemberPicker } from "./MemberPicker";

/** Lễ tân / Super Admin chọn hội viên, rồi đăng ký, xem hoặc xóa hồ sơ khuôn mặt của họ. */
export function FaceEnroll() {
  const [member, setMember] = useState<MemberSummary | null>(null);
  return (
    <div className="grid gap-6 lg:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
      <section aria-label="Chọn hội viên">
        <MemberPicker selectedId={member?.id ?? null} onSelect={setMember} />
      </section>
      <section aria-label="Hồ sơ khuôn mặt">
        {member ? (
          <EnrollPanel key={member.id} member={member} />
        ) : (
          <EmptyState title="Chọn một hội viên">Tìm và chọn hội viên ở bên trái để xem hoặc đăng ký khuôn mặt hộ.</EmptyState>
        )}
      </section>
    </div>
  );
}
