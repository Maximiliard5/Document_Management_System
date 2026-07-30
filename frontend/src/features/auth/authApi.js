import { httpClient } from '../../shared/api/httpClient.js'

// Both endpoints return AuthResponse: { token, email, firstName, lastName, role }.
// Note there's no numeric id here - only GET /users/me includes it (see
// shared/auth/AuthContext.jsx, which fetches that separately after login/register).

export function login({ email, password }) {
  return httpClient.post('/auth/login', { email, password })
}

export function register({ email, password, firstName, lastName }) {
  return httpClient.post('/auth/register', { email, password, firstName, lastName })
}
