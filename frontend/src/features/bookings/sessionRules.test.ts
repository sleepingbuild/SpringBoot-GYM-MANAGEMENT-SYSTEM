import { describe, expect, it } from "vitest";
import { sessionSummary } from "./sessionRules";

describe("sessionSummary", () => {
  it("chua co goi: chan va bao mua goi", () => {
    const s = sessionSummary(null);
    expect(s.blocked).toBe(true);
    expect(s.text).toContain("chưa có gói");
  });

  it("goi khong tinh theo buoi: khong chan", () => {
    const s = sessionSummary({ remainingSessions: null });
    expect(s.blocked).toBe(false);
    expect(s.text).toContain("không giới hạn");
  });

  it("co du con so tu backend: hien con/dang giu/dat them duoc", () => {
    const s = sessionSummary({ remainingSessions: 10, heldSessions: 3, bookableSessions: 7 });
    expect(s.blocked).toBe(false);
    expect(s.text).toContain("còn 10 buổi");
    expect(s.text).toContain("giữ chỗ 3");
    expect(s.text).toContain("7 buổi");
  });

  it("dat them duoc = 0: chan va giai thich", () => {
    const s = sessionSummary({ remainingSessions: 2, heldSessions: 2, bookableSessions: 0 });
    expect(s.blocked).toBe(true);
    expect(s.text).toContain("đang được giữ chỗ");
  });

  it("backend chua tra heldSessions: chi hien so buoi con lai", () => {
    const s = sessionSummary({ remainingSessions: 5 });
    expect(s.blocked).toBe(false);
    expect(s.text).toBe("Gói của bạn còn 5 buổi.");
  });

  it("backend chua tra heldSessions va het buoi: chan", () => {
    expect(sessionSummary({ remainingSessions: 0 }).blocked).toBe(true);
  });
});
