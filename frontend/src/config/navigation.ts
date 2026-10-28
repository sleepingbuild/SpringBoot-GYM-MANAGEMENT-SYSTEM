import type { Role } from "@/lib/roles";

export interface NavItem {
  href: string;
  label: string;
  /** Vai trò được thấy mục này. Bỏ trống = mọi người đã đăng nhập. Backend vẫn là lớp phân quyền thật. */
  roles?: Role[];
  /** "ready" khi chủ module đã làm màn hình thật. "planned" hiện nhãn "Sắp có" để biết tiến độ. */
  status: "ready" | "planned";
  /** Agent phụ trách (để biết gửi phản hồi lỗi cho ai). */
  owner: string;
}

export interface NavGroup {
  title: string;
  items: NavItem[];
}

/**
 * Mỗi chủ module CHỈ sửa các dòng của mình (đổi status thành "ready" khi xong, thêm mục mới nếu cần).
 * Thêm nhóm hoặc đổi cấu trúc chung: nhờ Agent 1.
 */
export const NAVIGATION: NavGroup[] = [
  {
    title: "Chung",
    items: [
      { href: "/dashboard", label: "Tổng quan", status: "ready", owner: "Agent 1" },
      { href: "/profile", label: "Hồ sơ cá nhân", status: "ready", owner: "Agent 1" },
    ],
  },
  {
    title: "Gói tập",
    items: [
      { href: "/packages", label: "Gói tập", roles: ["MEMBER", "RECEPTIONIST", "SUPER_ADMIN"], status: "planned", owner: "Agent 2" },
      { href: "/my-membership", label: "Gói của tôi", roles: ["MEMBER"], status: "planned", owner: "Agent 2" },
      { href: "/payments", label: "Thanh toán", roles: ["RECEPTIONIST", "SUPER_ADMIN"], status: "planned", owner: "Agent 2" },
      { href: "/admin/packages", label: "Quản trị gói", roles: ["SUPER_ADMIN"], status: "planned", owner: "Agent 2" },
    ],
  },
  {
    title: "Lịch tập",
    items: [
      { href: "/bookings", label: "Đặt lịch PT", roles: ["MEMBER", "RECEPTIONIST", "SUPER_ADMIN"], status: "planned", owner: "Agent 3" },
      { href: "/schedule", label: "Lịch dạy", roles: ["TRAINER"], status: "planned", owner: "Agent 3" },
    ],
  },
  {
    title: "Điểm danh",
    items: [
      { href: "/check-in", label: "Tự điểm danh", roles: ["MEMBER", "TRAINER", "RECEPTIONIST", "SALES"], status: "planned", owner: "Agent 4" },
      { href: "/face-profile", label: "Đăng ký khuôn mặt", roles: ["MEMBER", "TRAINER", "RECEPTIONIST", "SALES"], status: "planned", owner: "Agent 4" },
      { href: "/kiosk", label: "Kiosk điểm danh", roles: ["RECEPTIONIST", "SUPER_ADMIN"], status: "planned", owner: "Agent 4" },
      { href: "/attendance", label: "Chấm công", roles: ["RECEPTIONIST", "SALES", "SUPER_ADMIN"], status: "planned", owner: "Agent 4" },
      { href: "/check-ins", label: "Lịch sử check-in", roles: ["MEMBER"], status: "planned", owner: "Agent 4" },
    ],
  },
  {
    title: "Bán hàng",
    items: [
      { href: "/pos", label: "Bán lẻ (POS)", roles: ["RECEPTIONIST", "SUPER_ADMIN"], status: "planned", owner: "Agent 5" },
      { href: "/commissions", label: "Lương theo buổi", roles: ["TRAINER", "SUPER_ADMIN"], status: "planned", owner: "Agent 5" },
    ],
  },
  {
    title: "Kinh doanh",
    items: [
      { href: "/leads", label: "Khách tiềm năng", roles: ["SALES", "SUPER_ADMIN"], status: "planned", owner: "Agent 6" },
      { href: "/reports", label: "Báo cáo", roles: ["SUPER_ADMIN"], status: "planned", owner: "Agent 6" },
    ],
  },
];
