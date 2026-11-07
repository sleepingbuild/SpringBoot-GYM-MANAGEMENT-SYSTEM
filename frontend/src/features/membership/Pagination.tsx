"use client";

import { Button } from "@/components/ui/Button";

/** Phân trang đơn giản. `page` bắt đầu từ 0 như PageResponse của backend. Ẩn khi chỉ có một trang. */
export function Pagination({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (page: number) => void }) {
  if (totalPages <= 1) return null;
  return (
    <nav className="mt-4 flex items-center justify-between gap-3 text-sm" aria-label="Phân trang">
      <Button variant="secondary" disabled={page <= 0} onClick={() => onChange(page - 1)}>
        Trang trước
      </Button>
      <span className="text-steel">
        Trang {page + 1} / {totalPages}
      </span>
      <Button variant="secondary" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        Trang sau
      </Button>
    </nav>
  );
}
