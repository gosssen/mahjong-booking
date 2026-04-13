import { logError } from '../../utils/logger'
import { useEffect, useState } from 'react'
import { getAdmins, addAdmin, removeAdmin } from '../../api'

interface Admin {
  id: number
  lineUserId: string
  displayName?: string
  addedBy?: string
}

export default function AdminManage() {
  const [admins, setAdmins] = useState<Admin[]>([])
  const [newUserId, setNewUserId] = useState('')
  const [adding, setAdding] = useState(false)
  const [loading, setLoading] = useState(true)

  function load() {
    setLoading(true)
    getAdmins().then(setAdmins).catch((e) => logError("load failed", e)).finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  async function handleAdd() {
    if (!newUserId.trim()) return
    setAdding(true)
    try {
      await addAdmin(newUserId.trim())
      setNewUserId('')
      load()
    } catch (e: any) {
      alert(e.response?.data?.detail ?? '新增失敗')
    } finally {
      setAdding(false)
    }
  }

  async function handleRemove(admin: Admin) {
    if (!confirm(`確定要移除管理員 ${admin.displayName ?? admin.lineUserId} 嗎？`)) return
    try {
      await removeAdmin(admin.id)
      load()
    } catch (e: any) {
      alert(e.response?.data?.detail ?? '移除失敗')
    }
  }

  return (
    <div className="max-w-lg mx-auto p-4">
      <h1 className="text-lg font-bold text-gray-800 mb-4">管理員帳號</h1>

      <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-4 mb-4">
        <h2 className="font-medium text-gray-700 mb-3">新增管理員</h2>
        <input
          type="text"
          placeholder="LINE userId（例如 Uxxxxxxxxxx）"
          value={newUserId}
          onChange={e => setNewUserId(e.target.value)}
          className="w-full border rounded-lg px-3 py-2 text-sm mb-2 focus:outline-none focus:ring-2 focus:ring-green-400"
        />
        <button
          onClick={handleAdd}
          disabled={!newUserId.trim() || adding}
          className="w-full py-2 rounded-lg bg-green-500 text-white text-sm font-medium hover:bg-green-600 disabled:opacity-50 transition-colors"
        >
          {adding ? '新增中...' : '新增管理員'}
        </button>
      </div>

      <h2 className="font-medium text-gray-700 mb-2">現有管理員</h2>
      {loading ? (
        <p className="text-center text-gray-400 py-4">載入中...</p>
      ) : (
        admins.map(a => (
          <div key={a.id} className="bg-white rounded-xl border border-gray-100 shadow-sm p-4 mb-2 flex items-center justify-between">
            <div>
              <p className="font-medium text-gray-800">{a.displayName ?? a.lineUserId}</p>
              <p className="text-xs text-gray-400 mt-0.5">{a.lineUserId}</p>
            </div>
            <button
              onClick={() => handleRemove(a)}
              className="text-sm text-red-400 hover:text-red-600"
            >
              移除
            </button>
          </div>
        ))
      )}
    </div>
  )
}
