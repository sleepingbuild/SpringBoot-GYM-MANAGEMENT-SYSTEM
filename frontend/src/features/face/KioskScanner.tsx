"use client";

import { useEffect, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { scanKiosk } from "./api";
import { FaceCameraView } from "./FaceCameraView";
import { faceErrorMessage } from "./messages";
import { describeScan } from "./scanView";
import type { ScanView } from "./scanView";
import { useFaceCamera } from "./useFaceCamera";

/** Chu kỳ kiểm tra khung hình. */
const POLL_MS = 700;
/** Sau mỗi lượt quét, tạm dừng để người dùng đọc kết quả. */
const PAUSE_MS = 3000;
/** Nếu người đó đứng yên không rời khung, sau bấy nhiêu thì cho quét lại. */
const REARM_MS = 8000;

type Phase = "watching" | "sending" | "paused";

/** Kiosk điểm danh tại quầy: tự quét liên tục, so khớp 1:N với mọi hồ sơ khuôn mặt. */
export function KioskScanner() {
  const camera = useFaceCamera(0.15);
  const { status, captureDescriptor } = camera;
  const [phase, setPhase] = useState<Phase>("watching");
  const [view, setView] = useState<ScanView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [hint, setHint] = useState<string | null>(null);

  useEffect(() => {
    if (status !== "ready") return;
    let cancelled = false;
    const controller = new AbortController();
    let timer: ReturnType<typeof setTimeout> | undefined;
    let armed = true;
    let pauseUntil = 0;
    let rearmAt = 0;

    const tick = async () => {
      if (cancelled) return;
      try {
        const capture = await captureDescriptor();
        if (cancelled) return;
        const now = Date.now();
        if (capture.kind === "no-face") {
          armed = true;
          setHint(null);
        } else if (capture.kind === "too-small") {
          setHint("Lại gần camera hơn một chút.");
        } else if ((armed || now >= rearmAt) && now >= pauseUntil) {
          armed = false;
          setHint(null);
          setPhase("sending");
          try {
            const result = await scanKiosk(capture.descriptor, controller.signal);
            setView(describeScan(result));
            setError(null);
          } catch (e) {
            if (e instanceof DOMException && e.name === "AbortError") return;
            setView(null);
            setError(faceErrorMessage(e));
          }
          pauseUntil = Date.now() + PAUSE_MS;
          rearmAt = Date.now() + REARM_MS;
          setPhase("paused");
        }
        if (cancelled) return;
        if (phase !== "watching" && Date.now() >= pauseUntil) setPhase("watching");
      } catch {
        // Lỗi tạm thời của một lần quét (ví dụ khung hình chưa sẵn sàng): bỏ qua, thử lại ở chu kỳ sau.
      }
      if (!cancelled) timer = setTimeout(tick, POLL_MS);
    };
    timer = setTimeout(tick, POLL_MS);

    return () => {
      cancelled = true;
      controller.abort();
      if (timer) clearTimeout(timer);
    };
    // phase chỉ dùng để hiển thị nên không đưa vào dependency, tránh khởi động lại vòng quét.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status, captureDescriptor]);

  const phaseText =
    phase === "sending" ? "Đang nhận diện..." : phase === "paused" ? "Đã quét xong" : "Đang chờ khuôn mặt, nhìn thẳng vào camera.";

  return (
    <div className="space-y-4">
      <FaceCameraView camera={camera} />
      {status === "ready" && (
        <p className="text-center text-sm text-steel" role="status">
          {hint ?? phaseText}
        </p>
      )}
      {view && (
        <Alert tone={view.tone} title={view.title}>
          {view.detail}
        </Alert>
      )}
      {error && (
        <Alert tone="error" title="Không ghi nhận được">
          {error}
        </Alert>
      )}
    </div>
  );
}
