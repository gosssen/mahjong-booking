import { logError } from './../utils/logger'
import { useEffect, useState } from 'react'
import { getSessions, bookTable, type Session, type MahjongTable } from '../api'
import { formatSession, formatDate, toISODate } from '../utils/format'
import { useAuth } from '../context/AuthContext'

type DayStatus = 'none' | 'available' | 'almost-full' | 'full'

interface DayInfo {
  date: string
  status: DayStatus
  sessions: Session[]
}

function buildCalendarDays(sessions: Session[], year: number, month: number): DayInfo[] {
  const byDate: Record<string, Session[]> = {}
  for (const s of sessions) {
    if (!byDate[s.sessionDate]) byDate[s.sessionDate] = []
    byDate[s.sessionDate].push(s)
  }

  const daysInMonth = new Date(year, month + 1, 0).getDate()
  return Array.from({ length: daysInMonth }, (_, i) => {
    const d = new Date(year, month, i + 1)
    const date = toISODate(d)
    const daySessions = byDate[date] ?? []

    let status: DayStatus = 'none'
    if (daySessions.length > 0) {
      const totalSeats = daySessions.reduce((acc, s) => acc + s.tables.length * 4, 0)
      const taken = daySessions.reduce(
        (acc, s) => acc + s.tables.reduce((a, t) => a + (t.reservations?.length ?? 0), 0),
        0
      )
      const remaining = totalSeats - taken
      if (remaining === 0) status = 'full'
      else if (remaining <= daySessions.length) status = 'almost-full'
      else status = 'available'
    }

    return { date, status, sessions: daySessions }
  })
}

function seatsLeft(table: MahjongTable): number {
  return 4 - (table.reservations?.length ?? 0)
}

