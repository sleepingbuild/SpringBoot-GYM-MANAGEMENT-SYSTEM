import { integerAxisMax, niceMax } from "./chartMath";

export interface ChartSeries {
  name: string;
  /** Lớp Tailwind fill-*, viết nguyên văn ở nơi gọi để Tailwind không loại bỏ. */
  fillClass: string;
}

export interface ChartItem {
  label: string;
  values: number[];
}

const W = 800;
const H = 240;
const PAD = { top: 12, right: 8, bottom: 28, left: 52 };

/**
 * Biểu đồ cột (chồng nếu có nhiều chuỗi) bằng SVG, không cần thư viện biểu đồ.
 * Mỗi cột có <title> để rê chuột thấy số liệu. Luôn kèm bảng số liệu ở nơi gọi cho người dùng trình đọc màn hình.
 */
export function BarChart({
  items,
  series,
  ariaLabel,
  formatValue,
  formatAxis,
  labelEvery,
  integerAxis = false,
}: {
  items: ChartItem[];
  series: ChartSeries[];
  ariaLabel: string;
  formatValue: (n: number) => string;
  formatAxis: (n: number) => string;
  /** Hiện nhãn trục hoành cứ mỗi N cột. Mặc định tự chọn để không chồng chữ. */
  labelEvery?: number;
  /** true cho số đếm: trục tung chia ra số nguyên. */
  integerAxis?: boolean;
}) {
  const n = Math.max(items.length, 1);
  const stackMax = Math.max(0, ...items.map((i) => i.values.reduce((a, b) => a + b, 0)));
  const top = integerAxis ? integerAxisMax(stackMax) : niceMax(stackMax);
  const innerW = W - PAD.left - PAD.right;
  const innerH = H - PAD.top - PAD.bottom;
  const step = innerW / n;
  const barW = Math.max(2, step * 0.72);
  const every = labelEvery ?? Math.max(1, Math.ceil(n / 12));
  const ticks = [0, 0.25, 0.5, 0.75, 1];

  return (
    <div>
      <svg viewBox={`0 0 ${W} ${H}`} role="img" aria-label={ariaLabel} className="h-auto w-full">
        {ticks.map((t) => {
          const y = PAD.top + innerH * (1 - t);
          return (
            <g key={t}>
              <line x1={PAD.left} x2={W - PAD.right} y1={y} y2={y} className="stroke-line" strokeWidth={1} />
              <text x={PAD.left - 6} y={y + 4} textAnchor="end" className="fill-steel" fontSize={11}>
                {formatAxis(top * t)}
              </text>
            </g>
          );
        })}
        {items.map((item, i) => {
          const x = PAD.left + step * i + (step - barW) / 2;
          let acc = 0;
          return (
            <g key={`${item.label}-${i}`}>
              <title>{`${item.label}: ${item.values.map((v, k) => `${series[k]?.name ?? ""} ${formatValue(v)}`).join(", ")}`}</title>
              {item.values.map((v, k) => {
                const h = (v / top) * innerH;
                const y = PAD.top + innerH - ((acc + v) / top) * innerH;
                acc += v;
                return v > 0 ? <rect key={k} x={x} y={y} width={barW} height={h} className={series[k]?.fillClass ?? "fill-plate"} /> : null;
              })}
              {i % every === 0 && (
                <text x={x + barW / 2} y={H - 8} textAnchor="middle" className="fill-steel" fontSize={11}>
                  {item.label}
                </text>
              )}
            </g>
          );
        })}
      </svg>
      {series.length > 1 && (
        <ul className="mt-2 flex flex-wrap gap-4 text-xs text-steel">
          {series.map((s) => (
            <li key={s.name} className="flex items-center gap-1.5">
              <svg width="10" height="10" aria-hidden="true">
                <rect width="10" height="10" className={s.fillClass} />
              </svg>
              {s.name}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
