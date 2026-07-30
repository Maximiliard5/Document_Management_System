import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './useAuth.js'

// Layout-route guard: wraps protected routes in app/routes.jsx (Step 10) and
// renders their content via <Outlet/> once authenticated, or bounces to /login
// otherwise. Renders nothing while a persisted token is still being validated -
// swap for a real Spinner once shared/components exists (Step 11), rather than
// importing it ahead of when it's built.
export function ProtectedRoute() {
  const { isAuthenticated, isLoading } = useAuth()
  const location = useLocation()

  if (isLoading) {
    return null
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return <Outlet />
}
