// Thin fetch wrapper shared by every feature's api.js. Handles the three things
// every call site would otherwise repeat: attaching the JWT, parsing the backend's
// error shape (see backend/ARCHITECTURE.md section 7) into something throwable, and
// reacting to a 401 by clearing auth state.
//
// getToken/onUnauthorized are wired up by AuthContext at app startup via
// configureHttpClient - kept as plain module-level hooks (rather than importing
// AuthContext here) so this module has no dependency on the auth feature.

let getToken = () => null
let onUnauthorized = () => {}

export function configureHttpClient({ getToken: getTokenFn, onUnauthorized: onUnauthorizedFn }) {
  if (getTokenFn) getToken = getTokenFn
  if (onUnauthorizedFn) onUnauthorized = onUnauthorizedFn
}

export class ApiError extends Error {
  constructor(status, message, details) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }
}

async function request(path, { method = 'GET', body, headers, signal } = {}) {
  const token = getToken()
  const requestHeaders = { ...headers }
  // FormData (document upload) must NOT get a Content-Type here - fetch sets
  // its own, with the multipart boundary, only when it's left unset.
  const isFormData = body instanceof FormData
  if (body !== undefined && !isFormData) {
    requestHeaders['Content-Type'] = 'application/json'
  }
  if (token) {
    requestHeaders.Authorization = `Bearer ${token}`
  }

  const response = await fetch(path, {
    method,
    headers: requestHeaders,
    body: body === undefined ? undefined : isFormData ? body : JSON.stringify(body),
    signal,
  })

  if (response.status === 401) {
    // Also fires on a failed login/register attempt (wrong password, disabled
    // account) - harmless there, since there's no session yet to clear.
    onUnauthorized()
  }

  const payload = response.status === 204 ? null : await response.json().catch(() => null)

  if (!response.ok) {
    throw new ApiError(
      payload?.status ?? response.status,
      payload?.message ?? 'An unexpected error occurred',
      payload?.details,
    )
  }

  return payload
}

// Document download returns a binary stream, not JSON - a separate path that
// resolves to a Blob instead of parsing the body as JSON.
async function requestBlob(path, { signal } = {}) {
  const token = getToken()
  const headers = token ? { Authorization: `Bearer ${token}` } : {}

  const response = await fetch(path, { headers, signal })

  if (response.status === 401) {
    onUnauthorized()
  }

  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    throw new ApiError(
      payload?.status ?? response.status,
      payload?.message ?? 'An unexpected error occurred',
      payload?.details,
    )
  }

  return response.blob()
}

export const httpClient = {
  get: (path, options) => request(path, { ...options, method: 'GET' }),
  post: (path, body, options) => request(path, { ...options, method: 'POST', body }),
  put: (path, body, options) => request(path, { ...options, method: 'PUT', body }),
  delete: (path, options) => request(path, { ...options, method: 'DELETE' }),
  getBlob: (path, options) => requestBlob(path, options),
}
