"use client";

import { useEffect, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { formatDateTime } from "@/features/attendance/format";
import { getMyFaceStatus, registerMyFace } from "./api";
import { FaceCameraView } from "./FaceCameraView";
import { faceErrorMessage } from "./messages";
import type { FaceProfileStatus } from "./types";
import { useFaceCamera } from "./useFaceCamera";

type ProfileState = { kind: "loading" } | { kind: "known"; status: FaceProfileStatus } | { kind: "unknown" };
type Notice = { tone: "success" | "warning" | "error"; text: string };

/** Đăng ký (hoặc đăng ký lại) khuôn mặt của chính mình. Đăng ký lại ghi đè hồ sơ cũ. */
export function FaceProfileManager() {
  const camera = useFaceCamera(0.25);
  const [profile, setProfile] = useState<ProfileState>({ kind: "loading" });
  const [reloadKey, setReloadKey] = useState(0);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<Notice | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    getMyFaceStatus(controller.signal)
      .then((status) => setProfile({ kind: "known", status }))
      .catch((e: unknown) => {
        if (e instanceof DOMException && e.name === "AbortError") return;
        // Không biết trạng thái cũng không chặn việc đăng ký.
        setProfile({ kind: "unknown" });
      });
    return () => controller.abort();
  }, [reloadKey]);

  async function handleRegister() {
    setBusy(true);
    setNotice(null);
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
      await registerMyFace(capture.descriptor);
      setNotice({ tone: "success", text: "Đăng ký khuôn mặt thành công." });
      setReloadKey((k) => k + 1);
    } catch (e) {
      setNotice({ tone: "error", text: faceErrorMessage(e) });
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-4">
      {profile.kind === "known" && profile.status.registered && (
        <Alert tone="info" title="Bạn đã đăng ký khuôn mặt">
          Cập nhật lần cuối: {formatDateTime(profile.status.updatedAt)}. Đăng ký lại sẽ thay thế hồ sơ hiện tại.
        </Alert>
      )}
      {profile.kind === "known" && !profile.status.registered && (
        <Alert tone="warning" title="Bạn chưa đăng ký khuôn mặt">
          Cần đăng ký trước khi dùng điểm danh bằng khuôn mặt.
        </Alert>
      )}

      <FaceCameraView camera={camera} />

      <p className="mx-auto max-w-xl text-sm text-steel">
        Nhìn thẳng vào camera ở nơi đủ sáng, không đeo khẩu trang hoặc kính tối màu. Hệ thống chỉ lưu đặc trưng khuôn mặt, không lưu ảnh.
      </p>

      {notice && <Alert tone={notice.tone}>{notice.text}</Alert>}

      <div className="flex justify-center">
        <Button onClick={handleRegister} loading={busy} disabled={camera.status !== "ready"}>
          {profile.kind === "known" && profile.status.registered ? "Chụp và đăng ký lại" : "Chụp và đăng ký"}
        </Button>
      </div>
    </div>
  );
}
