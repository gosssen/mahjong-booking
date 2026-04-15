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
      {/* 底部 nav 高度：一般用戶 1 排 56px (pb-14)，管理員 2 排 112px (pb-28) */}
      <main className={`flex-1 ${isAdmin ? 'pb-28' : 'pb-14'}`}>
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
      <nav className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200">

        {isAdmin ? (
          /* 管理員：2 排 */
          <>
            {/* 排 1：常用功能 */}
            <div className="flex border-b border-gray-100">
              {[
                { to: '/calendar',        icon: '📅', label: '預約' },
                { to: '/my',             icon: '📋', label: '我的預約' },
                { to: '/admin/sessions', icon: '⚙️', label: '場次' },
                { to: '/admin/tables',   icon: '🀄', label: '桌位' },
              ].map(({ to, icon, label }) => (
                <NavLink key={to} to={to} className={({ isActive }) =>
                  `flex-1 flex flex-col items-center py-2 text-xs gap-0.5 ${isActive ? 'text-green-600' : 'text-gray-400'}`
                }>
                  <span className="text-xl">{icon}</span>{label}
                </NavLink>
              ))}
            </div>
            {/* 排 2：管理功能 */}
            <div className="flex">
              {[
                { to: '/admin/push',             icon: '📢', label: '推播' },
                { to: '/admin/session-requests', icon: '✅', label: '申請審核' },
                { to: '/admin/admins',           icon: '👥', label: '成員管理' },
              ].map(({ to, icon, label }) => (
                <NavLink key={to} to={to} className={({ isActive }) =>
                  `flex-1 flex flex-col items-center py-2 text-xs gap-0.5 ${isActive ? 'text-green-600' : 'text-gray-400'}`
                }>
                  <span className="text-xl">{icon}</span>{label}
                </NavLink>
              ))}
            </div>
          </>
        ) : (
          /* 一般用戶：1 排 3 項 */
          <div className="flex">
            {[
              { to: '/calendar',          icon: '📅', label: '預約' },
              { to: '/my',               icon: '📋', label: '我的預約' },
              { to: '/session-requests', icon: '📝', label: '申請場次' },
            ].map(({ to, icon, label }) => (
              <NavLink key={to} to={to} className={({ isActive }) =>
                `flex-1 flex flex-col items-center py-2 text-xs gap-0.5 ${isActive ? 'text-green-600' : 'text-gray-400'}`
              }>
                <span className="text-xl">{icon}</span>{label}
              </NavLink>
            ))}
          </div>
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
