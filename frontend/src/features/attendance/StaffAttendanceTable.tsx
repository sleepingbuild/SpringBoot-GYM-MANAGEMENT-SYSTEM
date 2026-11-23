import { attendanceStatusLabel, attendanceStatusTone, formatDate, formatDateTime, methodLabel } from "./format";
import type { StatusTone } from "./format";
import type { StaffAttendanceItem } from "./types";

const TONE_CLASS: Record<StatusTone, string> = {
  success: "border-moss bg-moss-tint",
  warning: "border-amber bg-amber-tint",
  error: "border-rubber bg-rubber-tint",
  neutral: "border-plate bg-plate-tint",
};

function StatusBadge({ status }: { status: string }) {
  return (
    <span className={`inline-block rounded-control border px-2 py-0.5 text-xs font-medium ${TONE_CLASS[attendanceStatusTone(status)]}`}>
      {attendanceStatusLabel(status)}
    </span>
  );
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
            <th className="px-4 py-3 font-semibold">{mode === "mine" ? "Ngày" : "Nhân sự"}</th>
            <th className="px-4 py-3 font-semibold">Giờ vào</th>
            <th className="px-4 py-3 font-semibold">Giờ ra</th>
            <th className="px-4 py-3 font-semibold">Trạng thái</th>
            <th className="px-4 py-3 font-semibold">Cách điểm danh</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id} className="border-b last:border-b-0">
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
