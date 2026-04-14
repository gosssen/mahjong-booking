import { apiErrorMessage } from '../utils/apiError'
import { logError } from '../utils/logger'
import { useEffect, useState } from 'react'
import { applySession, getMySessionRequests, type SessionRequestRecord } from '../api'
import { formatDate, toISODate } from '../utils/format'

const STATUS_LABEL: Record<string, string> = {
  PENDING: '待審核',
  APPROVED: '已核准',
  REJECTED: '未核准',
}
const STATUS_COLOR: Record<string, string> = {
  PENDING: 'bg-yellow-100 text-yellow-700',
  APPROVED: 'bg-green-100 text-green-700',
  REJECTED: 'bg-red-100 text-red-600',
}

export default function SessionRequestPage() {
  const [requests, setRequests] = useState<SessionRequestRecord[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)

  // form state
  const today = toISODate(new Date())
  const maxDate = toISODate(new Date(new Date().setMonth(new Date().getMonth() + 2)))
  const [date, setDate] = useState('')
  const [time, setTime] = useState('19:00')
  const [note, setNote] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState('')

  // 所有 10 分鐘間隔的時間選項，今天只顯示尚未過去的時段
  function getTimeOptions(forDate: string): string[] {
    const all: string[] = []
    for (let h = 8; h <= 23; h++) {
      for (let m = 0; m < 60; m += 10) {
        all.push(`${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`)
      }
    }
    if (forDate !== today) return all
    const now = new Date()
    const nowMinutes = now.getHours() * 60 + now.getMinutes()
    return all.filter(t => {
      const [h, m] = t.split(':').map(Number)
      return h * 60 + m > nowMinutes
    })
  }

  const timeOptions = getTimeOptions(date)

  function load() {
    setLoading(true)
    getMySessionRequests()
      .then(setRequests)
      .catch((e) => logError('load failed', e))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  async function handleSubmit() {
    if (!date) return
    setSubmitting(true)
    setSubmitError('')
    try {
      await applySession(date, time + ':00', note.trim() || undefined)
      setShowForm(false)
      setDate('')
      setNote('')
      load()
    } catch (e: any) {
      setSubmitError(apiErrorMessage(e, '送出失敗'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="max-w-lg mx-auto p-4">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-lg font-bold text-gray-800">場次申請</h1>
        <button
          onClick={() => setShowForm(v => !v)}
          className="text-sm px-3 py-1.5 rounded-lg bg-green-500 text-white hover:bg-green-600 transition-colors"
        >
          {showForm ? '取消' : '+ 申請場次'}
        </button>
      </div>

      {/* Submit form */}
      {showForm && (
        <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-4 mb-4">
          <h2 className="font-medium text-gray-700 mb-3">填寫申請資料</h2>
          <div className="space-y-3">
            <div>
              <label className="text-xs text-gray-500 mb-1 block">希望日期</label>
              <input
                type="date"
                min={today}
                max={maxDate}
                value={date}
                onChange={e => setDate(e.target.value)}
                className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-400"
              />
            </div>
            <div>
              <label className="text-xs text-gray-500 mb-1 block">希望時間</label>
              <select
                value={time}
                onChange={e => setTime(e.target.value)}
                className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-400"
              >
                {timeOptions.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs text-gray-500 mb-1 block">備注（選填）</label>
              <input
                type="text"
                placeholder="例如：週末晚場，麻煩盡量安排"
                value={note}
                onChange={e => setNote(e.target.value)}
                maxLength={100}
                className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-400"
              />
            </div>
          </div>
          {submitError && <p className="text-red-500 text-xs mt-2">{submitError}</p>}
          <button
            onClick={handleSubmit}
            disabled={!date || submitting}
            className="mt-3 w-full py-2 rounded-lg bg-green-500 text-white text-sm font-medium hover:bg-green-600 disabled:opacity-50 transition-colors"
          >
            {submitting ? '送出中...' : '送出申請'}
          </button>
          <p className="text-xs text-gray-400 mt-2 text-center">
            申請送出後，管理員審核核准才會開放場次
          </p>
        </div>
      )}

      {/* My requests */}
      <h2 className="font-medium text-gray-700 mb-2">我的申請記錄</h2>
      {loading ? (
        <p className="text-center text-gray-400 py-6">載入中...</p>
      ) : requests.length === 0 ? (
        <p className="text-center text-gray-400 py-6">尚無申請記錄</p>
      ) : (
        <div className="space-y-2">
          {requests.map(r => (
            <div key={r.id} className="bg-white rounded-xl border border-gray-100 shadow-sm p-4">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-gray-800">
                    {formatDate(r.requestDate)} {r.requestTime.slice(0, 5)}
                  </p>
                  {r.note && <p className="text-xs text-gray-500 mt-0.5">{r.note}</p>}
                </div>
                <span className={`text-xs rounded-full px-2 py-0.5 font-medium ${STATUS_COLOR[r.status]}`}>
                  {STATUS_LABEL[r.status]}
                </span>
              </div>
              {r.reviewNote && (
                <p className="text-xs text-gray-500 mt-2 border-t border-gray-100 pt-2">
                  管理員備注：{r.reviewNote}
                </p>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
