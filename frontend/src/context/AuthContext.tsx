import React, { createContext, useContext, useEffect, useState } from 'react'
import { initLiff } from '../liff'
import { getMe, type MeResponse } from '../api'

interface AuthContextValue {
  me: MeResponse | null
  loading: boolean
  error: string | null
}

const AuthContext = createContext<AuthContextValue>({ me: null, loading: true, error: null })

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [me, setMe] = useState<MeResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    initLiff()
      .then(() => getMe())
      .then(setMe)
      .catch((e) => setError(e.message ?? '初始化失敗'))
      .finally(() => setLoading(false))
  }, [])

  return <AuthContext.Provider value={{ me, loading, error }}>{children}</AuthContext.Provider>
}

export function useAuth() {
  return useContext(AuthContext)
}
