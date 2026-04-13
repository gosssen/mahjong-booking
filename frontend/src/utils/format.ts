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

/** Date → 'YYYY-MM-DD' */
export function toISODate(d: Date): string {
  return d.toISOString().slice(0, 10)
}
