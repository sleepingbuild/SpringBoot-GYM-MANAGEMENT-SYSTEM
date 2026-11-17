/**
 * Phan can thiet cua gói hoi vien de hien thi so buoi con lai o man hinh dat lich.
 * Tuong thich voi MemberPackage cua Agent 2 (khong import de file nay test duoc thuan).
 * heldSessions/bookableSessions la hai truong Agent 2 them o GET /member-packages/me; chua merge thi vang mat.
 */
export interface SessionInfo {
  /** null = goi khong tinh theo buoi (khong gioi han). */
  remainingSessions: number | null;
  heldSessions?: number | null;
  bookableSessions?: number | null;
}

export interface SessionSummary {
  text: string;
  /** true: khong dat them duoc (khong co goi, hoac het buoi). Backend van chan lai bang MEMBERSHIP_NO_SESSION. */
  blocked: boolean;
}

/** info = null nghia la chua co goi ACTIVE. */
export function sessionSummary(info: SessionInfo | null): SessionSummary {
  if (info === null) {
    return { text: "Bạn chưa có gói tập đang hoạt động nên chưa đặt lịch được. Hãy mua gói để đặt lịch.", blocked: true };
  }
  const remaining = info.remainingSessions;
  if (remaining === null) {
    return { text: "Gói của bạn không giới hạn số buổi.", blocked: false };
  }
  const held = info.heldSessions;
  const bookable = info.bookableSessions;
  if (typeof held === "number" && typeof bookable === "number") {
    if (bookable <= 0) {
      return {
        text: `Gói còn ${remaining} buổi nhưng ${held} buổi đang được giữ chỗ nên chưa đặt thêm được. Đợi các buổi đó hoàn thành hoặc huỷ bớt để đặt thêm.`,
        blocked: true,
      };
    }
    return { text: `Gói còn ${remaining} buổi, đang giữ chỗ ${held}, đặt thêm được ${bookable} buổi.`, blocked: false };
  }
  // Backend chua tra heldSessions/bookableSessions: chi biet so buoi con lai.
  if (remaining <= 0) {
    return { text: "Gói của bạn đã hết buổi nên chưa đặt lịch được. Hãy gia hạn hoặc mua gói mới.", blocked: true };
  }
  return { text: `Gói của bạn còn ${remaining} buổi.`, blocked: false };
}
