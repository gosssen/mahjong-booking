import { apiErrorMessage } from '../../utils/apiError'
import { logError } from '../../utils/logger'
import { useEffect, useState } from 'react'
import { getAdmins, getUsers, setAdminByUserId, removeAdminByUserId, type User } from '../../api'
import { useAuth } from '../../context/AuthContext'

interface Admin {
  id: number
  lineUserId: string
  displayName?: string
  addedBy?: string
}

export default function AdminManage() {
  const { me } = useAuth()
  const [admins, setAdmins] = useState<Admin[]>([])
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState<string | null>(null) // lineUserId currently being toggled

  const adminUserIds = new Set(admins.map(a => a.lineUserId))

  function load() {
    setLoading(true)
    Promise.all([getAdmins(), getUsers()])
      .then(([a, u]) => { setAdmins(a); setUsers(u) })
      .catch((e) => logError('load failed', e))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  async function handleToggle(user: User) {
    const isAdmin = adminUserIds.has(user.lineUserId)
    if (!confirm(`確定要${isAdmin ? '取消' : '設定'} ${user.displayName} 的管理員身分嗎？`)) return
    setBusy(user.lineUserId)
    try {
      if (isAdmin) {
        await removeAdminByUserId(user.lineUserId)
      } else {
        await setAdminByUserId(user.lineUserId)
      }
      load()
    } catch (e: any) {
      alert(apiErrorMessage(e, isAdmin ? '移除失敗' : '設定失敗'))
    } finally {
      setBusy(null)
    }
  }

  // Whether a user is a protected developer (cannot toggle)
  function isProtected(lineUserId: string): boolean {
    // If current user is developer, protect developer's own account
    // The backend will also reject, but we disable the button proactively
    return adminUserIds.has(lineUserId) && lineUserId === me?.userId && (me?.developer ?? false)
  }

  return (
    <div className="max-w-lg mx-auto p-4">
      <h1 className="text-lg font-bold text-gray-800 mb-4">管理員帳號</h1>

      {me?.developer && (
        <div className="mb-4 p-3 rounded-lg bg-blue-50 border border-blue-200 text-sm text-blue-700">
          你是開發人員，帳號受保護，無法被移除或修改。
        </div>
      )}

      {loading ? (
        <p className="text-center text-gray-400 py-8">載入中...</p>
      ) : (
        <div className="space-y-2">
          {users.map(user => {
            const isAdmin = adminUserIds.has(user.lineUserId)
            const isSelf = user.lineUserId === me?.userId
            const protected_ = isProtected(user.lineUserId)
            const isBusy = busy === user.lineUserId

            return (
              <div
                key={user.lineUserId}
                className="bg-white rounded-xl border border-gray-100 shadow-sm p-3 flex items-center gap-3"
              >
                {/* Avatar */}
                <div className="w-10 h-10 rounded-full bg-gray-200 overflow-hidden flex-shrink-0">
                  {user.pictureUrl ? (
                    <img src={user.pictureUrl} alt="" className="w-full h-full object-cover" />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-gray-400 text-lg">👤</div>
                  )}
                </div>

                {/* Info */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-1.5 flex-wrap">
                    <p className="font-medium text-gray-800 truncate">{user.displayName}</p>
                    {isAdmin && (
                      <span className="text-xs bg-green-100 text-green-700 rounded-full px-2 py-0.5">管理員</span>
                    )}
                    {protected_ && (
                      <span className="text-xs bg-blue-100 text-blue-700 rounded-full px-2 py-0.5">開發人員</span>
                    )}
                    {isSelf && !protected_ && (
                      <span className="text-xs bg-gray-100 text-gray-500 rounded-full px-2 py-0.5">你</span>
                    )}
                  </div>
                  <p className="text-xs text-gray-400 truncate mt-0.5">{user.lineUserId}</p>
                </div>

                {/* Toggle button */}
                <button
                  onClick={() => handleToggle(user)}
                  disabled={isBusy || protected_ || isSelf}
                  className={`flex-shrink-0 text-sm px-3 py-1.5 rounded-lg border transition-colors disabled:opacity-40 disabled:cursor-not-allowed
                    ${isAdmin
                      ? 'border-red-200 text-red-500 hover:bg-red-50'
                      : 'border-green-200 text-green-600 hover:bg-green-50'
                    }`}
                >
                  {isBusy ? '...' : isAdmin ? '取消管理員' : '設為管理員'}
                </button>
              </div>
            )
          })}

          {users.length === 0 && (
            <p className="text-center text-gray-400 py-6">尚無用戶資料（需先有人互動過 Bot）</p>
          )}
        </div>
      )}
    </div>
  )
}
