type Tone = "error" | "info" | "success" | "warning";

const TONE: Record<Tone, string> = {
  error: "border-rubber bg-rubber-tint",
  info: "border-plate bg-plate-tint",
  success: "border-moss bg-moss-tint",
  warning: "border-amber bg-amber-tint",
};

/** Tiêu đề lấy màu theo tông, nội dung giữ chữ chính để đọc rõ trên nền tint. */
const TITLE: Record<Tone, string> = {
  error: "text-rubber",
  info: "text-plate",
  success: "text-moss",
  warning: "text-amber",
};

export function Alert({ tone = "info", title, children }: { tone?: Tone; title?: string; children: React.ReactNode }) {
  return (
    <div role={tone === "error" ? "alert" : "status"} className={`rounded-control border-l-4 px-4 py-3 text-sm ${TONE[tone]}`}>
      {title && <p className={`mb-0.5 font-semibold ${TITLE[tone]}`}>{title}</p>}
      <div>{children}</div>
    </div>
  );
}
