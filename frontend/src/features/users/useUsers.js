import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import * as usersApi from './usersApi.js'

export function useUserSearch(query) {
  return useQuery({
    queryKey: ['users', 'search', query],
    queryFn: () => usersApi.searchUsers(query),
    // Mirrors the backend's own guard - no point hitting the API for a query
    // it'll just reject with an empty array anyway.
    enabled: query.trim().length >= 2,
  })
}

// Admin user list - a single global key, unlike projects/tasks/documents,
// since GET /users has no project scope to key by.
const userListKey = ['users', 'list']

export function useUsers() {
  return useQuery({ queryKey: userListKey, queryFn: usersApi.listUsers })
}

export function useUpdateUserRole() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ userId, role }) => usersApi.updateUserRole(userId, role),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: userListKey })
    },
  })
}

export function useDeactivateUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (userId) => usersApi.deactivateUser(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: userListKey })
    },
  })
}
