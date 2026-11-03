/** Làm tròn trần trục tung lên số "đẹp" (1, 2, 5 nhân luỹ thừa 10). */
export function niceMax(value: number): number {
  if (value <= 0) return 1;
  const pow = 10 ** Math.floor(Math.log10(value));
  const n = value / pow;
  const nice = n <= 1 ? 1 : n <= 2 ? 2 : n <= 5 ? 5 : 10;
  return nice * pow;
}

/** Trần trục tung cho số đếm: bội của 4 để bốn vạch chia đều ra số nguyên (tối thiểu 4). */
export function integerAxisMax(value: number): number {
  return Math.max(4, Math.ceil(niceMax(value) / 4) * 4);
}
