"use client";

import { Alert } from "@/components/ui/Alert";
import { Button, Spinner } from "@/components/ui/Button";
import type { FaceCamera } from "./useFaceCamera";

const STATUS_TEXT: Record<string, string> = {
  "loading-models": "Đang tải mô hình nhận diện (lần đầu có thể mất vài giây)...",
  "starting-camera": "Đang bật camera...",
};

/** Khung xem trước camera (lật gương cho dễ căn mặt) kèm trạng thái tải và lỗi. */
export function FaceCameraView({ camera }: { camera: FaceCamera }) {
  const overlay = STATUS_TEXT[camera.status];
  return (
    <div className="space-y-3">
      <div className="relative mx-auto aspect-[4/3] w-full max-w-xl overflow-hidden rounded-control bg-black">
        <video ref={camera.videoRef} className="h-full w-full -scale-x-100 object-cover" playsInline muted />
        {overlay && (
          <div className="absolute inset-0 flex items-center justify-center gap-2 bg-black/60 px-4 text-center text-sm text-white" role="status">
            <Spinner />
            {overlay}
          </div>
        )}
      </div>
      {camera.status === "error" && (
        <div className="space-y-3">
          <Alert tone="error" title="Không bật được camera">
            {camera.errorMessage}
          </Alert>
          <Button variant="secondary" onClick={camera.restart}>
            Thử lại
          </Button>
        </div>
      )}
    </div>
  );
}