export default function CalendarPage() {
  const { me } = useAuth()
  const [sessions, setSessions] = useState<Session[]>([])
  const [viewYear, setViewYear] = useState(() => new Date().getFullYear())
  const [viewMonth, setViewMonth] = useState(() => new Date().getMonth())
  const [selectedDate, setSelectedDate] = useState<string | null>(null)
  const [selectedSession, setSelectedSession] = useState<Session | null>(null)
  const [booking, setBooking] = useState(false)
  const [message, setMessage] = useState<string | null>(null)

  const today = toISODate(new Date())
  const maxDate = toISODate(new Date(new Date().setMonth(new Date().getMonth() + 2)))

  useEffect(() => {
    const from = toISODate(new Date(viewYear, viewMonth, 1))
    const to = toISODate(new Date(viewYear, viewMonth + 1, 0))
    getSessions(from, to).then(setSessions).catch((e) => logError("load failed", e))
  }, [viewYear, viewMonth])

  const days = buildCalendarDays(sessions, viewYear, viewMonth)
  const firstDow = new Date(viewYear, viewMonth, 1).getDay()

  function prevMonth() {
    if (viewMonth === 0) { setViewYear(y => y - 1); setViewMonth(11) }
    else setViewMonth(m => m - 1)
    setSelectedDate(null); setSelectedSession(null)
  }
  function nextMonth() {
    if (viewMonth === 11) { setViewYear(y => y + 1); setViewMonth(0) }
    else setViewMonth(m => m + 1)
    setSelectedDate(null); setSelectedSession(null)
  }

  function selectDay(day: DayInfo) {
    if (day.status === 'none' || day.date < today || day.date > maxDate) return
    setSelectedDate(day.date)
    setSelectedSession(null)
    setMessage(null)
  }

  async function handleBook(table: MahjongTable) {
    if (!selectedSession || booking) return
    setBooking(true)
    setMessage(null)
    try {
      await bookTable(selectedSession.id, table.id)
      const members = table.reservations?.map(r => r.displayName ?? r.lineUserId).join('、') ?? ''
      const total = (table.reservations?.length ?? 0) + 1
      setMessage(`✅ 預約成功！${formatSession(selectedSession.sessionDate, selectedSession.startTime)} 第${table.tableNumber}桌\n同桌：${members}（目前${total}人）`)
      // refresh sessions
      const from = toISODate(new Date(viewYear, viewMonth, 1))
      const to = toISODate(new Date(viewYear, viewMonth + 1, 0))
      getSessions(from, to).then(setSessions)
      setSelectedSession(null)
      setSelectedDate(null)
    } catch (e: any) {
      setMessage(`❌ ${e.response?.data?.detail ?? e.message ?? '預約失敗'}`)
    } finally {
      setBooking(false)
    }
  }

  const DOW = ['日', '一', '二', '三', '四', '五', '六']

  return (
    <div className="max-w-lg mx-auto p-4">
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <button onClick={prevMonth} className="p-2 rounded-full hover:bg-gray-200 text-xl">‹</button>
        <h1 className="text-lg font-bold">{viewYear} 年 {viewMonth + 1} 月</h1>
        <button onClick={nextMonth} className="p-2 rounded-full hover:bg-gray-200 text-xl">›</button>
      </div>

      {/* Day of week headers */}
      <div className="grid grid-cols-7 text-center text-sm font-medium text-gray-500 mb-1">
        {DOW.map(d => <div key={d}>{d}</div>)}
      </div>

      {/* Calendar grid */}
      <div className="grid grid-cols-7 gap-1">
        {Array.from({ length: firstDow }, (_, i) => <div key={`e${i}`} />)}
        {days.map(day => {
          const isToday = day.date === today
          const isPast = day.date < today || day.date > maxDate
          const isSelected = day.date === selectedDate
          const colors: Record<DayStatus, string> = {
            none: 'text-gray-300',
            available: 'bg-green-100 text-green-800 hover:bg-green-200 cursor-pointer',
            'almost-full': 'bg-orange-100 text-orange-800 hover:bg-orange-200 cursor-pointer',
            full: 'bg-gray-200 text-gray-400',
          }
          return (
            <button
              key={day.date}
              onClick={() => selectDay(day)}
              disabled={day.status === 'none' || day.status === 'full' || isPast}
              className={`
                aspect-square rounded-lg flex items-center justify-center text-sm font-medium transition-colors
                ${colors[day.status]}
                ${isToday ? 'ring-2 ring-green-500' : ''}
                ${isSelected ? 'ring-2 ring-blue-500' : ''}
                ${isPast ? 'opacity-40' : ''}
              `}
            >
              {new Date(day.date + 'T00:00:00').getDate()}
            </button>
          )
        })}
      </div>

      {/* Legend */}
      <div className="flex gap-3 mt-3 text-xs text-gray-500 flex-wrap">
        <span className="flex items-center gap-1"><span className="w-3 h-3 rounded bg-green-200 inline-block" />有空位</span>
        <span className="flex items-center gap-1"><span className="w-3 h-3 rounded bg-orange-200 inline-block" />快滿了</span>
        <span className="flex items-center gap-1"><span className="w-3 h-3 rounded bg-gray-300 inline-block" />已滿桌</span>
      </div>

      {/* Session list for selected date */}
      {selectedDate && !selectedSession && (
        <div className="mt-4">
          <h2 className="font-bold text-gray-700 mb-2">{formatDate(selectedDate)} 場次</h2>
          {days.find(d => d.date === selectedDate)?.sessions.map(s => {
            const totalSeats = s.tables.length * 4
            const taken = s.tables.reduce((a, t) => a + (t.reservations?.length ?? 0), 0)
            const remaining = totalSeats - taken
            return (
              <button
                key={s.id}
                onClick={() => setSelectedSession(s)}
                className="w-full text-left p-3 mb-2 rounded-lg border border-gray-200 bg-white hover:bg-green-50 transition-colors"
              >
                <span className="font-medium">{s.startTime.slice(0, 5)}</span>
                <span className="ml-2 text-sm text-gray-500">剩 {remaining} 位</span>
              </button>
            )
          })}
        </div>
      )}

      {/* Table selection */}
      {selectedSession && (
        <div className="mt-4">
          <button onClick={() => setSelectedSession(null)} className="text-sm text-blue-500 mb-2">← 返回場次列表</button>
          <h2 className="font-bold text-gray-700 mb-2">
            {formatSession(selectedSession.sessionDate, selectedSession.startTime)} — 選桌
          </h2>
          {selectedSession.tables.map(table => {
            const left = seatsLeft(table)
            const members = table.reservations?.map(r => r.displayName ?? '匿名').join('、') ?? ''
            const alreadyBooked = table.reservations?.some(r => r.lineUserId === me?.userId)
            return (
              <div key={table.id} className="p-3 mb-2 rounded-lg border bg-white">
                <div className="flex items-center justify-between">
                  <span className="font-medium">第 {table.tableNumber} 桌</span>
                  <span className={`text-sm ${left === 0 ? 'text-gray-400' : 'text-green-600'}`}>
                    {4 - left}/4 人
                  </span>
                </div>
                {members && <p className="text-xs text-gray-500 mt-1">{members}</p>}
                <button
                  onClick={() => handleBook(table)}
                  disabled={left === 0 || booking || !!alreadyBooked}
                  className="mt-2 w-full py-1.5 rounded-lg text-sm font-medium bg-green-500 text-white disabled:opacity-40 disabled:cursor-not-allowed hover:bg-green-600 transition-colors"
                >
                  {alreadyBooked ? '已在此桌' : left === 0 ? '已滿' : booking ? '預約中...' : '選擇此桌'}
                </button>
              </div>
            )
          })}
        </div>
      )}

      {/* Result message */}
      {message && (
        <div className={`mt-4 p-3 rounded-lg text-sm whitespace-pre-line ${message.startsWith('✅') ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
          {message}
        </div>
      )}
    </div>
  )
}
