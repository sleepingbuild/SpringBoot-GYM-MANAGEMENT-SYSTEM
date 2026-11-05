import { StatusBadge } from "./StatusBadge";
import { formatDate, formatDateTime, formatVnd, shortId } from "./format";
import { MEMBERSHIP_STATUS_LABELS, PAYMENT_STATUS_LABELS, RELATED_TYPE_LABELS, labelOf, membershipTone, paymentTone } from "./labels";
import type { MemberPackage, PaymentTransaction } from "./types";

const TH = "whitespace-nowrap border-b px-3 py-2 font-medium text-steel";
const TD = "border-b px-3 py-2 align-top";

export function MembershipTable({ rows }: { rows: MemberPackage[] }) {
  return (
    <div className="panel overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead>
          <tr>
            <th className={TH}>Gói</th>
            <th className={TH}>Trạng thái</th>
            <th className={TH}>Bắt đầu</th>
            <th className={TH}>Hết hạn</th>
            <th className={TH}>Số buổi còn lại</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((m) => (
            <tr key={m.id}>
              <td className={TD}>{m.packageName}</td>
              <td className={TD}>
                <StatusBadge tone={membershipTone(m.status)}>{labelOf(MEMBERSHIP_STATUS_LABELS, m.status)}</StatusBadge>
              </td>
              <td className={TD}>{formatDate(m.startDate)}</td>
              <td className={TD}>{m.endDate ? formatDate(m.endDate) : "Không thời hạn"}</td>
              <td className={TD}>{m.remainingSessions == null ? "Không giới hạn" : m.remainingSessions}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

/** Bảng giao dịch. `showBuyer` hiện cột người mua (cho Lễ tân, Admin); `renderAction` thêm cột thao tác. */
export function PaymentTable({
  rows,
  showBuyer = false,
  renderAction,
}: {
  rows: PaymentTransaction[];
  showBuyer?: boolean;
  renderAction?: (p: PaymentTransaction) => React.ReactNode;
}) {
  return (
    <div className="panel overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead>
          <tr>
            <th className={TH}>Mã giao dịch</th>
            {showBuyer && <th className={TH}>Người mua</th>}
            <th className={TH}>Loại</th>
            <th className={TH}>Số tiền</th>
            <th className={TH}>Trạng thái</th>
            <th className={TH}>Thời gian</th>
            {renderAction && <th className={TH}>Thao tác</th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((p) => (
            <tr key={p.id}>
              <td className={`${TD} font-mono text-xs`}>{p.transactionCode}</td>
              {showBuyer && <td className={TD}>{buyerName(p)}</td>}
              <td className={TD}>{labelOf(RELATED_TYPE_LABELS, p.relatedType)}</td>
              <td className={`${TD} whitespace-nowrap`}>{formatVnd(p.amount)}</td>
              <td className={TD}>
                <StatusBadge tone={paymentTone(p.status)}>{labelOf(PAYMENT_STATUS_LABELS, p.status)}</StatusBadge>
              </td>
              <td className={`${TD} whitespace-nowrap`}>{formatDateTime(p.createdAt)}</td>
              {renderAction && <td className={TD}>{renderAction(p)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

/** Tên người mua; chưa có tên (backend chưa có H3) thì hiện mã rút gọn, không có người mua là khách vãng lai. */
export function buyerName(p: Pick<PaymentTransaction, "userId" | "userFullName">): string {
  if (p.userFullName) return p.userFullName;
  if (p.userId) return `Mã ${shortId(p.userId)}`;
  return "Khách vãng lai";
}
