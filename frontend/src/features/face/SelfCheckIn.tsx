"use client";

import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { scanSelf } from "./api";
import { EarlyLeaveForm } from "./EarlyLeaveForm";
import { FaceCameraView } from "./FaceCameraView";
import { faceErrorMessage } from "./messages";
import { ScanResultCard } from "./ScanResultCard";
import type { FaceAttendanceResult } from "./types";
import { useFaceCamera } from "./useFaceCamera";

type Notice = { tone: "warning" | "error"; text: string };

/** Tự điểm danh: so khớp với đúng tài khoản đang đăng nhập (1:1). */
export function SelfCheckIn() {
  const camera = useFaceCamera(0.2);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<Notice | null>(null);
  const [result, setResult] = useState<FaceAttendanceResult | null>(null);
  const [reasonSent, setReasonSent] = useState(false);

  async function handleScan() {
    setBusy(true);
    setNotice(null);
    setResult(null);
    setReasonSent(false);
    try {
      const capture = await camera.captureDescriptor();
      if (capture.kind === "no-face") {
        setNotice({ tone: "warning", text: "Không thấy khuôn mặt. Nhìn thẳng vào camera và thử lại." });
        return;
      }
      if (capture.kind === "too-small") {
        setNotice({ tone: "warning", text: "Khuôn mặt quá nhỏ trong khung hình. Lại gần camera hơn rồi thử lại." });
        return;
      }
      setResult(await scanSelf(capture.descriptor));
    } catch (e) {
      setNotice({ tone: "error", text: faceErrorMessage(e) });
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-4">
      <FaceCameraView camera={camera} />

      {notice && <Alert tone={notice.tone}>{notice.text}</Alert>}
      {result && <ScanResultCard result={result} />}
      {result?.reasonRequired && result.bookingId && !reasonSent && (
        <EarlyLeaveForm bookingId={result.bookingId} onDone={() => setReasonSent(true)} />
      )}
      {reasonSent && <Alert tone="success">Đã gửi lý do rời buổi sớm.</Alert>}

      <div className="flex justify-center">
        <Button onClick={handleScan} loading={busy} disabled={camera.status !== "ready"}>
          Quét khuôn mặt
        </Button>
      </div>
    </div>
  );
}
