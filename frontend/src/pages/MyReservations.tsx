import { apiErrorMessage } from '../utils/apiError'
import { logError } from './../utils/logger'
import { useEffect, useState } from 'react'
import { getMyReservations, cancelReservation, type Reservation } from '../api'
import { formatSession } from '../utils/format'

export default function MyReservations() {
  const [reservations, setReservations] = useState<Reservation[]>([])
  const [loading, setLoading] = useState(true)
  const [cancelling, setCancelling] = useState<number | null>(null)

  function load() {
    setLoading(true)
    getMyReservations()
      .then(setReservations)
      .catch((e) => logError("load failed", e))
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
    return (
      <div className="flex justify-center p-8 text-gray-400">載入中...</div>
    )
  }

  if (reservations.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center p-12 text-gray-400 gap-2">
        <span className="text-4xl">📋</span>
        <p>目前沒有預約</p>
      </div>
    )
  }

  return (
    <div className="max-w-lg mx-auto p-4">
      <h1 className="text-lg font-bold text-gray-800 mb-4">我的預約</h1>
      {reservations.map(r => (
        <div key={r.id} className="bg-white rounded-xl border border-gray-100 shadow-sm p-4 mb-3">
          <p className="font-medium text-gray-800">
            {formatSession(r.sessionDate, r.sessionStartTime)}
          </p>
          <p className="text-sm text-gray-500 mt-1">第 {r.tableNumber} 桌</p>
          <button
            onClick={() => handleCancel(r.id)}
            disabled={cancelling === r.id}
            className="mt-3 px-4 py-1.5 rounded-lg text-sm text-red-600 border border-red-200 hover:bg-red-50 disabled:opacity-50 transition-colors"
          >
            {cancelling === r.id ? '取消中...' : '取消預約'}
          </button>
        </div>
      ))}
    </div>
  )
}
