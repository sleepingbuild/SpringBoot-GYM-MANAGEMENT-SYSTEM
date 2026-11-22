export type BadgeTone = "success" | "danger" | "warning" | "info" | "neutral";

/** Chữ màu trên nền cùng tông rất đậm, viền mờ. Viết nguyên văn để Tailwind không loại bỏ class. */
const TONE: Record<BadgeTone, string> = {
  success: "border-moss/40 bg-moss-tint text-moss",
  danger: "border-rubber/40 bg-rubber-tint text-rubber",
  warning: "border-amber/40 bg-amber-tint text-amber",
  info: "border-plate/40 bg-plate-tint text-plate",
  neutral: "border-line-strong bg-raised text-steel",
};

/**
 * Nhãn trạng thái và chip thông tin dùng chung (ví dụ "Đang hoạt động", "Còn 176 ngày").
 * Các module nên dùng thành phần này thay cho StatusBadge tự viết để màu thống nhất.
 */
export function Badge({ tone = "neutral", children, className = "" }: { tone?: BadgeTone; children: React.ReactNode; className?: string }) {
  return (
    <span className={`inline-flex items-center whitespace-nowrap rounded-full border px-2.5 py-0.5 text-xs font-semibold ${TONE[tone]} ${className}`}>
      {children}
    </span>
  );
}
