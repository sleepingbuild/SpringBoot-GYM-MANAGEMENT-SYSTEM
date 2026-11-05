"use client";

import { useEffect, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { searchMembers } from "./api";
import { membershipErrorMessage } from "./messages";
import type { MemberSummary } from "./types";

/** Thông báo lỗi từ backend có thể không có dấu chấm cuối; thêm vào để câu nối tiếp không bị dính liền. */
function endWithPeriod(message: string): string {
  const text = message.trim();
  return /[.!?…]$/.test(text) ? text : `${text}.`;
}

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/**
 * Chọn hội viên để bán gói hộ. Tìm theo tên, số điện thoại hoặc email qua GET /api/v1/users (do Agent 1 làm).
 * Khi API đó chưa có hoặc lỗi, chuyển sang nhập thẳng mã hội viên để vẫn dùng được.
 */
export function MemberPicker({
  value,
  onChange,
  changeLabel = "Chọn người khác",
}: {
  value: MemberSummary | null;
  onChange: (m: MemberSummary | null) => void;
  /** Nhãn nút khi đã chọn xong (bộ lọc dùng "Bỏ lọc người mua"). */
  changeLabel?: string;
}) {
  const [q, setQ] = useState("");
  const [results, setResults] = useState<MemberSummary[]>([]);
  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState<unknown>(null);
  const [manualId, setManualId] = useState("");
  const [manualError, setManualError] = useState<string | null>(null);

  useEffect(() => {
    const term = q.trim();
    if (term.length < 2) {
      setResults([]);
      setSearching(false);
      return;
    }
    const controller = new AbortController();
    setSearching(true);
    const timer = window.setTimeout(() => {
      searchMembers(term, { signal: controller.signal })
        .then((page) => {
          if (controller.signal.aborted) return;
          setResults(page.content);
          setSearchError(null);
        })
        .catch((e: unknown) => {
          if (!controller.signal.aborted) setSearchError(e);
        })
        .finally(() => {
          if (!controller.signal.aborted) setSearching(false);
        });
    }, 300);
    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [q]);

  if (value) {
    return (
      <div className="panel flex flex-wrap items-center justify-between gap-2 p-3 text-sm">
        <span>
          <strong>{value.fullName}</strong>
          {value.phone && ` · ${value.phone}`}
          {value.email && ` · ${value.email}`}
        </span>
        <Button variant="secondary" onClick={() => onChange(null)}>
          {changeLabel}
        </Button>
      </div>
    );
  }

  function applyManualId() {
    const id = manualId.trim();
    if (!UUID_RE.test(id)) {
      setManualError("Mã hội viên phải đúng định dạng UUID.");
      return;
    }
    setManualError(null);
    onChange({ id, fullName: `Hội viên mã ${id.slice(0, 8)}`, phone: null, email: "" });
  }

  return (
    <div className="space-y-3">
      <Field label="Tìm hội viên" value={q} onChange={(e) => setQ(e.target.value)} placeholder="Tên, số điện thoại hoặc email (từ 2 ký tự)" hint="Chọn đúng người trong danh sách trước khi bán gói." autoComplete="off" />

      {searching && <p className="text-sm text-steel">Đang tìm…</p>}

      {searchError != null && (
        <Alert tone="warning" title="Chưa tìm được hội viên">
          {endWithPeriod(membershipErrorMessage(searchError, "Chức năng tìm hội viên chưa sẵn sàng."))} Bạn vẫn có thể nhập mã hội viên bên dưới.
        </Alert>
      )}

      {results.length > 0 && (
        <ul className="panel divide-y text-sm">
          {results.map((m) => (
            <li key={m.id}>
              <button type="button" className="w-full px-3 py-2 text-left hover:bg-plate-tint" onClick={() => onChange(m)}>
                <span className="font-medium">{m.fullName}</span>
                <span className="block text-xs text-steel">{[m.phone, m.email].filter(Boolean).join(" · ")}</span>
              </button>
            </li>
          ))}
        </ul>
      )}

      {!searching && searchError == null && q.trim().length >= 2 && results.length === 0 && <p className="text-sm text-steel">Không có hội viên nào khớp.</p>}

      {searchError != null && (
        <div className="flex flex-wrap items-end gap-2">
          <Field className="min-w-[16rem] flex-1" label="Mã hội viên (UUID)" value={manualId} error={manualError} onChange={(e) => setManualId(e.target.value)} />
          <Button variant="secondary" onClick={applyManualId}>
            Dùng mã này
          </Button>
        </div>
      )}
    </div>
  );
}
