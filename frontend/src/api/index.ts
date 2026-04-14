import api from './client'
import type { MeResponse, Session, Reservation, MahjongTable, User, SessionRequestRecord } from './types'

export * from './types'

export async function getMe(): Promise<MeResponse> {
  const res = await api.get<MeResponse>('/api/me')
  return res.data
}

export async function getSessions(from?: string, to?: string): Promise<Session[]> {
  const res = await api.get<Session[]>('/api/sessions', { params: { from, to } })
  return res.data
}

export async function getSession(id: number): Promise<Session> {
  const res = await api.get<Session>(`/api/sessions/${id}`)
  return res.data
}

export async function createSession(date: string, startTime: string): Promise<Session> {
  const res = await api.post<Session>('/api/sessions', { date, startTime })
  return res.data
}

export async function updateSessionTime(sessionId: number, startTime: string): Promise<void> {
  await api.put(`/api/sessions/${sessionId}/time`, { startTime })
}

export async function cancelSession(sessionId: number, reason?: string): Promise<void> {
  await api.delete(`/api/sessions/${sessionId}`, { data: { reason } })
}

export async function addTable(sessionId: number): Promise<MahjongTable> {
  const res = await api.post<MahjongTable>(`/api/sessions/${sessionId}/tables`)
  return res.data
}

export async function removeTable(sessionId: number, tableId: number): Promise<void> {
  await api.delete(`/api/sessions/${sessionId}/tables/${tableId}`)
}

export async function bookTable(sessionId: number, tableId: number): Promise<Reservation> {
  const res = await api.post<Reservation>('/api/reservations', { sessionId, tableId })
  return res.data
}

export async function getMyReservations(): Promise<Reservation[]> {
  const res = await api.get<Reservation[]>('/api/reservations/my')
  return res.data
}

export async function cancelReservation(id: number, reason?: string): Promise<void> {
  await api.delete(`/api/reservations/${id}`, { data: { reason } })
}

export async function getSessionReservations(sessionId: number): Promise<Reservation[]> {
  const res = await api.get<Reservation[]>(`/api/sessions/${sessionId}/reservations`)
  return res.data
}

export async function swapTables(reservationId1: number, reservationId2: number): Promise<void> {
  await api.post('/api/reservations/swap', { reservationId1, reservationId2 })
}

export async function moveToTable(reservationId: number, tableId: number): Promise<void> {
  await api.put(`/api/reservations/${reservationId}/table`, { tableId })
}

export async function getBlockedDates(from?: string, to?: string) {
  const res = await api.get('/api/blocked-dates', { params: { from, to } })
  return res.data
}

export async function blockDate(date: string, reason?: string) {
  const res = await api.post('/api/blocked-dates', { date, reason })
  return res.data
}

export async function unblockDate(id: number): Promise<void> {
  await api.delete(`/api/blocked-dates/${id}`)
}

export async function getPushQuota(): Promise<{ used: number; remaining: number; limit: number }> {
  const res = await api.get('/api/push-quota')
  return res.data
}

export async function getAdmins() {
  const res = await api.get('/api/admins')
  return res.data
}

export async function addAdmin(lineUserId: string) {
  await api.post('/api/admins', { lineUserId })
}

export async function removeAdmin(id: number) {
  await api.delete(`/api/admins/${id}`)
}

// ── 用戶名單 ───────────────────────────────────────────────────

export async function getUsers(): Promise<User[]> {
  const res = await api.get<User[]>('/api/users')
  return res.data
}

// 透過 lineUserId 設定 / 取消管理員
export async function setAdminByUserId(lineUserId: string): Promise<void> {
  await api.post(`/api/admins/by-user/${lineUserId}`)
}

export async function removeAdminByUserId(lineUserId: string): Promise<void> {
  await api.delete(`/api/admins/by-user/${lineUserId}`)
}

// ── 場次申請 ───────────────────────────────────────────────────

export async function applySession(
  date: string,
  startTime: string,
  note?: string
): Promise<SessionRequestRecord> {
  const res = await api.post<SessionRequestRecord>('/api/session-requests', { date, startTime, note })
  return res.data
}

export async function getMySessionRequests(): Promise<SessionRequestRecord[]> {
  const res = await api.get<SessionRequestRecord[]>('/api/session-requests/my')
  return res.data
}

export async function getPendingRequests(): Promise<SessionRequestRecord[]> {
  const res = await api.get<SessionRequestRecord[]>('/api/session-requests/pending')
  return res.data
}

export async function getAllSessionRequests(): Promise<SessionRequestRecord[]> {
  const res = await api.get<SessionRequestRecord[]>('/api/session-requests')
  return res.data
}

export async function reviewSessionRequest(
  id: number,
  approved: boolean,
  note?: string
): Promise<SessionRequestRecord> {
  const res = await api.post<SessionRequestRecord>(`/api/session-requests/${id}/review`, { approved, note })
  return res.data
}
