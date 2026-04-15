import { apiErrorMessage } from '../utils/apiError'
import { logError } from './../utils/logger'
import { useEffect, useState } from 'react'
import { getMyReservations, getMyReservationHistory, cancelReservation, type Reservation } from '../api'
import { formatSession } from '../utils/format'

function ReservationCard({
  r,
  canCancel,
  cancelling,
  onCancel,
}: {
  r: Reservation
  canCancel: boolean
  cancelling: boolean
  onCancel: () => void
}) {
  const guestNote = r.guestCount > 0 ? `（含 ${r.guestCount} 位朋友）` : ''
  return (
    <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-4 mb-3">
      <p className="font-medium text-gray-800">
        {formatSession(r.sessionDate, r.sessionStartTime)}
      </p>
      <p className="text-sm text-gray-500 mt-1">
        第 {r.tableNumber} 桌{guestNote}
      </p>
      {canCancel && (
        <button
          onClick={onCancel}
          disabled={cancelling}
          className="mt-3 px-4 py-1.5 rounded-lg text-sm text-red-600 border border-red-200 hover:bg-red-50 disabled:opacity-50 transition-colors"
        >
          {cancelling ? '取消中...' : '取消預約'}
        </button>
      )}
      {r.status === 'CANCELLED' && (
        <p className="mt-2 text-xs text-gray-400">已取消</p>
      )}
    </div>
  )
}

export default function MyReservations() {
  const [upcoming, setUpcoming] = useState<Reservation[]>([])
  const [history, setHistory] = useState<Reservation[]>([])
  const [loading, setLoading] = useState(true)
  const [cancelling, setCancelling] = useState<number | null>(null)
  const [showPast, setShowPast] = useState(false)
  const [showCancelled, setShowCancelled] = useState(false)

  function load() {
    setLoading(true)
    Promise.all([getMyReservations(), getMyReservationHistory()])
      .then(([up, hist]) => { setUpcoming(up); setHistory(hist) })
      .catch((e) => logError('load failed', e))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  async function handleCancel(id: number) {
    if (!confirm('確定要取消這筆預約嗎？')) return
    setCancelling(id)
    try {
      await cancelReservation(id)
      load()
    } catch (e: any) {
      alert(apiErrorMessage(e, '取消失敗'))
    } finally {
      setCancelling(null)
    }
  }

  if (loading) {
    return <div className="flex justify-center p-8 text-gray-400">載入中...</div>
  }

  const past = history.filter(r => r.status === 'CONFIRMED')
  const cancelled = history.filter(r => r.status === 'CANCELLED')
  const hasAny = upcoming.length > 0 || past.length > 0 || cancelled.length > 0

  if (!hasAny) {
    return (
      <div className="flex flex-col items-center justify-center p-12 text-gray-400 gap-2">
        <span className="text-4xl">📋</span>
        <p>目前沒有預約記錄</p>
      </div>
    )
  }

  return (
    <div className="max-w-lg mx-auto p-4">

      {/* 預約中 */}
      {upcoming.length > 0 && (
        <section className="mb-6">
          <h2 className="text-sm font-semibold text-green-700 mb-2 flex items-center gap-1">
            <span className="w-2 h-2 rounded-full bg-green-500 inline-block" />
            預約中
          </h2>
          {upcoming.map(r => (
            <ReservationCard
              key={r.id}
              r={r}
              canCancel
              cancelling={cancelling === r.id}
              onCancel={() => handleCancel(r.id)}
            />
          ))}
        </section>
      )}

      {/* 已結束（可折疊） */}
      {past.length > 0 && (
        <section className="mb-6">
          <button
            onClick={() => setShowPast(v => !v)}
            className="w-full flex items-center justify-between mb-2"
          >
            <span className="text-sm font-semibold text-gray-400 flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-gray-400 inline-block" />
              已結束
              <span className="text-gray-300 font-normal">（{past.length} 筆）</span>
            </span>
            <span className="text-xs text-gray-400">{showPast ? '▲ 收起' : '▼ 展開'}</span>
          </button>
          {showPast && past.map(r => (
            <ReservationCard
              key={r.id}
              r={r}
              canCancel={false}
              cancelling={false}
              onCancel={() => {}}
            />
          ))}
        </section>
      )}

      {/* 已取消（可折疊） */}
      {cancelled.length > 0 && (
        <section className="mb-6">
          <button
            onClick={() => setShowCancelled(v => !v)}
            className="w-full flex items-center justify-between mb-2"
          >
            <span className="text-sm font-semibold text-red-400 flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-red-400 inline-block" />
              已取消
              <span className="text-red-300 font-normal">（{cancelled.length} 筆）</span>
            </span>
            <span className="text-xs text-gray-400">{showCancelled ? '▲ 收起' : '▼ 展開'}</span>
          </button>
          {showCancelled && cancelled.map(r => (
            <ReservationCard
              key={r.id}
              r={r}
              canCancel={false}
              cancelling={false}
              onCancel={() => {}}
            />
          ))}
        </section>
      )}
    </div>
  )
}
