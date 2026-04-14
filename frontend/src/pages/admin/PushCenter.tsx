import { logError } from '../../utils/logger'
import { useEffect, useState } from 'react'
import { getPushQuota, getSessions, type Session } from '../../api'
import api from '../../api/client'
import { formatSession, toISODate } from '../../utils/format'

export default function PushCenter() {
  const [quota, setQuota] = useState<{ used: number; remaining: number; limit: number } | null>(null)
  const [sessions, setSessions] = useState<Session[]>([])
  const [selectedSessionId, setSelectedSessionId] = useState<number | ''>('')
  const [message, setMessage] = useState('')
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState<string | null>(null)

  const today = toISODate(new Date())
  const maxDate = toISODate(new Date(new Date().setMonth(new Date().getMonth() + 2)))

  useEffect(() => {
    getPushQuota().then(setQuota).catch((e) => logError("load failed", e))
    getSessions(today, maxDate).then(setSessions).catch((e) => logError("load failed", e))
  }, [])

  async function handleSend() {
    if (!selectedSessionId || !message.trim()) return
    setSending(true)
    setResult(null)
    try {
      const res = await api.post(`/api/sessions/${selectedSessionId}/push`, { message })
      const { sent, total } = res.data
      if (total === 0) {
        setResult(`⚠️ 該場次目前無人預約，未發送推播`)
      } else {
        setResult(`✅ 已送出 ${sent} / ${total} 則推播`)
      }
      getPushQuota().then(setQuota)
      setMessage('')
    } catch (e: any) {
      setResult(`❌ ${e.response?.data?.detail ?? e.message ?? '推播失敗'}`)
    } finally {
      setSending(false)
    }
  }

  const quotaColor = !quota
    ? 'text-gray-500'
    : quota.remaining === 0
    ? 'text-red-600'
    : quota.remaining <= 30
    ? 'text-orange-500'
    : 'text-green-600'

  return (
    <div className="max-w-lg mx-auto p-4">
      <h1 className="text-lg font-bold text-gray-800 mb-4">推播中心</h1>

      {/* Quota */}
      <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-4 mb-4">
        <p className="text-sm text-gray-500 mb-1">本月推播用量</p>
        {quota ? (
          <>
            <p className={`text-2xl font-bold ${quotaColor}`}>
              {quota.used} / {quota.limit}
            </p>
            <p className={`text-sm ${quotaColor}`}>剩餘 {quota.remaining} 則</p>
            {quota.remaining === 0 && (
              <p className="mt-2 text-sm text-red-600 bg-red-50 rounded p-2">推播額度已用完，本月無法再發送推播。</p>
            )}
            {quota.remaining > 0 && quota.remaining <= 30 && (
              <p className="mt-2 text-sm text-orange-600 bg-orange-50 rounded p-2">推播額度即將用完，請謹慎使用。</p>
            )}
          </>
        ) : (
          <p className="text-gray-400">載入中...</p>
        )}
      </div>

      {/* Push form */}
      <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-4">
        <h2 className="font-medium text-gray-700 mb-3">推播訊息</h2>
        <select
          value={selectedSessionId}
          onChange={e => setSelectedSessionId(Number(e.target.value) || '')}
          className="w-full border rounded-lg px-3 py-2 text-sm mb-2 focus:outline-none focus:ring-2 focus:ring-green-400"
        >
          <option value="">選擇場次</option>
          {sessions.map(s => (
            <option key={s.id} value={s.id}>
              {formatSession(s.sessionDate, s.startTime)}
            </option>
          ))}
        </select>
        <textarea
          value={message}
          onChange={e => setMessage(e.target.value)}
          placeholder="推播內容..."
          rows={4}
          className="w-full border rounded-lg px-3 py-2 text-sm mb-2 focus:outline-none focus:ring-2 focus:ring-green-400 resize-none"
        />
        <button
          onClick={handleSend}
          disabled={!selectedSessionId || !message.trim() || sending || quota?.remaining === 0}
          className="w-full py-2 rounded-lg bg-green-500 text-white text-sm font-medium hover:bg-green-600 disabled:opacity-50 transition-colors"
        >
          {sending ? '發送中...' : '發送推播'}
        </button>
        {result && (
          <p className={`mt-2 text-sm ${result.startsWith('✅') ? 'text-green-600' : 'text-red-600'}`}>
            {result}
          </p>
        )}
      </div>
    </div>
  )
}
