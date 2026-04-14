import { BrowserRouter, Routes, Route, NavLink, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import LoadingScreen from './components/LoadingScreen'
import CalendarPage from './pages/CalendarPage'
import MyReservations from './pages/MyReservations'
import SessionRequestPage from './pages/SessionRequestPage'
import SessionManage from './pages/admin/SessionManage'
import TableLayout from './pages/admin/TableLayout'
import BlockDate from './pages/admin/BlockDate'
import PushCenter from './pages/admin/PushCenter'
import AdminManage from './pages/admin/AdminManage'
import SessionRequestAdmin from './pages/admin/SessionRequestAdmin'

function AppRoutes() {
  const { me, loading, error } = useAuth()

  if (loading) return <LoadingScreen />
  if (error) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen gap-2 p-8 text-red-500">
        <p className="text-4xl">⚠️</p>
        <p className="text-center">{error}</p>
      </div>
    )
  }

  const isAdmin = me?.admin ?? false

  return (
    <div className="flex flex-col min-h-screen bg-gray-50">
      <main className="flex-1 pb-16">
        <Routes>
          <Route path="/" element={<Navigate to="/calendar" replace />} />
          <Route path="/calendar" element={<CalendarPage />} />
          <Route path="/my" element={<MyReservations />} />
          {/* 一般用戶場次申請 */}
          {!isAdmin && <Route path="/session-requests" element={<SessionRequestPage />} />}
          {/* 管理員頁面 */}
          {isAdmin && <Route path="/admin/sessions" element={<SessionManage />} />}
          {isAdmin && <Route path="/admin/tables" element={<TableLayout />} />}
          {isAdmin && <Route path="/admin/block-dates" element={<BlockDate />} />}
          {isAdmin && <Route path="/admin/push" element={<PushCenter />} />}
          {isAdmin && <Route path="/admin/admins" element={<AdminManage />} />}
          {isAdmin && <Route path="/admin/session-requests" element={<SessionRequestAdmin />} />}
          <Route path="*" element={<Navigate to="/calendar" replace />} />
        </Routes>
      </main>

      {/* ── 底部導覽列 ── */}
      <nav className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 flex overflow-x-auto">

        {/* 共用：預約月曆 */}
        <NavLink to="/calendar" className={({ isActive }) =>
          `flex-1 flex flex-col items-center py-2 text-xs gap-0.5 min-w-[52px] ${isActive ? 'text-green-600' : 'text-gray-400'}`
        }>
          <span className="text-xl">📅</span>預約
        </NavLink>

        {/* 共用：我的預約 */}
        <NavLink to="/my" className={({ isActive }) =>
          `flex-1 flex flex-col items-center py-2 text-xs gap-0.5 min-w-[52px] ${isActive ? 'text-green-600' : 'text-gray-400'}`
        }>
          <span className="text-xl">📋</span>我的預約
        </NavLink>

        {/* 一般用戶：申請場次（管理員不顯示） */}
        {!isAdmin && (
          <NavLink to="/session-requests" className={({ isActive }) =>
            `flex-1 flex flex-col items-center py-2 text-xs gap-0.5 min-w-[52px] ${isActive ? 'text-green-600' : 'text-gray-400'}`
          }>
            <span className="text-xl">📝</span>申請場次
          </NavLink>
        )}

        {/* 管理員專屬 */}
        {isAdmin && (
          <>
            <NavLink to="/admin/sessions" className={({ isActive }) =>
              `flex-1 flex flex-col items-center py-2 text-xs gap-0.5 min-w-[52px] ${isActive ? 'text-green-600' : 'text-gray-400'}`
            }>
              <span className="text-xl">⚙️</span>場次
            </NavLink>

            <NavLink to="/admin/tables" className={({ isActive }) =>
              `flex-1 flex flex-col items-center py-2 text-xs gap-0.5 min-w-[52px] ${isActive ? 'text-green-600' : 'text-gray-400'}`
            }>
              <span className="text-xl">🀄</span>桌位
            </NavLink>

            <NavLink to="/admin/push" className={({ isActive }) =>
              `flex-1 flex flex-col items-center py-2 text-xs gap-0.5 min-w-[52px] ${isActive ? 'text-green-600' : 'text-gray-400'}`
            }>
              <span className="text-xl">📢</span>推播
            </NavLink>

            {/* 場次申請審核 */}
            <NavLink to="/admin/session-requests" className={({ isActive }) =>
              `flex-1 flex flex-col items-center py-2 text-xs gap-0.5 min-w-[52px] ${isActive ? 'text-green-600' : 'text-gray-400'}`
            }>
              <span className="text-xl">✅</span>申請審核
            </NavLink>

            {/* 成員權限管理 */}
            <NavLink to="/admin/admins" className={({ isActive }) =>
              `flex-1 flex flex-col items-center py-2 text-xs gap-0.5 min-w-[52px] ${isActive ? 'text-green-600' : 'text-gray-400'}`
            }>
              <span className="text-xl">👥</span>成員管理
            </NavLink>
          </>
        )}
      </nav>
    </div>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AuthProvider>
  )
}
