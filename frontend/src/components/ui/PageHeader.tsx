interface Props {
  title: string;
  description?: string;
  actions?: React.ReactNode;
  /** Biểu tượng (emoji) đứng trước tiêu đề, tùy chọn. Trang nào không truyền thì không hiện. */
  icon?: string;
}

export function PageHeader({ title, description, actions, icon }: Props) {
  return (
    <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
      <div>
        <h1 className="text-xl sm:text-2xl">
          {icon && (
            <span aria-hidden="true" className="mr-2">
              {icon}
            </span>
          )}
          {title}
        </h1>
        {description && <p className="mt-1 max-w-3xl text-sm text-steel">{description}</p>}
      </div>
      {actions && <div className="flex gap-2">{actions}</div>}
    </div>
  );
}
