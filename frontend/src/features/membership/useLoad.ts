"use client";

import { useCallback, useEffect, useState } from "react";

export interface LoadState<T> {
  status: "loading" | "ready" | "error";
  data?: T;
  error?: unknown;
  /** Tải lại với cùng tham số (giữ dữ liệu cũ trên màn hình trong lúc tải). */
  reload: () => void;
}

/**
 * Tải dữ liệu khi `deps` đổi và có hàm tải lại. Huỷ lời gọi đang chạy khi rời trang hoặc đổi tham số,
 * để kết quả chậm của lần gọi cũ không ghi đè kết quả mới.
 */
export function useLoad<T>(loader: (signal: AbortSignal) => Promise<T>, deps: readonly unknown[]): LoadState<T> {
  const [state, setState] = useState<{ status: "loading" | "ready" | "error"; data?: T; error?: unknown }>({ status: "loading" });
  const [tick, setTick] = useState(0);
  const reload = useCallback(() => setTick((t) => t + 1), []);

  useEffect(() => {
    const controller = new AbortController();
    setState((prev) => ({ status: "loading", data: prev.data }));
    loader(controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) setState({ status: "ready", data });
      })
      .catch((error: unknown) => {
        if (!controller.signal.aborted) setState({ status: "error", error });
      });
    return () => controller.abort();
    // `loader` được tạo lại mỗi lần render; chỉ chạy lại khi tham số (`deps`) hoặc yêu cầu tải lại đổi.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick]);

  return { ...state, reload };
}
