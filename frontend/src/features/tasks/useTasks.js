import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import * as tasksApi from './tasksApi.js'

// Same key-factory pattern as projects/useProjects.js - projectId coerced to
// String() for the same reason documented there (useParams() gives a string,
// callers reading it off a fetched object give a number; TanStack Query keys
// match by structural equality including type).
//
// There's no single-task GET endpoint on the backend (see backend/ARCHITECTURE.md
// section 10), so every mutation invalidates the whole per-project list rather
// than a per-task detail key - there's no detail query to invalidate.
export const taskKeys = {
  all: ['tasks'],
  list: (projectId, filters = {}) => [...taskKeys.all, 'list', String(projectId), filters],
}

export function useTasks(projectId, filters = {}) {
  return useQuery({
    queryKey: taskKeys.list(projectId, filters),
    queryFn: () => tasksApi.listTasks(projectId, filters),
    enabled: Boolean(projectId),
  })
}

export function useCreateTask(projectId) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data) => tasksApi.createTask(projectId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [...taskKeys.all, 'list', String(projectId)] })
    },
  })
}

export function useUpdateTask(projectId) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ taskId, ...data }) => tasksApi.updateTask(projectId, taskId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [...taskKeys.all, 'list', String(projectId)] })
    },
  })
}

// Writes an optimistic task field change straight into the cache, synchronously.
// Deliberately *not* done via useMutation's onMutate: TanStack Query always defers
// onMutate by at least a microtask, so a caller that also clears drag-overlay state
// in the same event handler (e.g. TaskBoardPage's handleDragEnd) would render one
// frame with the overlay gone but the task list still showing the old column - a
// visible snap-back before the optimistic write lands. Calling this directly,
// synchronously, in the same handler as the other state update lets React batch
// both into a single render. Returns a snapshot for rollback via rollbackTaskLists.
export function optimisticallyUpdateTask(queryClient, projectId, taskId, data) {
  const listKeyPrefix = [...taskKeys.all, 'list', String(projectId)]
  const previousLists = queryClient.getQueriesData({ queryKey: listKeyPrefix })

  queryClient.setQueriesData({ queryKey: listKeyPrefix }, (tasks) =>
    tasks?.map((task) => (task.id === taskId ? { ...task, ...data } : task)),
  )

  return previousLists
}

export function rollbackTaskLists(queryClient, previousLists) {
  previousLists?.forEach(([queryKey, tasks]) => {
    queryClient.setQueryData(queryKey, tasks)
  })
}

export function useDeleteTask(projectId) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (taskId) => tasksApi.deleteTask(projectId, taskId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [...taskKeys.all, 'list', String(projectId)] })
    },
  })
}
