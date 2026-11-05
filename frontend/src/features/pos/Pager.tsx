import { Button } from "@/components/ui/Button";

/** Phân trang đơn giản. `page` bắt đầu từ 0 (đúng PageResponse của backend). */
export function Pager({ page, totalPages, onPage }: { page: number; totalPages: number; onPage: (p: number) => void }) {
  if (totalPages <= 1) return null;
  return (
    <div className="mt-3 flex items-center justify-end gap-3 text-sm">
      <Button variant="secondary" disabled={page <= 0} onClick={() => onPage(page - 1)}>
        Trang trước
      </Button>
      <span>
        Trang {page + 1} / {totalPages}
      </span>
      <Button variant="secondary" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>
        Trang sau
      </Button>
    </div>
  );
}
