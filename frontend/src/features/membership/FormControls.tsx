import { useId } from "react";

interface BaseProps {
  label: string;
  error?: string | null;
  hint?: string;
  className?: string;
  required?: boolean;
}

function Wrapper({ label, error, hint, className = "", required, id, children }: BaseProps & { id: string; children: React.ReactNode }) {
  return (
    <div className={className}>
      <label htmlFor={id} className="mb-1 block text-sm font-medium text-ink">
        {label}
        {required && <span className="text-rubber"> *</span>}
      </label>
      {children}
      {hint && !error && (
        <p id={`${id}-hint`} className="mt-1 text-xs text-steel">
          {hint}
        </p>
      )}
      {error && (
        <p id={`${id}-error`} className="mt-1 text-xs text-rubber">
          {error}
        </p>
      )}
    </div>
  );
}

function describedBy(id: string, error?: string | null, hint?: string): string | undefined {
  return [error ? `${id}-error` : null, hint ? `${id}-hint` : null].filter(Boolean).join(" ") || undefined;
}

/** Ô chọn có nhãn, cùng kiểu với Field dùng chung (Field chỉ dành cho ô nhập). */
export function SelectField({
  label,
  error,
  hint,
  className,
  required,
  options,
  ...rest
}: BaseProps & { options: { value: string; label: string }[] } & Omit<React.SelectHTMLAttributes<HTMLSelectElement>, "className">) {
  const id = useId();
  return (
    <Wrapper label={label} error={error} hint={hint} className={className} required={required} id={id}>
      <select id={id} required={required} aria-invalid={error ? true : undefined} aria-describedby={describedBy(id, error, hint)} className={`input ${error ? "input-error" : ""}`} {...rest}>
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
    </Wrapper>
  );
}

export function TextAreaField({
  label,
  error,
  hint,
  className,
  required,
  ...rest
}: BaseProps & Omit<React.TextareaHTMLAttributes<HTMLTextAreaElement>, "className">) {
  const id = useId();
  return (
    <Wrapper label={label} error={error} hint={hint} className={className} required={required} id={id}>
      <textarea id={id} required={required} aria-invalid={error ? true : undefined} aria-describedby={describedBy(id, error, hint)} className={`input ${error ? "input-error" : ""}`} rows={3} {...rest} />
    </Wrapper>
  );
}
