/** Màn rỗng nên chỉ ra việc tiếp theo, không chỉ báo "không có dữ liệu". */
export function EmptyState({
  title,
  children,
  action,
  icon,
}: {
  title: string;
  children?: React.ReactNode;
  action?: React.ReactNode;
  /** Biểu tượng (emoji) phía trên tiêu đề, tùy chọn. */
  icon?: string;
}) {
  return (
    <div className="panel px-6 py-16 text-center">
      {icon && (
        <p aria-hidden="true" className="mb-3 text-3xl">
          {icon}
        </p>
      )}
      <p className="text-lg font-semibold">{title}</p>
      {children && <p className="mx-auto mt-1 max-w-md text-sm text-steel">{children}</p>}
      {action && <div className="mt-5 flex justify-center">{action}</div>}
    </div>
  );
}
