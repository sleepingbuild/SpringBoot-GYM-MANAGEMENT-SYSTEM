"use client";

import { useEffect, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { errorMessage } from "@/lib/api/errors";
import { searchMembers } from "./api";
import type { MemberOption } from "./types";

const MIN_QUERY_LENGTH = 2;
const DEBOUNCE_MS = 300;

const memberLabel = (m: MemberOption) => m.fullName ?? m.email ?? m.phone ?? "Hội viên";
const memberDetail = (m: MemberOption) => [m.phone, m.email].filter(Boolean).join(" - ");

/** Le tan, admin: tim hoi vien theo ten, so dien thoai hoac email roi chon mot nguoi. */
export function MemberPicker({
  value,
  onChange,
}: {
  value: MemberOption | null;
  onChange: (member: MemberOption | null) => void;
}) {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<MemberOption[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const q = query.trim();
    if (value || q.length < MIN_QUERY_LENGTH) {
      setResults(null);
      setError(null);
      return;
    }
    let cancelled = false;
    setResults(null);
    setError(null);
    const timer = setTimeout(() => {
      searchMembers(q)
        .then((page) => !cancelled && setResults(page.content))
        .catch((e) => !cancelled && setError(errorMessage(e)));
    }, DEBOUNCE_MS);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [query, value]);

  if (value) {
    return (
      <div className="panel mb-6 flex max-w-md items-start justify-between gap-3 p-5">
        <div>
          <p className="text-sm text-steel">Đặt hộ cho hội viên</p>
          <p className="text-sm font-semibold">{memberLabel(value)}</p>
          {memberDetail(value) && <p className="text-sm text-steel">{memberDetail(value)}</p>}
        </div>
        <button
          type="button"
          className="text-sm underline"
          onClick={() => {
            onChange(null);
            setQuery("");
          }}
        >
          Đổi hội viên
        </button>
      </div>
    );
  }

  const searching = query.trim().length >= MIN_QUERY_LENGTH && results === null && error === null;

  return (
    <div className="panel mb-6 max-w-md p-5">
      <Field
        label="Tìm hội viên"
        type="search"
        value={query}
        placeholder="Tên, số điện thoại hoặc email"
        onChange={(e) => setQuery(e.target.value)}
      />
      <div className="mt-3" aria-live="polite">
        {query.trim().length < MIN_QUERY_LENGTH ? (
          <p className="text-sm text-steel">Nhập ít nhất {MIN_QUERY_LENGTH} ký tự để tìm.</p>
        ) : error ? (
          <Alert tone="error">{error}</Alert>
        ) : searching ? (
          <div className="flex justify-center py-3 text-steel" role="status" aria-label="Đang tìm hội viên">
            <Spinner />
          </div>
        ) : results && results.length === 0 ? (
          <p className="text-sm text-steel">Không tìm thấy hội viên nào khớp.</p>
        ) : (
          <ul className="space-y-1">
            {results?.map((m) => (
              <li key={m.id}>
                <button
                  type="button"
                  className="w-full rounded border px-3 py-2 text-left text-sm"
                  onClick={() => onChange(m)}
                >
                  <span className="block font-medium">{memberLabel(m)}</span>
                  {memberDetail(m) && <span className="block text-xs text-steel">{memberDetail(m)}</span>}
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
