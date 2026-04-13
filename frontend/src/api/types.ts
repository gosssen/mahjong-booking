export interface MeResponse {
  userId: string
  displayName: string
  pictureUrl: string | null
  admin: boolean
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
  status: string
  displayName: string | null
  sessionDate: string
  sessionStartTime: string
  tableNumber: number
}
