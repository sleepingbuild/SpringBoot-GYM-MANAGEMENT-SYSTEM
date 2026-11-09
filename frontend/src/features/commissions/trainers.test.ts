import { describe, expect, it } from "vitest";
import { sortTrainers, trainerLabel } from "./trainers";
import type { Trainer } from "./types";

const t = (id: string, fullName: string): Trainer => ({ id, fullName, avatarUrl: null });

describe("trainers", () => {
  it("trainerLabel trả tên, không có thì rút gọn mã", () => {
    const list = [t("11111111-aaaa", "Lê Minh")];
    expect(trainerLabel(list, "11111111-aaaa")).toBe("Lê Minh");
    expect(trainerLabel(list, "99999999-bbbb-cccc")).toBe("PT 99999999");
  });

  it("sortTrainers theo tên tiếng Việt và không đổi mảng gốc", () => {
    const list = [t("1", "Trần An"), t("2", "Ân Bảo"), t("3", "Bùi Cường")];
    const sorted = sortTrainers(list);
    expect(sorted.map((x) => x.fullName)).toEqual(["Ân Bảo", "Bùi Cường", "Trần An"]);
    expect(list[0].fullName).toBe("Trần An");
  });
});
