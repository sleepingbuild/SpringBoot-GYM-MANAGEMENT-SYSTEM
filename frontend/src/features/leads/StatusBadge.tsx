import { Badge, type BadgeTone } from "@/components/ui/Badge";
import { STATUS_LABELS } from "./status";
import type { LeadStatus } from "./types";

const TONE: Record<LeadStatus, BadgeTone> = {
  NEW: "info",
  CONTACTED: "warning",
  CONVERTED: "success",
  LOST: "danger",
};

/** Có chữ chứ không chỉ màu để người khiếm thị màu vẫn phân biệt được. */
export function StatusBadge({ status }: { status: LeadStatus }) {
  return <Badge tone={TONE[status]}>{STATUS_LABELS[status]}</Badge>;
}
