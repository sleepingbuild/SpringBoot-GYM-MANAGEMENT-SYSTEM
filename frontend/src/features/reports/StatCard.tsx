export function StatCard({ label, value, note }: { label: string; value: string; note?: string }) {
  return (
    <div className="panel p-4">
      <p className="text-xs text-steel">{label}</p>
      <p className="mt-1 text-xl font-semibold">{value}</p>
      {note && <p className="mt-1 text-xs text-steel">{note}</p>}
    </div>
  );
}
