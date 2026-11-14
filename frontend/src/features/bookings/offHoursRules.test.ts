import { describe, expect, it } from "vitest";
import { bookedMessage, displayEnd, hhmm, slotLabel, toTrainerChoices } from "./offHoursRules";

describe("displayEnd", () => {
  it("23:59 hien thi la 24:00", () => {
    expect(displayEnd("23:59:00")).toBe("24:00");
    expect(displayEnd("23:59")).toBe("24:00");
  });

  it("gio khac giu nguyen HH:mm", () => {
    expect(displayEnd("22:00:00")).toBe("22:00");
    expect(displayEnd("09:30:00")).toBe("09:30");
  });
});

describe("slotLabel", () => {
  it("khung cuoi ngay ket thuc o 24:00", () => {
    expect(slotLabel("23:00:00", "23:59:00")).toBe("23:00 - 24:00");
  });

  it("khung thuong", () => {
    expect(slotLabel("21:00:00", "22:00:00")).toBe("21:00 - 22:00");
    expect(hhmm("08:05:00")).toBe("08:05");
  });
});

describe("bookedMessage", () => {
  it("CONFIRMED: khong noi cho PT xac nhan", () => {
    expect(bookedMessage("CONFIRMED", false)).not.toContain("huấn luyện viên đồng ý");
    expect(bookedMessage("CONFIRMED", true)).not.toContain("huấn luyện viên đồng ý");
  });

  it("PENDING: noi ro cho PT xac nhan", () => {
    expect(bookedMessage("PENDING", false)).toContain("huấn luyện viên đồng ý");
    expect(bookedMessage("PENDING", true)).toContain("huấn luyện viên đồng ý");
  });

  it("le tan dat ho co chu 'hộ hội viên'", () => {
    expect(bookedMessage("CONFIRMED", true)).toContain("hộ hội viên");
    expect(bookedMessage("PENDING", true)).toContain("hộ hội viên");
  });
});

describe("toTrainerChoices", () => {
  it("doi sang trainerId va xep theo ten", () => {
    const out = toTrainerChoices([
      { id: "b", fullName: "Một", avatarUrl: null },
      { id: "a", fullName: "Hai", avatarUrl: null },
    ]);
    expect(out.map((t) => t.trainerId)).toEqual(["a", "b"]);
    expect(out[0].trainerName).toBe("Hai");
  });

  it("PT khong co ten van co mat", () => {
    const out = toTrainerChoices([{ id: "x", fullName: null, avatarUrl: null }]);
    expect(out).toEqual([{ trainerId: "x", trainerName: null }]);
  });
});
