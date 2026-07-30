import { useCallback, useEffect, useRef, useState } from 'react'
import { configureHttpClient, httpClient } from '../api/httpClient.js'
import { login as loginRequest, register as registerRequest } from '../../features/auth/authApi.js'
import { AuthContext } from './context.js'

const TOKEN_STORAGE_KEY = 'dms_token'

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_STORAGE_KEY))
  const [user, setUser] = useState(null)
  // 'loading' only while a persisted token is being validated against the API;
  // starts at 'unauthenticated' directly when there's nothing to check.
  const [status, setStatus] = useState(() => (localStorage.getItem(TOKEN_STORAGE_KEY) ? 'loading' : 'unauthenticated'))

  // getToken/onUnauthorized (registered with httpClient below) must never read a
  // stale value, regardless of when the effect that registered them last ran -
  // a ref sidesteps that entirely.
  const tokenRef = useRef(token)
  useEffect(() => {
    tokenRef.current = token
  }, [token])

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_STORAGE_KEY)
    setToken(null)
    setUser(null)
    setStatus('unauthenticated')
  }, [])

  useEffect(() => {
    configureHttpClient({
      getToken: () => tokenRef.current,
      onUnauthorized: () => logout(),
    })
  }, [logout])

  // Single place that turns "we have a token" into "we have a profile": runs on
  // first load (persisted token) and again whenever login/register change it -
  // both set status to 'loading' themselves (below) before touching token, so
  // this effect never needs to set 'loading' synchronously itself; it only
  // ever needs to resolve to 'authenticated' or 'unauthenticated'.
  // AuthResponse from login/register doesn't include the numeric id we need
  // for ownership checks later (e.g. "am I this project's owner") - only
  // GET /users/me does - so this fetch is required, not just a nicety.
  useEffect(() => {
    if (!token) {
      return
    }
    let cancelled = false
    httpClient
      .get('/users/me')
      .then((profile) => {
        if (!cancelled) {
          setUser(profile)
          setStatus('authenticated')
        }
      })
      .catch(() => {
        // Invalid/expired token, or account deactivated since last login.
        // onUnauthorized (registered above) already cleared it on a 401; if it
        // failed for some other reason, still don't leave status stuck on
        // 'loading' forever.
        if (!cancelled) {
          setStatus('unauthenticated')
        }
      })
    return () => {
      cancelled = true
    }
  }, [token])

  const login = useCallback(async (email, password) => {
    const auth = await loginRequest({ email, password })
    localStorage.setItem(TOKEN_STORAGE_KEY, auth.token)
    setToken(auth.token)
    setStatus('loading')
  }, [])

  const register = useCallback(async (payload) => {
    const auth = await registerRequest(payload)
    localStorage.setItem(TOKEN_STORAGE_KEY, auth.token)
    setToken(auth.token)
    setStatus('loading')
  }, [])

  const value = {
    user,
    isAuthenticated: status === 'authenticated',
    isLoading: status === 'loading',
    login,
    register,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
