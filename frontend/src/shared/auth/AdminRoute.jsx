import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from './useAuth.js'
import { Role } from '../constants/roles.js'

// Nested inside ProtectedRoute (Step 10) in routes.jsx, so isAuthenticated and
// `user` are already resolved by the time this renders. Redirects non-admins
// to /projects rather than a 403 page, since none exists - matches the
// backend's own behavior of just not exposing these three endpoints to a
// non-admin, no dedicated error state.
export function AdminRoute() {
  const { user } = useAuth()

  if (user?.role !== Role.ADMIN) {
    return <Navigate to="/projects" replace />
  }

  return <Outlet />
}
