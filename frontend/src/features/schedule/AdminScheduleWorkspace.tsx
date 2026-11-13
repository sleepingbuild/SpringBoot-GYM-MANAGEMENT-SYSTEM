"use client";

import { useEffect, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Button";
import { PageHeader } from "@/components/ui/PageHeader";
import { errorMessage } from "@/lib/api/errors";
import { listTrainers } from "./api";
import { ScheduleManager } from "./ScheduleManager";
import type { TrainerOption } from "./types";

const trainerLabel = (t: TrainerOption) => t.fullName ?? "Huấn luyện viên";

/** Super Admin: chon mot huan luyen vien roi xem, khai, tat/bat, xoa ca lam viec ho. */
export function AdminScheduleWorkspace() {
  const [trainers, setTrainers] = useState<TrainerOption[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState("");

  useEffect(() => {
    let cancelled = false;
    listTrainers()
      .then((list) => !cancelled && setTrainers(list))
      .catch((e) => !cancelled && setLoadError(errorMessage(e)));
    return () => {
      cancelled = true;
    };
  }, []);

  if (loadError) return <Alert tone="error">{loadError}</Alert>;

  if (trainers === null) {
    return (
      <div className="flex justify-center py-16 text-steel" role="status" aria-label="Đang tải danh sách huấn luyện viên">
        <Spinner />
      </div>
    );
  }

  if (trainers.length === 0) {
    return (
      <div className="panel p-8 text-center">
        <p className="text-sm font-medium">Chưa có huấn luyện viên nào đang hoạt động.</p>
      </div>
    );
  }

  const selected = trainers.find((t) => t.id === selectedId) ?? null;

  return (
    <div>
      <PageHeader
        title="Lịch làm việc của huấn luyện viên"
        description="Chọn một huấn luyện viên để xem và khai ca làm việc thay họ."
      />
      <div className="panel mb-6 max-w-md p-5">
        <label htmlFor="admin-trainer" className="mb-1 block text-sm font-medium">
          Huấn luyện viên
        </label>
        <select
          id="admin-trainer"
          className="w-full rounded border px-3 py-2 text-sm"
          value={selectedId}
          onChange={(e) => setSelectedId(e.target.value)}
        >
          <option value="">Chọn huấn luyện viên</option>
          {trainers.map((t) => (
            <option key={t.id} value={t.id}>
              {trainerLabel(t)}
            </option>
          ))}
        </select>
      </div>

      {selected ? (
        <ScheduleManager key={selected.id} trainerId={selected.id} trainerName={selected.fullName} />
      ) : (
        <p className="text-sm text-steel">Chưa chọn huấn luyện viên.</p>
      )}
    </div>
  );
}
