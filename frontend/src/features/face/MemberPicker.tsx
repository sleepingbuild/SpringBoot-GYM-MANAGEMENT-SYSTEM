"use client";

import { useEffect, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button, Spinner } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { errorTone, searchMembers } from "./enrollApi";
import type { MemberSummary } from "./enrollTypes";
import { faceErrorMessage } from "./messages";
import { isUuid } from "./members";

type State =
  | { status: "loading" }
  | { status: "ok"; members: MemberSummary[]; totalElements: number }
  | { status: "error"; message: string; tone: "warning" | "error" };

/** Tìm hội viên theo tên, số điện thoại hoặc email. Có ô nhập mã người dùng (UUID) dự phòng khi tìm kiếm lỗi. */
export function MemberPicker({ selectedId, onSelect }: { selectedId: string | null; onSelect: (m: MemberSummary) => void }) {
  const [q, setQ] = useState("");
  const [debounced, setDebounced] = useState("");
  const [state, setState] = useState<State>({ status: "loading" });
  const [attempt, setAttempt] = useState(0);
  const [uuid, setUuid] = useState("");
  const [uuidError, setUuidError] = useState<string | null>(null);

  useEffect(() => {
    const t = setTimeout(() => setDebounced(q.trim()), 300);
    return () => clearTimeout(t);
  }, [q]);

  useEffect(() => {
    const ctrl = new AbortController();
    setState({ status: "loading" });
    searchMembers(debounced, 0, 10, ctrl.signal)
      .then((r) => {
        if (!ctrl.signal.aborted) setState({ status: "ok", members: r.members, totalElements: r.totalElements });
      })
      .catch((e: unknown) => {
        if (ctrl.signal.aborted) return;
        setState({ status: "error", message: faceErrorMessage(e), tone: errorTone(e) });
      });
    return () => ctrl.abort();
  }, [debounced, attempt]);

  function confirmUuid() {
    const id = uuid.trim();
    if (!isUuid(id)) {
      setUuidError("Mã không đúng dạng UUID (ví dụ 123e4567-e89b-12d3-a456-426614174000).");
      return;
    }
    setUuidError(null);
    onSelect({ id, fullName: `Người dùng ${id.slice(0, 8)}`, phone: null, email: null });
  }

  return (
    <div className="space-y-3">
      <div>
        <label htmlFor="member-search" className="mb-1 block text-sm font-semibold">
          Tìm hội viên
        </label>
        <input
          id="member-search"
          type="search"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Tên, số điện thoại hoặc email"
          className="w-full rounded-control border border-plate px-3 py-2 text-sm"
          autoComplete="off"
        />
      </div>

      {state.status === "loading" && (
        <div className="flex items-center gap-2 text-sm text-steel" role="status">
          <Spinner className="h-4 w-4" />
          Đang tìm...
        </div>
      )}

      {state.status === "error" && (
        <div className="space-y-3">
          <Alert tone={state.tone} title="Không tìm được hội viên">
            {state.message}
          </Alert>
          <Button variant="secondary" onClick={() => setAttempt((n) => n + 1)}>
            Thử lại
          </Button>
        </div>
      )}

      {state.status === "ok" && state.members.length === 0 && (
        <EmptyState title="Không có hội viên phù hợp">Thử từ khóa khác (một phần tên, số điện thoại hoặc email).</EmptyState>
      )}

      {state.status === "ok" && state.members.length > 0 && (
        <>
          <ul className="space-y-2">
            {state.members.map((m) => (
              <li key={m.id}>
                <button
                  type="button"
                  onClick={() => onSelect(m)}
                  aria-pressed={m.id === selectedId}
                  className={`w-full rounded-control border px-3 py-2 text-left text-sm ${
                    m.id === selectedId ? "border-plate bg-plate-tint" : "border-plate/40"
                  }`}
                >
                  <span className="block font-semibold">{m.fullName}</span>
                  <span className="block text-steel">{[m.phone, m.email].filter(Boolean).join(" · ") || "—"}</span>
                </button>
              </li>
            ))}
          </ul>
          {state.totalElements > state.members.length && (
            <p className="text-xs text-steel">
              Hiển thị {state.members.length} trên {state.totalElements} hội viên. Nhập thêm từ khóa để thu hẹp.
            </p>
          )}
        </>
      )}

      <details className="text-sm">
        <summary className="cursor-pointer text-steel">Tìm không ra? Nhập mã người dùng (UUID)</summary>
        <div className="mt-2 space-y-2">
          <input
            aria-label="Mã người dùng (UUID)"
            value={uuid}
            onChange={(e) => setUuid(e.target.value)}
            className="w-full rounded-control border border-plate px-3 py-2 text-sm"
            placeholder="123e4567-e89b-12d3-a456-426614174000"
          />
          {uuidError && <p className="text-xs">{uuidError}</p>}
          <Button variant="secondary" onClick={confirmUuid}>
            Dùng mã này
          </Button>
        </div>
      </details>
    </div>
  );
}
