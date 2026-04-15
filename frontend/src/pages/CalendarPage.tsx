import { apiErrorMessage } from '../utils/apiError'
import { logError } from './../utils/logger'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getSessions, bookTable, type Session, type MahjongTable } from '../api'
import { formatSession, formatDate, toISODate } from '../utils/format'
import { useAuth } from '../context/AuthContext'

type DayStatus = 'none' | 'available' | 'almost-full' | 'full'

interface DayInfo {
  date: string
  status: DayStatus
  sessions: Session[]
}

function buildCalendarDays(
  sessions: Session[], year: number, month: number, today: string
): DayInfo[] {
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

    // 過去日期一律不顯示場次顏色
    if (date < today) return { date, status: 'none', sessions: daySessions }

    let status: DayStatus = 'none'
    if (daySessions.length > 0) {
      const totalSeats = daySessions.reduce((acc, s) => acc + s.tables.length * 4, 0)
      const taken = daySessions.reduce(
        (acc, s) => acc + s.tables.reduce(
          (a, t) => a + (t.reservations ?? []).reduce((sum, r) => sum + 1 + (r.guestCount ?? 0), 0),
          0
        ),
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

/** 計算桌上剩餘座位（含各人攜伴） */
function seatsLeft(table: MahjongTable): number {
  const taken = (table.reservations ?? []).reduce((acc, r) => acc + 1 + (r.guestCount ?? 0), 0)
  return 4 - taken
}

export default function CalendarPage() {
  const { me } = useAuth()
  const navigate = useNavigate()
  const [sessions, setSessions] = useState<Session[]>([])
  const [viewYear, setViewYear] = useState(() => new Date().getFullYear())
  const [viewMonth, setViewMonth] = useState(() => new Date().getMonth())
  const [selectedDate, setSelectedDate] = useState<string | null>(null)
  const [selectedSession, setSelectedSession] = useState<Session | null>(null)
  const [booking, setBooking] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [guestCount, setGuestCount] = useState(0)

  const today = toISODate(new Date())
  const maxDate = toISODate(new Date(new Date().setMonth(new Date().getMonth() + 2)))

  useEffect(() => {
    const from = toISODate(new Date(viewYear, viewMonth, 1))
    const to = toISODate(new Date(viewYear, viewMonth + 1, 0))
    getSessions(from, to).then(setSessions).catch((e) => logError("load failed", e))
  }, [viewYear, viewMonth])

  const days = buildCalendarDays(sessions, viewYear, viewMonth, today)
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
    const isPast = day.date < today || day.date > maxDate
    if (isPast) return
    setSelectedDate(day.date)
    setSelectedSession(null)
    setGuestCount(0)
    setMessage(null)
  }

  async function handleBook(table: MahjongTable) {
    if (!selectedSession || booking) return
    setBooking(true)
    setMessage(null)
    try {
      await bookTable(selectedSession.id, table.id, guestCount)
      const members = table.reservations?.map(r => r.displayName ?? r.lineUserId).join('、') ?? ''
      const totalOld = (table.reservations ?? []).reduce((acc, r) => acc + 1 + (r.guestCount ?? 0), 0)
      const totalNew = totalOld + 1 + guestCount
      const guestNote = guestCount > 0 ? `（含 ${guestCount} 位朋友）` : ''
      setMessage(`✅ 預約成功！${formatSession(selectedSession.sessionDate, selectedSession.startTime)} 第${table.tableNumber}桌${guestNote}\n同桌：${members}（目前${totalNew}人）`)
      // refresh sessions
      const from = toISODate(new Date(viewYear, viewMonth, 1))
      const to = toISODate(new Date(viewYear, viewMonth + 1, 0))
      getSessions(from, to).then(setSessions)
      setSelectedSession(null)
      setSelectedDate(null)
      setGuestCount(0)
    } catch (e: any) {
      setMessage(`❌ ${apiErrorMessage(e, '預約失敗')}`)
    } finally {
      setBooking(false)
    }
  }

  const DOW = ['日', '一', '二', '三', '四', '五', '六']

  const selectedDay = selectedDate ? days.find(d => d.date === selectedDate) : null
  const hasNoSession = selectedDay?.status === 'none'

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
            none: `text-gray-400 ${!isPast ? 'hover:bg-gray-100 cursor-pointer' : ''}`,
            available: 'bg-green-100 text-green-800 hover:bg-green-200 cursor-pointer',
            'almost-full': 'bg-orange-100 text-orange-800 hover:bg-orange-200 cursor-pointer',
            full: 'bg-gray-200 text-gray-400 cursor-pointer',
          }
          return (
            <button
              key={day.date}
              onClick={() => !isPast && selectDay(day)}
              disabled={isPast}
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

      {/* Selected date with no session — offer to apply */}
      {selectedDate && hasNoSession && !me?.admin && (
        <div className="mt-4 p-4 rounded-xl bg-gray-50 border border-gray-200 text-center">
          <p className="text-gray-600 text-sm mb-3">
            {formatDate(selectedDate)} 目前沒有場次
          </p>
          <button
            onClick={() => navigate('/session-requests')}
            className="px-4 py-2 rounded-lg bg-green-500 text-white text-sm font-medium hover:bg-green-600 transition-colors"
          >
            申請開場
          </button>
        </div>
      )}

      {/* Session list for selected date */}
      {selectedDate && !hasNoSession && !selectedSession && (
        <div className="mt-4">
          <h2 className="font-bold text-gray-700 mb-2">{formatDate(selectedDate)} 場次</h2>
          {selectedDay?.sessions.map(s => {
            const totalSeats = s.tables.length * 4
            const taken = s.tables.reduce(
              (a, t) => a + (t.reservations ?? []).reduce((sum, r) => sum + 1 + (r.guestCount ?? 0), 0),
              0
            )
            const remaining = totalSeats - taken
            const isFull = remaining === 0
            return (
              <button
                key={s.id}
                onClick={() => !isFull && setSelectedSession(s)}
                disabled={isFull}
                className={`w-full text-left p-3 mb-2 rounded-lg border bg-white transition-colors
                  ${isFull ? 'opacity-60 cursor-not-allowed' : 'hover:bg-green-50 border-gray-200'}`}
              >
                <span className="font-medium">{s.startTime.slice(0, 5)}</span>
                <span className={`ml-2 text-sm ${isFull ? 'text-gray-400' : 'text-gray-500'}`}>
                  {isFull ? '已滿桌' : `剩 ${remaining} 位`}
                </span>
              </button>
            )
          })}
        </div>
      )}

      {/* Table selection */}
      {selectedSession && (
        <div className="mt-4">
          <button onClick={() => { setSelectedSession(null); setGuestCount(0) }} className="text-sm text-blue-500 mb-2">← 返回場次列表</button>
          <h2 className="font-bold text-gray-700 mb-2">
            {formatSession(selectedSession.sessionDate, selectedSession.startTime)} — 選桌
          </h2>

          {/* 攜伴人數選擇 */}
          <div className="mb-3 p-3 rounded-lg bg-blue-50 border border-blue-100">
            <p className="text-sm text-blue-700 font-medium mb-2">帶幾位朋友一起來？</p>
            <div className="flex gap-2">
              {[0, 1, 2, 3].map(n => (
                <button
                  key={n}
                  onClick={() => setGuestCount(n)}
                  className={`flex-1 py-1.5 rounded-lg text-sm font-medium border transition-colors
                    ${guestCount === n
                      ? 'bg-blue-500 text-white border-blue-500'
                      : 'bg-white text-gray-600 border-gray-300 hover:border-blue-300'}`}
                >
                  {n === 0 ? '只有我' : `+${n} 位`}
                </button>
              ))}
            </div>
            {guestCount > 0 && (
              <p className="text-xs text-blue-500 mt-1.5">共佔 {1 + guestCount} 個座位</p>
            )}
          </div>

          {selectedSession.tables.map(table => {
            const left = seatsLeft(table)
            const canBook = left >= 1 + guestCount
            const members = table.reservations?.map(r => {
              const name = r.displayName ?? '匿名'
              return r.guestCount > 0 ? `${name}(+${r.guestCount})` : name
            }).join('、') ?? ''
            const alreadyBooked = table.reservations?.some(r => r.lineUserId === me?.userId)
            return (
              <div key={table.id} className="p-3 mb-2 rounded-lg border bg-white">
                <div className="flex items-center justify-between">
                  <span className="font-medium">第 {table.tableNumber} 桌</span>
                  <span className={`text-sm ${left === 0 ? 'text-gray-400' : 'text-green-600'}`}>
                    {4 - left}/4 人・剩 {left} 位
                  </span>
                </div>
                {members && <p className="text-xs text-gray-500 mt-1">{members}</p>}
                <button
                  onClick={() => handleBook(table)}
                  disabled={!canBook || booking || !!alreadyBooked}
                  className="mt-2 w-full py-1.5 rounded-lg text-sm font-medium bg-green-500 text-white disabled:opacity-40 disabled:cursor-not-allowed hover:bg-green-600 transition-colors"
                >
                  {alreadyBooked ? '已在此桌' : !canBook ? `空位不足（需 ${1 + guestCount} 位）` : booking ? '預約中...' : '選擇此桌'}
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
