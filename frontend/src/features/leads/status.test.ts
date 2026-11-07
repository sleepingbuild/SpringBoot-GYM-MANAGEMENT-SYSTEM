import { describe, expect, it } from "vitest";
import { allowedStatusChanges, canAssign, canConvert, canEdit } from "./status";
import { LEAD_STATUSES } from "./types";

describe("quy tắc trạng thái lead (khớp backend)", () => {
  it("NEW được đánh dấu đã liên hệ, đánh dấu mất và chuyển hội viên", () => {
    expect(allowedStatusChanges("NEW")).toEqual(["CONTACTED", "LOST"]);
    expect(canConvert("NEW")).toBe(true);
  });

  it("CONTACTED chỉ còn đánh dấu mất và chuyển hội viên", () => {
    expect(allowedStatusChanges("CONTACTED")).toEqual(["LOST"]);
    expect(canConvert("CONTACTED")).toBe(true);
  });

  it("LOST và CONVERTED là trạng thái cuối, không còn thao tác nào", () => {
    for (const s of ["LOST", "CONVERTED"] as const) {
      expect(allowedStatusChanges(s)).toEqual([]);
      expect(canConvert(s)).toBe(false);
    }
  });

  it("không bao giờ đề nghị đặt CONVERTED qua PATCH", () => {
    for (const s of LEAD_STATUSES) {
      expect(allowedStatusChanges(s)).not.toContain("CONVERTED");
    }
  });

  it("lead đã chuyển hội viên không sửa được", () => {
    expect(canEdit("CONVERTED")).toBe(false);
    expect(canEdit("LOST")).toBe(true);
    expect(canEdit("NEW")).toBe(true);
  });
});

describe("quy tắc giao lead", () => {
  it("chỉ giao lại được lead NEW và CONTACTED", () => {
    expect(canAssign("NEW")).toBe(true);
    expect(canAssign("CONTACTED")).toBe(true);
    expect(canAssign("LOST")).toBe(false);
    expect(canAssign("CONVERTED")).toBe(false);
  });
});
