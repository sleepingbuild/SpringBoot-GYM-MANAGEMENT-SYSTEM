import { describe, expect, it } from "vitest";
import { canRenew, daysChip, diffDays, matchesTab, todayInVietnam } from "./membershipView";

describe("diffDays", () => {
  it("đếm đúng qua tháng và năm", () => {
    expect(diffDays("2026-08-12", "2027-02-04")).toBe(176);
    expect(diffDays("2026-07-12", "2026-08-11")).toBe(30);
  });

  it("cùng ngày là 0, ngược chiều là số âm", () => {
    expect(diffDays("2026-11-21", "2026-11-21")).toBe(0);
    expect(diffDays("2026-11-21", "2026-11-20")).toBe(-1);
  });
});

describe("todayInVietnam", () => {
  it("tính theo giờ Việt Nam, không theo UTC", () => {
    expect(todayInVietnam(new Date("2026-11-20T18:00:00Z"))).toBe("2026-11-21");
    expect(todayInVietnam(new Date("2026-11-20T16:59:00Z"))).toBe("2026-11-20");
  });
});

describe("matchesTab", () => {
  it("lọc theo từng tab", () => {
    expect(matchesTab("PENDING", "all")).toBe(true);
    expect(matchesTab("ACTIVE", "active")).toBe(true);
    expect(matchesTab("SCHEDULED", "active")).toBe(false);
    expect(matchesTab("EXPIRED", "history")).toBe(true);
    expect(matchesTab("CANCELLED", "history")).toBe(true);
    expect(matchesTab("ACTIVE", "history")).toBe(false);
  });
});

describe("daysChip", () => {
  it("gói đang hoạt động: còn bao nhiêu ngày", () => {
    const chip = daysChip({ status: "ACTIVE", startDate: "2026-08-08", endDate: "2027-02-04" }, "2026-08-12");
    expect(chip).toEqual({ label: "Trạng thái", text: "Còn 176 ngày", tone: "good" });
  });

  it("đúng ngày cuối thì báo hết hạn hôm nay", () => {
    const chip = daysChip({ status: "ACTIVE", startDate: "2026-10-01", endDate: "2026-11-21" }, "2026-11-21");
    expect(chip?.text).toBe("Hết hạn hôm nay");
    expect(chip?.tone).toBe("warn");
  });

  it("gói đang hoạt động không có ngày hết hạn", () => {
    const chip = daysChip({ status: "ACTIVE", startDate: "2026-10-01", endDate: null }, "2026-11-21");
    expect(chip?.text).toBe("Không thời hạn");
  });

  it("gói đã hết hạn: số ngày đã sử dụng", () => {
    const chip = daysChip({ status: "EXPIRED", startDate: "2026-07-12", endDate: "2026-08-11" }, "2026-11-21");
    expect(chip).toEqual({ label: "Đã sử dụng", text: "30 ngày", tone: "bad" });
  });

  it("gói chờ thanh toán, đã lên lịch, đã hủy: không có chip", () => {
    for (const status of ["PENDING", "SCHEDULED", "CANCELLED"]) {
      expect(daysChip({ status, startDate: "2026-11-21", endDate: null }, "2026-11-21")).toBeNull();
    }
  });
});

describe("canRenew", () => {
  it("cho gia hạn gói hết hạn khi không còn gói nào đang chạy hay chờ", () => {
    expect(canRenew({ status: "EXPIRED" }, [{ status: "EXPIRED" }, { status: "CANCELLED" }])).toBe(true);
  });

  it("không cho gia hạn khi đã có gói ACTIVE, PENDING hoặc SCHEDULED", () => {
    for (const other of ["ACTIVE", "PENDING", "SCHEDULED"]) {
      expect(canRenew({ status: "EXPIRED" }, [{ status: "EXPIRED" }, { status: other }])).toBe(false);
    }
  });

  it("chỉ gói đã hết hạn mới gia hạn được", () => {
    expect(canRenew({ status: "ACTIVE" }, [])).toBe(false);
    expect(canRenew({ status: "CANCELLED" }, [])).toBe(false);
  });
});
