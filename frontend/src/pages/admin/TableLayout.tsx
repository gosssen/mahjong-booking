import { apiErrorMessage } from '../../utils/apiError'
import { logError } from '../../utils/logger'
import { useEffect, useState } from 'react'
import {
  getSessions,
  getSessionReservations,
  swapTables,
  moveToTable,
  cancelReservation,
  type Session,
  type Reservation,
} from '../../api'
import { formatSession, toISODate } from '../../utils/format'

export default function TableLayout() {
  const [sessions, setSessions] = useState<Session[]>([])
  const [selectedSession, setSelectedSession] = useState<Session | null>(null)
  const [reservations, setReservations] = useState<Reservation[]>([])
  const [selected, setSelected] = useState<Reservation | null>(null)
  const [loading, setLoading] = useState(false)

  const today = toISODate(new Date())
  const maxDate = toISODate(new Date(new Date().setMonth(new Date().getMonth() + 2)))

  useEffect(() => {
    getSessions(today, maxDate).then(setSessions).catch((e) => logError("load failed", e))
  }, [])

  async function loadReservations(session: Session) {
    setSelectedSession(session)
    setLoading(true)
    setSelected(null)
    try {
      const res = await getSessionReservations(session.id)
      setReservations(res)
    } finally {
      setLoading(false)
    }
  }

  // Group reservations by tableId
  const byTable: Record<number, Reservation[]> = {}
  for (const r of reservations) {
    if (!byTable[r.tableId]) byTable[r.tableId] = []
    byTable[r.tableId].push(r)
  }

  async function handleSelectPerson(res: Reservation) {
    if (!selected) {
      setSelected(res)
      return
    }
    if (selected.id === res.id) {
      setSelected(null)
      return
    }
    // Swap the two
    if (!confirm(`對調 ${selected.displayName ?? '?'} 和 ${res.displayName ?? '?'} 的桌位嗎？`)) {
      setSelected(null)
      return
    }
    try {
      await swapTables(selected.id, res.id)
      setSelected(null)
      await loadReservations(selectedSession!)
    } catch (e: any) {
      alert(apiErrorMessage(e, '對調失敗'))
      setSelected(null)
    }
  }

  async function handleMoveToTable(tableId: number) {
    if (!selected) return
    if (selected.tableId === tableId) { setSelected(null); return }
    if (!confirm(`將 ${selected.displayName ?? '?'} 移至此桌？`)) return
    try {
      await moveToTable(selected.id, tableId)
      setSelected(null)
      await loadReservations(selectedSession!)
    } catch (e: any) {
      alert(apiErrorMessage(e, '移桌失敗'))
      setSelected(null)
    }
  }

  async function handleCancelReservation(res: Reservation) {
    const note = prompt(`取消 ${res.displayName ?? res.lineUserId} 的預約？\n備註（可留空）：`) ?? ''
    if (note === null) return
    try {
      await cancelReservation(res.id, note || undefined)
      await loadReservations(selectedSession!)
    } catch (e: any) {
      alert(apiErrorMessage(e, '取消失敗'))
    }
  }

  if (!selectedSession) {
    return (
      <div className="max-w-lg mx-auto p-4">
        <h1 className="text-lg font-bold text-gray-800 mb-4">桌位配置</h1>
        {sessions.length === 0 ? (
          <p className="text-center text-gray-400 py-8">目前無場次</p>
        ) : (
          sessions.map(s => (
            <button
              key={s.id}
              onClick={() => loadReservations(s)}
              className="w-full text-left bg-white rounded-xl border border-gray-100 shadow-sm p-4 mb-2 hover:bg-green-50 transition-colors"
            >
              <p className="font-medium text-gray-800">{formatSession(s.sessionDate, s.startTime)}</p>
              <p className="text-sm text-gray-400">{s.tables.length} 桌</p>
            </button>
          ))
        )}
      </div>
    )
  }

  return (
    <div className="max-w-lg mx-auto p-4">
      <button onClick={() => setSelectedSession(null)} className="text-sm text-blue-500 mb-2">← 返回場次列表</button>
      <h1 className="text-lg font-bold text-gray-800 mb-1">桌位配置</h1>
      <p className="text-sm text-gray-500 mb-3">{formatSession(selectedSession.sessionDate, selectedSession.startTime)}</p>

      {selected && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-2 mb-3 text-sm text-blue-700">
          已選取：<strong>{selected.displayName ?? selected.lineUserId}</strong>
          ｜點另一人桌位對調，或點桌名移動，或點同一人取消選取
        </div>
      )}

      {loading ? (
        <p className="text-center text-gray-400 py-8">載入中...</p>
      ) : (
        selectedSession.tables.map(t => {
          const seated = byTable[t.id] ?? []
          const occupiedSeats = seated.reduce((a, r) => a + 1 + (r.guestCount ?? 0), 0)
          const emptySlots = 4 - occupiedSeats
          return (
            <div
              key={t.id}
              className={`bg-white rounded-xl border shadow-sm p-4 mb-3 ${selected && selected.tableId !== t.id ? 'border-blue-300 cursor-pointer hover:bg-blue-50' : 'border-gray-100'}`}
              onClick={() => selected && selected.tableId !== t.id && handleMoveToTable(t.id)}
            >
              <p className="font-medium text-gray-700 mb-2">第 {t.tableNumber} 桌 ({occupiedSeats}/4)</p>
              <div className="grid grid-cols-2 gap-2">
                {seated.map(res => (
                  <>
                    {/* 本人格子（可選取/對調/取消） */}
                    <div
                      key={res.id}
                      onClick={e => { e.stopPropagation(); handleSelectPerson(res) }}
                      className={`flex items-center justify-between rounded-lg px-3 py-2 cursor-pointer transition-colors text-sm
                        ${selected?.id === res.id ? 'bg-blue-200 border-blue-400 border' : 'bg-gray-50 border border-gray-200 hover:bg-gray-100'}`}
                    >
                      <span className="truncate">
                        {res.displayName ?? res.lineUserId}
                        {(res.guestCount ?? 0) > 0 && (
                          <span className="text-xs text-blue-500 ml-1">(+{res.guestCount})</span>
                        )}
                      </span>
                      <button
                        onClick={e => { e.stopPropagation(); handleCancelReservation(res) }}
                        className="ml-1 text-gray-300 hover:text-red-400 text-xs shrink-0"
                        title="取消預約"
                      >✕</button>
                    </div>
                    {/* 攜伴佔位格（僅顯示，不可互動） */}
                    {Array.from({ length: res.guestCount ?? 0 }, (_, gi) => (
                      <div
                        key={`guest-${res.id}-${gi}`}
                        className="rounded-lg border border-blue-100 bg-blue-50 px-3 py-2 text-xs text-blue-400 text-center"
                      >
                        {res.displayName ?? '?'} 的朋友
                      </div>
                    ))}
                  </>
                ))}
                {Array.from({ length: emptySlots }, (_, i) => (
                  <div key={`empty-${i}`} className="rounded-lg border border-dashed border-gray-200 px-3 py-2 text-sm text-gray-300 text-center">
                    空位
                  </div>
                ))}
              </div>
            </div>
          )
        })
      )}
    </div>
  )
}
