"use client";

import { useEffect, useState } from "react";
import { errorMessage } from "@/lib/api/errors";
import { listTrainers } from "./api";
import { sortTrainers } from "./trainers";
import type { Trainer } from "./types";

/** Tải danh sách PT một lần. `trainers` là null khi đang tải; lỗi thì `error` có nội dung và `trainers` là []. */
export function useTrainers(): { trainers: Trainer[] | null; error: string | null } {
  const [trainers, setTrainers] = useState<Trainer[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;
    listTrainers()
      .then((list) => {
        if (alive) setTrainers(sortTrainers(list));
      })
      .catch((e: unknown) => {
        if (alive) {
          setError(errorMessage(e));
          setTrainers([]);
        }
      });
    return () => {
      alive = false;
    };
  }, []);

  return { trainers, error };
}
