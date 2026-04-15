const DAY_NAMES = ['日', '一', '二', '三', '四', '五', '六']

/** '2026-04-19' + '19:30:00' → '2026/04/19（日）19:30' */
export function formatSession(date: string, time: string): string {
  const d = new Date(date + 'T00:00:00')
  const dayName = DAY_NAMES[d.getDay()]
  const hhmm = time.slice(0, 5)
  return `${date.replace(/-/g, '/')}（${dayName}）${hhmm}`
}

/** '2026-04-19' → '2026/04/19（日）' */
export function formatDate(date: string): string {
  const d = new Date(date + 'T00:00:00')
  const dayName = DAY_NAMES[d.getDay()]
  return `${date.replace(/-/g, '/')}（${dayName}）`
}

/** '19:30:00' → '19:30' */
export function formatTime(time: string): string {
  return time.slice(0, 5)
}

/** Date → 'YYYY-MM-DD'（用本地時間，避免 UTC 跨日偏移） */
export function toISODate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}
