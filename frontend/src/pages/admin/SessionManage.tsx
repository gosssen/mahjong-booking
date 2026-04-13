import { logError } from '../../utils/logger'
import { useEffect, useState } from 'react'
import {
  getSessions,
  createSession,
  cancelSession,
  addTable,
  removeTable,
  type Session,
} from '../../api'
import { formatSession, toISODate } from '../../utils/format'

export default function SessionManage() {
  const [sessions, setSessions] = useState<Session[]>([])
  const [loading, setLoading] = useState(true)

  // create form
  const [newDate, setNewDate] = useState('')
  const [newTime, setNewTime] = useState('19:00')
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState('')

  const today = toISODate(new Date())
  const maxDate = toISODate(new Date(new Date().setMonth(new Date().getMonth() + 2)))

  function load() {
    setLoading(true)
    getSessions(today, maxDate)
      .then(setSessions)
      .catch((e) => logError("load failed", e))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  async function handleCreate() {
    if (!newDate || !newTime) return
    setCreating(true)
    setCreateError('')
    try {
      await createSession(newDate, newTime + ':00')
      load()
      setNewDate('')
      setNewTime('19:00')
    } catch (e: any) {
      setCreateError(e.response?.data?.detail ?? e.message ?? '建立失敗')
    } finally {
      setCreating(false)
    }
  }

  async function handleCancel(session: Session) {
    const reason = prompt(`取消場次：${formatSession(session.sessionDate, session.startTime)}\n取消原因（可留空）：`) ?? ''
    try {
      await cancelSession(session.id, reason || undefined)
      load()
    } catch (e: any) {
      alert(e.response?.data?.detail ?? '取消失敗')
    }
  }

  async function handleAddTable(sessionId: number) {
    try {
      await addTable(sessionId)
      load()
    } catch (e: any) {
      alert(e.response?.data?.detail ?? '新增桌失敗')
    }
  }

  async function handleRemoveTable(sessionId: number, tableId: number) {
    if (!confirm('確定要移除這張空桌嗎？')) return
    try {
      await removeTable(sessionId, tableId)
      load()
    } catch (e: any) {
      alert(e.response?.data?.detail ?? '移除失敗（桌位有人）')
    }
  }

  // Generate time options in 10-minute intervals
  const timeOptions: string[] = []
  for (let h = 8; h <= 23; h++) {
    for (let m = 0; m < 60; m += 10) {
      timeOptions.push(`${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`)
    }
  }

  return (
    <div className="max-w-lg mx-auto p-4">
      <h1 className="text-lg font-bold text-gray-800 mb-4">場次管理</h1>

      {/* Create form */}
      <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-4 mb-4">
        <h2 className="font-medium text-gray-700 mb-3">新增場次</h2>
        <div className="flex gap-2 mb-2">
          <input
            type="date"
            min={today}
            max={maxDate}
            value={newDate}
            onChange={e => setNewDate(e.target.value)}
            className="flex-1 border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-400"
          />
          <select
            value={newTime}
            onChange={e => setNewTime(e.target.value)}
            className="border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-400"
          >
            {timeOptions.map(t => <option key={t} value={t}>{t}</option>)}
          </select>
        </div>
        {createError && <p className="text-red-500 text-xs mb-2">{createError}</p>}
        <button
          onClick={handleCreate}
          disabled={!newDate || creating}
          className="w-full py-2 rounded-lg bg-green-500 text-white text-sm font-medium hover:bg-green-600 disabled:opacity-50 transition-colors"
        >
          {creating ? '建立中...' : '建立場次'}
        </button>
      </div>

      {/* Session list */}
      {loading ? (
        <p className="text-center text-gray-400 py-8">載入中...</p>
      ) : sessions.length === 0 ? (
        <p className="text-center text-gray-400 py-8">目前無場次</p>
      ) : (
        sessions.map(s => {
          const taken = s.tables.reduce((a, t) => a + (t.reservations?.length ?? 0), 0)
          const total = s.tables.length * 4
          return (
            <div key={s.id} className="bg-white rounded-xl border border-gray-100 shadow-sm p-4 mb-3">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-gray-800">{formatSession(s.sessionDate, s.startTime)}</p>
                  <p className="text-sm text-gray-500 mt-0.5">{s.tables.length} 桌・{taken}/{total} 人</p>
                </div>
                <button
                  onClick={() => handleCancel(s)}
                  className="text-xs text-red-500 border border-red-200 rounded-lg px-2 py-1 hover:bg-red-50 transition-colors"
                >
                  取消場次
                </button>
              </div>

              {/* Tables */}
              <div className="mt-3 space-y-1">
                {s.tables.map(t => {
                  const count = t.reservations?.length ?? 0
                  const isEmpty = count === 0
                  return (
                    <div key={t.id} className="flex items-center justify-between text-sm">
                      <span className="text-gray-600">第 {t.tableNumber} 桌 ({count}/4 人)</span>
                      {isEmpty && (
                        <button
                          onClick={() => handleRemoveTable(s.id, t.id)}
                          className="text-xs text-gray-400 hover:text-red-500 transition-colors"
                        >
                          移除
                        </button>
                      )}
                    </div>
                  )
                })}
              </div>

              <button
                onClick={() => handleAddTable(s.id)}
                className="mt-2 text-sm text-green-600 hover:text-green-700 font-medium"
              >
                + 追加一桌
              </button>
            </div>
          )
        })
      )}
    </div>
  )
}
