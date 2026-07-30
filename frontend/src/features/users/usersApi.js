import { httpClient } from '../../shared/api/httpClient.js'

// Returns UserSearchResponse[]: {id, email, firstName, lastName}. Backend
// itself returns [] for queries shorter than 2 characters and caps at 10
// results (see backend/ARCHITECTURE.md).
export function searchUsers(email) {
  return httpClient.get(`/users/search?email=${encodeURIComponent(email)}`)
}

// The following three are ADMIN-only on the backend (@PreAuthorize) - only
// ever called from UsersAdminPage, which is itself gated by AdminRoute.
export function listUsers() {
  return httpClient.get('/users')
}

export function updateUserRole(userId, role) {
  return httpClient.put(`/users/${userId}/role?role=${encodeURIComponent(role)}`)
}

export function deactivateUser(userId) {
  return httpClient.put(`/users/${userId}/deactivate`)
}
