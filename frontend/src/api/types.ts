export interface MeResponse {
  userId: string
  displayName: string
  pictureUrl: string | null
  admin: boolean
  developer: boolean
}

export interface User {
  id: number
  lineUserId: string
  displayName: string
  pictureUrl: string | null
}

export interface SessionRequestRecord {
  id: number
  lineUserId: string
  displayName: string | null
  requestDate: string    // 'YYYY-MM-DD'
  requestTime: string    // 'HH:mm:ss'
  note: string | null
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  reviewedBy: string | null
  reviewNote: string | null
  sessionId: number | null
  createdAt: string
}

export interface MahjongTable {
  id: number
  sessionId: number
  tableNumber: number
  reservations: Reservation[]
}

export interface Session {
  id: number
  sessionDate: string    // 'YYYY-MM-DD'
  startTime: string      // 'HH:mm:ss'
  status: string
  tables: MahjongTable[]
}

export interface Reservation {
  id: number
  sessionId: number
  tableId: number
  lineUserId: string
  guestCount: number      // 攜帶朋友數（不含本人），預設 0
  status: string
  displayName: string | null
  sessionDate: string
  sessionStartTime: string
  tableNumber: number
}
