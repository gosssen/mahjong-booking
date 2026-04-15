import { apiErrorMessage } from '../../utils/apiError'
import { logError } from '../../utils/logger'
import { useEffect, useState } from 'react'
import {
  getSessions,
  getSessionReservations,
  swapTables,
  moveToTable,
  splitGuest,
  cancelReservation,
  type Session,
  type Reservation,
} from '../../api'
import { formatSession, toISODate } from '../../utils/format'

// 選取狀態：真實用戶 or 朋友格
type SelectedItem =
  | { kind: 'person'; res: Reservation }
  | { kind: 'guest'; parentRes: Reservation }

export default function TableLayout() {
  const [sessions, setSessions] = useState<Session[]>([])
  const [selectedSession, setSelectedSession] = useState<Session | null>(null)
  const [reservations, setReservations] = useState<Reservation[]>([])
  const [selected, setSelected] = useState<SelectedItem | null>(null)
  const [loading, setLoading] = useState(false)

  const today = toISODate(new Date())
  const maxDate = toISODate(new Date(new Date().setMonth(new Date().getMonth() + 2)))

  useEffect(() => {
    getSessions(today, maxDate).then(setSessions).catch((e) => logError('load failed', e))
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

  // Group by tableId
  const byTable: Record<number, Reservation[]> = {}
  for (const r of reservations) {
    if (!byTable[r.tableId]) byTable[r.tableId] = []
    byTable[r.tableId].push(r)
  }

  // 點人物格
  async function handleSelectPerson(res: Reservation) {
    if (!selected) { setSelected({ kind: 'person', res }); return }

    // 再次點同一人 → 取消選取
    if (selected.kind === 'person' && selected.res.id === res.id) {
      setSelected(null); return
    }

    // 已選朋友格時，點人物格無效（朋友只能移桌，不能對調）
    if (selected.kind === 'guest') {
      setSelected({ kind: 'person', res }); return
    }

    // 兩人對調
    if (!confirm(`對調 ${selected.res.displayName ?? '?'} 和 ${res.displayName ?? '?'} 的桌位嗎？`)) {
      setSelected(null); return
    }
    try {
      await swapTables(selected.res.id, res.id)
      setSelected(null)
      await loadReservations(selectedSession!)
    } catch (e: any) {
      alert(apiErrorMessage(e, '對調失敗'))
      setSelected(null)
    }
  }

  // 點朋友格
  function handleSelectGuest(parentRes: Reservation) {
    if (selected?.kind === 'guest' && selected.parentRes.id === parentRes.id) {
      setSelected(null); return
    }
    setSelected({ kind: 'guest', parentRes })
  }

  // 點桌頭：移桌 or 拆朋友
  async function handleTableClick(tableId: number) {
    if (!selected) return

    if (selected.kind === 'person') {
      if (selected.res.tableId === tableId) { setSelected(null); return }
      if (!confirm(`將 ${selected.res.displayName ?? '?'} 移至此桌？`)) return
      try {
        await moveToTable(selected.res.id, tableId)
        setSelected(null)
        await loadReservations(selectedSession!)
      } catch (e: any) {
        alert(apiErrorMessage(e, '移桌失敗'))
        setSelected(null)
      }
    } else {
      // 拆朋友
      if (selected.parentRes.tableId === tableId) { setSelected(null); return }
      const guestName = selected.parentRes.displayName ?? '?'
      if (!confirm(`將 ${guestName} 的一位朋友移至此桌？`)) return
      try {
        await splitGuest(selected.parentRes.id, tableId)
        setSelected(null)
        await loadReservations(selectedSession!)
      } catch (e: any) {
        alert(apiErrorMessage(e, '移桌失敗'))
        setSelected(null)
      }
    }
  }

  async function handleCancelReservation(res: Reservation) {
    const label = res.guestLabel ?? res.displayName ?? res.lineUserId
    const note = prompt(`取消 ${label} 的預約？\n備註（可留空）：`) ?? ''
    if (note === null) return
    try {
      await cancelReservation(res.id, note || undefined)
      await loadReservations(selectedSession!)
    } catch (e: any) {
      alert(apiErrorMessage(e, '取消失敗'))
    }
  }

  // ── 場次列表畫面 ───────────────────────────────────────────────
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

  // ── 桌位配置畫面 ───────────────────────────────────────────────
  return (
    <div className="max-w-lg mx-auto p-4">
      <button onClick={() => setSelectedSession(null)} className="text-sm text-blue-500 mb-2">← 返回場次列表</button>
      <h1 className="text-lg font-bold text-gray-800 mb-1">桌位配置</h1>
      <p className="text-sm text-gray-500 mb-3">{formatSession(selectedSession.sessionDate, selectedSession.startTime)}</p>

      {/* 選取提示 */}
      {selected && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-2 mb-3 text-sm text-blue-700">
          {selected.kind === 'person' ? (
            <>已選取：<strong>{selected.res.displayName ?? selected.res.lineUserId}</strong>
            ｜點另一人對調，或點桌名移動，或再次點擊取消</>
          ) : (
            <>已選取：<strong>{selected.parentRes.displayName ?? '?'} 的朋友</strong>
            ｜點目標桌名移動此朋友，或再次點擊朋友格取消</>
          )}
        </div>
      )}

      {loading ? (
        <p className="text-center text-gray-400 py-8">載入中...</p>
      ) : (
        selectedSession.tables.map(t => {
          const seated = byTable[t.id] ?? []
          const occupiedSeats = seated.reduce((a, r) => a + 1 + (r.guestCount ?? 0), 0)
          const emptySlots = 4 - occupiedSeats
          const isTargetable = selected && (
            selected.kind === 'person' ? selected.res.tableId !== t.id
            : selected.parentRes.tableId !== t.id
          )

          return (
            <div
              key={t.id}
              className={`bg-white rounded-xl border shadow-sm p-4 mb-3 transition-colors
                ${isTargetable ? 'border-blue-300 cursor-pointer hover:bg-blue-50' : 'border-gray-100'}`}
              onClick={() => isTargetable && handleTableClick(t.id)}
            >
              <p className="font-medium text-gray-700 mb-2">第 {t.tableNumber} 桌 ({occupiedSeats}/4)</p>
              <div className="grid grid-cols-2 gap-2">
                {seated.map(res => (
                  <div key={res.id}>
                    {/* 本人格（LINE 用戶或拆出的獨立朋友記錄） */}
                    <div
                      onClick={e => { e.stopPropagation(); handleSelectPerson(res) }}
                      className={`flex items-center justify-between rounded-lg px-3 py-2 cursor-pointer transition-colors text-sm mb-2
                        ${selected?.kind === 'person' && selected.res.id === res.id
                          ? 'bg-blue-200 border-blue-400 border'
                          : 'bg-gray-50 border border-gray-200 hover:bg-gray-100'}`}
                    >
                      <span className="truncate">
                        {res.displayName ?? res.guestLabel ?? res.lineUserId}
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

                    {/* 攜伴佔位格（可點選以拆出移動） */}
                    {Array.from({ length: res.guestCount ?? 0 }, (_, gi) => (
                      <div
                        key={`guest-${res.id}-${gi}`}
                        onClick={e => { e.stopPropagation(); handleSelectGuest(res) }}
                        className={`rounded-lg border px-3 py-2 text-xs cursor-pointer transition-colors mb-2
                          ${selected?.kind === 'guest' && selected.parentRes.id === res.id
                            ? 'bg-blue-200 border-blue-400 text-blue-700'
                            : 'bg-blue-50 border-blue-100 text-blue-400 hover:bg-blue-100'}`}
                      >
                        {res.displayName ?? '?'} 的朋友
                      </div>
                    ))}
                  </div>
                ))}

                {/* 空位 */}
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
