import { STATUS_LABELS } from "./status";
import type { LeadStatus } from "./types";

const TONE: Record<LeadStatus, string> = {
  NEW: "bg-plate-tint text-plate-dark",
  CONTACTED: "bg-amber-tint text-amber",
  CONVERTED: "bg-moss-tint text-moss",
  LOST: "bg-rubber-tint text-rubber",
};

/** Có chữ chứ không chỉ màu để người khiếm thị màu vẫn phân biệt được. */
export function StatusBadge({ status }: { status: LeadStatus }) {
  return <span className={`inline-block rounded-control px-2 py-0.5 text-xs font-medium ${TONE[status]}`}>{STATUS_LABELS[status]}</span>;
}
