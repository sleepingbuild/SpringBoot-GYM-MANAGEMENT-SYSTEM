import type { Tone } from "./labels";

const TONE_CLASS: Record<Tone, string> = {
  success: "border-moss bg-moss-tint",
  warning: "border-amber bg-amber-tint",
  danger: "border-rubber bg-rubber-tint",
  info: "border-plate bg-plate-tint",
  neutral: "border-steel",
};

export function StatusBadge({ tone, children }: { tone: Tone; children: React.ReactNode }) {
  return (
    <span className={`inline-flex items-center rounded-control border px-2 py-0.5 text-xs font-medium ${TONE_CLASS[tone]}`}>{children}</span>
  );
}
