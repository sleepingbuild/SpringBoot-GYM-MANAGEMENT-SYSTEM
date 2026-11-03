export const LEAD_STATUSES = ["NEW", "CONTACTED", "CONVERTED", "LOST"] as const;
export type LeadStatus = (typeof LEAD_STATUSES)[number];

/** LeadResponse.java. Các trường rỗng bị backend bỏ khỏi JSON (NON_NULL) nên đều là tuỳ chọn. */
export interface Lead {
  id: string;
  fullName: string;
  phone: string;
  source?: string | null;
  status: LeadStatus;
  assignedSalesId?: string | null;
  convertedUserId?: string | null;
  notes?: string | null;
  createdAt: string;
  updatedAt?: string | null;
  /** Chỉ có khi vừa tạo lead: true nếu số đã có ở lead khác (lead vẫn được tạo). */
  duplicatePhone?: boolean;
  /** Chỉ có khi vừa chuyển đổi mà không truyền mật khẩu: hiển thị một lần duy nhất. */
  temporaryPassword?: string;
}

export interface LeadInput {
  fullName: string;
  phone: string;
  source: string;
  notes: string;
}

export interface ConvertInput {
  email: string;
  /** Để trống = hệ thống tự sinh mật khẩu ngẫu nhiên. */
  password: string;
}
