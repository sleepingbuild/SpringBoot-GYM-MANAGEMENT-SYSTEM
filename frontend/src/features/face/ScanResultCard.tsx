import { Alert } from "@/components/ui/Alert";
import { formatDateTime } from "@/features/attendance/format";
import { describeScan } from "./scanView";
import type { FaceAttendanceResult } from "./types";

/** Kết quả một lượt điểm danh. */
export function ScanResultCard({ result }: { result: FaceAttendanceResult }) {
  const view = describeScan(result);
  return (
    <Alert tone={view.tone} title={view.title}>
      {view.detail && <p>{view.detail}</p>}
      <p className="text-steel">{formatDateTime(result.scanTime)}</p>
    </Alert>
  );
}
