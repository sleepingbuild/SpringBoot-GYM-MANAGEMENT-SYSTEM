import { Badge } from "@/components/ui/Badge";
import type { BadgeTone } from "@/components/ui/Badge";
import { attendanceStatusLabel, attendanceStatusTone, formatDate, formatDateTime, methodLabel } from "./format";
import type { StatusTone } from "./format";
import type { StaffAttendanceItem } from "./types";

const BADGE_TONE: Record<StatusTone, BadgeTone> = {
  success: "success",
  warning: "warning",
  error: "danger",
  neutral: "neutral",
};

function StatusBadge({ status }: { status: string }) {
  return <Badge tone={BADGE_TONE[attendanceStatusTone(status)]}>{attendanceStatusLabel(status)}</Badge>;
}

/**
 * mode "mine": bảng của chính mình, cột đầu là ngày.
 * mode "report": báo cáo một ngày của quản trị viên, cột đầu là tên nhân sự.
 */
export function StaffAttendanceTable({ rows, mode }: { rows: StaffAttendanceItem[]; mode: "mine" | "report" }) {
  return (
    <div className="panel overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="border-b">
            <th className="px-4 py-3 font-semibold text-steel">{mode === "mine" ? "Ngày" : "Nhân sự"}</th>
            <th className="px-4 py-3 font-semibold text-steel">Giờ vào</th>
            <th className="px-4 py-3 font-semibold text-steel">Giờ ra</th>
            <th className="px-4 py-3 font-semibold text-steel">Trạng thái</th>
            <th className="px-4 py-3 font-semibold text-steel">Cách điểm danh</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id} className="border-b last:border-b-0 hover:bg-raised">
              <td className="px-4 py-3">
                {mode === "mine" ? formatDate(row.date) : row.staffName}
                {row.notes && <div className="text-steel">{row.notes}</div>}
              </td>
              <td className="px-4 py-3">{formatDateTime(row.checkInTime)}</td>
              <td className="px-4 py-3">
                {row.checkOutTime ? formatDateTime(row.checkOutTime) : <span className="text-steel">Chưa quét ra</span>}
              </td>
              <td className="px-4 py-3">
                <StatusBadge status={row.computedStatus} />
              </td>
              <td className="px-4 py-3">{methodLabel(row.method)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
