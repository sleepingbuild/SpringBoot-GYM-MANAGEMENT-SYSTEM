/** Hình dạng response của ReportController. Số tiền là BigDecimal nên JSON là number. */
export interface RevenueItem {
  /** yyyy-MM-dd khi groupBy=DAY, yyyy-MM khi groupBy=MONTH. */
  period: string;
  membership: number;
  pos: number;
  total: number;
}

export type RevenueGroupBy = "DAY" | "MONTH";

export interface RevenueReport {
  from: string;
  to: string;
  groupBy: RevenueGroupBy;
  total: number;
  membershipTotal: number;
  posTotal: number;
  items: RevenueItem[];
}

export interface RenewalReport {
  from: string;
  to: string;
  windowDays: number;
  expiredCount: number;
  renewedCount: number;
  /** Trong khoảng [0, 1]. Backend bỏ trường này khi không có gói nào hết hạn trong kỳ. */
  rate?: number | null;
}

export interface TrafficReport {
  from: string;
  to: string;
  total: number;
  byDay: Array<{ date: string; count: number }>;
  /** Luôn đủ 24 phần tử, hour từ 0 đến 23. */
  byHour: Array<{ hour: number; count: number }>;
}

export interface TopPackagesReport {
  from: string;
  to: string;
  items: Array<{ packageId: string; packageName: string; registrations: number; revenue: number }>;
}

export interface DateRange {
  from: string;
  to: string;
}

/** Khoảng tháng, dạng yyyy-MM, gồm cả hai đầu. */
export interface MonthRange {
  from: string;
  to: string;
}

export interface CommissionMonth {
  /** yyyy-MM. */
  month: string;
  totalSessions: number;
  totalAmount: number;
  pendingCount: number;
  pendingAmount: number;
  paidCount: number;
  paidAmount: number;
}

export interface CommissionsReport {
  fromMonth: string;
  toMonth: string;
  totalSessions: number;
  totalAmount: number;
  pendingCount: number;
  pendingAmount: number;
  paidCount: number;
  paidAmount: number;
  /** Luôn đủ mọi tháng trong khoảng. */
  months: CommissionMonth[];
}
