import { forwardRef, useId } from "react";

interface Props extends React.InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string | null;
  hint?: string;
}

/** Ô nhập có nhãn, gợi ý và thông báo lỗi gắn đúng ô (đọc được bằng trình đọc màn hình). */
export const Field = forwardRef<HTMLInputElement, Props>(function Field(
  { label, error, hint, id, className = "", required, ...rest },
  ref,
) {
  const autoId = useId();
  const inputId = id ?? autoId;
  const describedBy = [error ? `${inputId}-error` : null, hint ? `${inputId}-hint` : null].filter(Boolean).join(" ") || undefined;

  return (
    <div className={className}>
      <label htmlFor={inputId} className="mb-1 block text-sm font-medium text-ink">
        {label}
        {required && <span className="text-rubber"> *</span>}
      </label>
      <input
        ref={ref}
        id={inputId}
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={`input ${error ? "input-error" : ""}`}
        {...rest}
      />
      {hint && !error && (
        <p id={`${inputId}-hint`} className="mt-1 text-xs text-steel">
          {hint}
        </p>
      )}
      {error && (
        <p id={`${inputId}-error`} className="mt-1 text-xs text-rubber">
          {error}
        </p>
      )}
    </div>
  );
});
