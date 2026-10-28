"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "../api/client";
import type { AuthResponse, SessionUser } from "../types";
import { session } from "./session";

type Status = "loading" | "authenticated" | "unauthenticated";

interface AuthValue {
  status: Status;
  user: SessionUser | null;
  login: (email: string, password: string) => Promise<void>;
  register: (input: { fullName: string; email: string; phone?: string; password: string }) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [status, setStatus] = useState<Status>("loading");
  const [user, setUser] = useState<SessionUser | null>(null);

  // Đồng bộ React với kho phiên (kể cả khi bộ gọi API làm mới token ngầm).
  useEffect(() => {
    const unsubscribe = session.onChange(() => {
      const u = session.getUser();
      setUser(u);
      setStatus(u ? "authenticated" : "unauthenticated");
    });
    const unsubscribeExpired = session.onExpired(() => {
      router.replace("/login?expired=1");
    });
    return () => {
      unsubscribe();
      unsubscribeExpired();
    };
  }, [router]);

  // Khôi phục phiên khi tải trang: access token chỉ nằm trong bộ nhớ nên dùng refresh token.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      if (session.getRefreshToken()) {
        const ok = await session.refresh();
        if (cancelled) return;
        if (!ok) session.clear();
      }
      if (cancelled) return;
      const u = session.getUser();
      setUser(u);
      setStatus(u ? "authenticated" : "unauthenticated");
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const auth = await apiFetch<AuthResponse>("/api/v1/auth/login", {
      method: "POST",
      body: { email, password },
      auth: false,
    });
    session.set(auth);
  }, []);

  const register = useCallback(async (input: { fullName: string; email: string; phone?: string; password: string }) => {
    const phone = input.phone?.trim();
    const auth = await apiFetch<AuthResponse>("/api/v1/auth/register", {
      method: "POST",
      body: { fullName: input.fullName, email: input.email, phone: phone ? phone : null, password: input.password },
      auth: false,
    });
    session.set(auth);
  }, []);

  const logout = useCallback(async () => {
    try {
      // Backend đưa access token vào danh sách chặn; lỗi ở bước này không được cản việc đăng xuất.
      if (session.getAccessToken()) {
        await apiFetch("/api/v1/auth/logout", { method: "POST", skipAuthRedirect: true });
      }
    } catch {
      /* bỏ qua */
    } finally {
      session.clear();
      router.replace("/login");
    }
  }, [router]);

  const value = useMemo<AuthValue>(
    () => ({ status, user, login, register, logout }),
    [status, user, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth phải được dùng bên trong <AuthProvider>");
  return ctx;
}
