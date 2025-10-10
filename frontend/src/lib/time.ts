// Utilities for Asia/Taipei date handling

// Convert YYYY-MM-DD (local date in TPE) to an ISO string representing
// midnight at Asia/Taipei, expressed in UTC (Instant string)
export function tpeMidnightIso(dateYmd: string): string {
  const [y, m, d] = dateYmd.split('-').map((s) => parseInt(s, 10));
  // Asia/Taipei = UTC+8 => TPE 00:00 equals UTC previous day 16:00
  const iso = new Date(Date.UTC(y, (m - 1), d, -8, 0, 0)).toISOString();
  return iso;
}

// Given a JS Date (calendar day), return YYYY-MM-DD string for display
export function ymd(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

// Parse an ISO instant and return YYYY-MM-DD in Asia/Taipei time
export function ymdFromIsoInTpe(iso: string): string {
  const dt = new Date(iso);
  // shift to TPE (+8h) for display-only day extraction
  const shifted = new Date(dt.getTime() + 8 * 60 * 60 * 1000);
  return ymd(new Date(shifted.getFullYear(), shifted.getMonth(), shifted.getDate()));
}

export function startOfMonth(d: Date): Date { return new Date(d.getFullYear(), d.getMonth(), 1); }
export function endOfMonth(d: Date): Date { return new Date(d.getFullYear(), d.getMonth() + 1, 0); }

