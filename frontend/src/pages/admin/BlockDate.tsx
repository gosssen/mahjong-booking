import { apiErrorMessage } from '../../utils/apiError'
import { logError } from '../../utils/logger'
import { useEffect, useState } from 'react'
import { getBlockedDates, blockDate, unblockDate } from '../../api'
import { formatDate, toISODate } from '../../utils/format'

interface BlockedDate {
  id: number
  blockedDate: string
  reason: string | null
}

export default function BlockDate() {
  const [blocked, setBlocked] = useState<BlockedDate[]>([])
  const [newDate, setNewDate] = useState('')
  const [newReason, setNewReason] = useState('')
  const [loading, setLoading] = useState(true)
  const [adding, setAdding] = useState(false)

  const today = toISODate(new Date())
  const maxDate = toISODate(new Date(new Date().setMonth(new Date().getMonth() + 2)))

  function load() {
    setLoading(true)
    getBlockedDates(today, maxDate)
      .then(setBlocked)
      .catch((e) => logError("load failed", e))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  async function handleBlock() {
    if (!newDate) return
    setAdding(true)
    try {
      await blockDate(newDate, newReason || undefined)
      setNewDate('')
      setNewReason('')
      load()
    } catch (e: any) {
      alert(apiErrorMessage(e, '封鎖失敗'))
    } finally {
      setAdding(false)
    }
  }

  async function handleUnblock(id: number) {
    if (!confirm('確定要解除封鎖嗎？')) return
    try {
      await unblockDate(id)
      load()
    } catch (e: any) {
      alert(apiErrorMessage(e, '解除失敗'))
    }
  }

  return (
    <div className="max-w-lg mx-auto p-4">
      <h1 className="text-lg font-bold text-gray-800 mb-4">封鎖日期</h1>

      <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-4 mb-4">
        <h2 className="font-medium text-gray-700 mb-3">封鎖新日期</h2>
        <input
          type="date"
          min={today}
          max={maxDate}
          value={newDate}
          onChange={e => setNewDate(e.target.value)}
          className="w-full border rounded-lg px-3 py-2 text-sm mb-2 focus:outline-none focus:ring-2 focus:ring-red-300"
        />
        <input
          type="text"
          placeholder="封鎖原因（選填，例如：颱風假）"
          value={newReason}
          onChange={e => setNewReason(e.target.value)}
          className="w-full border rounded-lg px-3 py-2 text-sm mb-2 focus:outline-none focus:ring-2 focus:ring-red-300"
        />
        <button
          onClick={handleBlock}
          disabled={!newDate || adding}
          className="w-full py-2 rounded-lg bg-red-500 text-white text-sm font-medium hover:bg-red-600 disabled:opacity-50 transition-colors"
        >
          {adding ? '封鎖中...' : '封鎖日期'}
        </button>
      </div>

      <h2 className="font-medium text-gray-700 mb-2">已封鎖日期</h2>
      {loading ? (
        <p className="text-center text-gray-400 py-4">載入中...</p>
      ) : blocked.length === 0 ? (
        <p className="text-center text-gray-400 py-4">無封鎖日期</p>
      ) : (
        blocked.map(b => (
          <div key={b.id} className="bg-white rounded-xl border border-gray-100 shadow-sm p-4 mb-2 flex items-center justify-between">
            <div>
              <p className="font-medium text-gray-800">{formatDate(b.blockedDate)}</p>
              {b.reason && <p className="text-xs text-gray-400 mt-0.5">{b.reason}</p>}
            </div>
            <button
              onClick={() => handleUnblock(b.id)}
              className="text-sm text-blue-500 hover:text-blue-700"
            >
              解除
            </button>
          </div>
        ))
      )}
    </div>
  )
}
