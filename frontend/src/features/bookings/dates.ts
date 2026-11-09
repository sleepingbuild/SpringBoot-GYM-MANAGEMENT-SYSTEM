const pad = (n: number) => String(n).padStart(2, "0");

/** yyyy-MM-dd theo gio dia phuong cua trinh duyet (khong cong tru mui gio, khong dung toISOString). */
export const toIsoDate = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

export const addDays = (d: Date, n: number) => {
  const x = new Date(d);
  x.setDate(x.getDate() + n);
  return x;
};
