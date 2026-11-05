"use client";

import { Button } from "@/components/ui/Button";

export function Tabs<T extends string>({
  label,
  tabs,
  value,
  onChange,
}: {
  label: string;
  tabs: { id: T; label: string }[];
  value: T;
  onChange: (id: T) => void;
}) {
  return (
    <div role="tablist" aria-label={label} className="mb-4 flex flex-wrap gap-2">
      {tabs.map((t) => (
        <Button key={t.id} role="tab" aria-selected={t.id === value} variant={t.id === value ? "primary" : "secondary"} onClick={() => onChange(t.id)}>
          {t.label}
        </Button>
      ))}
    </div>
  );
}
