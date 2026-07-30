import { useEffect } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../../shared/auth/useAuth.js'

const STORAGE_PREFIX = 'dms.recentProjects'
const MAX_RECENT = 5

// A pre-scoping version of this feature stored everything under one shared
// key, so on a shared machine any logged-in user could see (and click
// straight into) every other user's recently-visited projects - the backend
// still rejected the actual project fetch, but project names/existence had
// already leaked into the sidebar. Nothing in that old key is safe to keep
// for any one user, so it's just dropped rather than migrated.
try {
  localStorage.removeItem(STORAGE_PREFIX)
} catch {
  // ignore
}

function storageKey(userId) {
  return `${STORAGE_PREFIX}.${userId}`
}

// Keyed on userId so switching accounts - even in the same browser tab,
// without a full page reload - can't show one user's history under another's
// session: it's a different TanStack Query cache entry, not a mutation of a
// shared one.
export const recentProjectsKeys = {
  forUser: (userId) => ['recentProjects', userId],
}

function readFromLocalStorage(userId) {
  try {
    const raw = localStorage.getItem(storageKey(userId))
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function writeToLocalStorage(userId, list) {
  localStorage.setItem(storageKey(userId), JSON.stringify(list))
}

// No backend last-accessed tracking exists, so recency is tracked entirely
// client-side. Backed by TanStack Query (the app's one state tool) rather
// than Context or a pub-sub, so the sidebar updates the moment a visit is
// recorded without prop drilling.
export function useRecentProjects() {
  const { user } = useAuth()

  return useQuery({
    queryKey: recentProjectsKeys.forUser(user?.id),
    queryFn: () => readFromLocalStorage(user.id),
    initialData: () => (user ? readFromLocalStorage(user.id) : []),
    enabled: Boolean(user),
    staleTime: Infinity,
  })
}

// Call from a project detail page - writes the most-recently-visited
// project to localStorage, most-recent-first, deduped by id, capped at 5.
export function useRecordProjectVisit(project) {
  const { user } = useAuth()
  const queryClient = useQueryClient()

  useEffect(() => {
    if (!project?.id || !user?.id) return

    const current = readFromLocalStorage(user.id)
    const next = [
      { id: project.id, name: project.name },
      ...current.filter((entry) => entry.id !== project.id),
    ].slice(0, MAX_RECENT)

    writeToLocalStorage(user.id, next)
    queryClient.setQueryData(recentProjectsKeys.forUser(user.id), next)
  }, [project?.id, project?.name, user?.id, queryClient])
}
