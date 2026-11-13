"use client";

import { useAuth } from "@/lib/auth/AuthContext";
import { hasAnyRole } from "@/lib/roles";
import { AdminScheduleWorkspace } from "./AdminScheduleWorkspace";
import { TrainerWorkspace } from "./TrainerWorkspace";

/** PT: tab "Buoi day" va "Ca lam viec" nhu cu. Super Admin: chon PT roi quan ly ca. */
export function ScheduleWorkspace() {
  const { user } = useAuth();
  if (!user) return null;
  return hasAnyRole(user.roles, ["TRAINER"]) ? <TrainerWorkspace /> : <AdminScheduleWorkspace />;
}
