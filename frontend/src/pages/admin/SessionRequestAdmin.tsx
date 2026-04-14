import { logError } from '../../utils/logger'
import { useEffect, useState } from 'react'
import { getAllSessionRequests, reviewSessionRequest, type SessionRequestRecord } from '../../api'
import { formatDate } from '../../utils/format'

const STATUS_LABEL: Record<string, string> = {
  PENDING: '待審核',
  APPROVED: '已核准',
  REJECTED: '已拒絕',
}
const STATUS_COLOR: Record<string, string> = {
  PENDING: 'bg-yellow-100 text-yellow-700',
  APPROVED: 'bg-green-100 text-green-700',
  REJECTED: 'bg-red-100 text-red-600',
}

export default function SessionRequestAdmin() {
  const [requests, setRequests] = useState<SessionRequestRecord[]>([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState<'PENDING' | 'ALL'>('PENDING')
  const [busy, setBusy] = useState<number | null>(null)

  function load() {
    setLoading(true)
    getAllSessionRequests()
      .then(setRequests)
      .catch((e) => logError('load failed', e))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const displayed = filter === 'PENDING'
    ? requests.filter(r => r.status === 'PENDING')
    : requests

  const pendingCount = requests.filter(r => r.status === 'PENDING').length

  async function handleReview(id: number, approved: boolean) {
    const action = approved ? '核准' : '拒絕'
    const note = prompt(`${action}理由（可留空）：`)
    if (note === null) return // user cancelled prompt (prompt returns null on cancel)
    setBusy(id)
    try {
      await reviewSessionRequest(id, approved, note.trim() || undefined)
      load()
    } catch (e: any) {
      alert(e.response?.data?.detail ?? `${action}失敗`)
    } finally {
      setBusy(null)
    }
  }

  return (
    <div className="max-w-lg mx-auto p-4">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-lg font-bold text-gray-800">
          場次申請
          {pendingCount > 0 && (
            <span className="ml-2 text-sm bg-red-500 text-white rounded-full px-2 py-0.5">
              {pendingCount}
            </span>
          )}
        </h1>
        <div className="flex gap-1">
          <button
            onClick={() => setFilter('PENDING')}
            className={`text-xs px-3 py-1.5 rounded-lg border transition-colors ${
              filter === 'PENDING'
                ? 'bg-green-500 text-white border-green-500'
                : 'border-gray-200 text-gray-500'
            }`}
          >
            待審核
          </button>
          <button
            onClick={() => setFilter('ALL')}
            className={`text-xs px-3 py-1.5 rounded-lg border transition-colors ${
              filter === 'ALL'
                ? 'bg-green-500 text-white border-green-500'
                : 'border-gray-200 text-gray-500'
            }`}
          >
            全部
          </button>
        </div>
      </div>

      {loading ? (
        <p className="text-center text-gray-400 py-8">載入中...</p>
      ) : displayed.length === 0 ? (
        <p className="text-center text-gray-400 py-8">
          {filter === 'PENDING' ? '目前沒有待審核申請' : '尚無申請記錄'}
        </p>
      ) : (
        <div className="space-y-3">
          {displayed.map(r => (
            <div key={r.id} className="bg-white rounded-xl border border-gray-100 shadow-sm p-4">
              {/* Header */}
              <div className="flex items-start justify-between mb-2">
                <div>
                  <p className="font-medium text-gray-800">
                    {formatDate(r.requestDate)} {r.requestTime.slice(0, 5)}
                  </p>
                  <p className="text-sm text-gray-600 mt-0.5">
                    申請人：{r.displayName ?? r.lineUserId}
                  </p>
                  {r.note && (
                    <p className="text-xs text-gray-400 mt-0.5">備注：{r.note}</p>
                  )}
                </div>
                <span className={`text-xs rounded-full px-2 py-0.5 font-medium flex-shrink-0 ${STATUS_COLOR[r.status]}`}>
                  {STATUS_LABEL[r.status]}
                </span>
              </div>

              {/* Review result */}
              {r.reviewNote && (
                <p className="text-xs text-gray-500 bg-gray-50 rounded p-2 mb-2">
                  審核備注：{r.reviewNote}
                </p>
              )}
              {r.sessionId && (
                <p className="text-xs text-green-600 mb-2">已建立場次 #{r.sessionId}</p>
              )}

              {/* Actions (only for PENDING) */}
              {r.status === 'PENDING' && (
                <div className="flex gap-2 mt-2">
                  <button
                    onClick={() => handleReview(r.id, true)}
                    disabled={busy === r.id}
                    className="flex-1 py-1.5 rounded-lg bg-green-500 text-white text-sm font-medium hover:bg-green-600 disabled:opacity-50 transition-colors"
                  >
                    {busy === r.id ? '...' : '核准並開場'}
                  </button>
                  <button
                    onClick={() => handleReview(r.id, false)}
                    disabled={busy === r.id}
                    className="flex-1 py-1.5 rounded-lg border border-red-200 text-red-500 text-sm font-medium hover:bg-red-50 disabled:opacity-50 transition-colors"
                  >
                    拒絕
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
