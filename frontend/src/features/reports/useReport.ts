"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { errorMessage } from "@/lib/api/errors";

export interface ReportState<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
}

/**
 * Tải một báo cáo mỗi khi `key` đổi (key là chuỗi mô tả tham số đã áp dụng) hoặc khi gọi reload().
 * Huỷ lời gọi cũ khi tham số đổi để kết quả cũ không ghi đè kết quả mới.
 */
export function useReport<T>(key: string, load: (signal: AbortSignal) => Promise<T>) {
  const loadRef = useRef(load);
  loadRef.current = load;
  const [tick, setTick] = useState(0);
  const [state, setState] = useState<ReportState<T>>({ data: null, loading: true, error: null });

  useEffect(() => {
    const ctrl = new AbortController();
    setState((s) => ({ ...s, loading: true, error: null }));
    loadRef.current(ctrl.signal)
      .then((data) => setState({ data, loading: false, error: null }))
      .catch((e) => {
        if (e instanceof DOMException && e.name === "AbortError") return;
        setState({ data: null, loading: false, error: errorMessage(e) });
      });
    return () => ctrl.abort();
  }, [key, tick]);

  const reload = useCallback(() => setTick((t) => t + 1), []);
  return { ...state, reload };
}
