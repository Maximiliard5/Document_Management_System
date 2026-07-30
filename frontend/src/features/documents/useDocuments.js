import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import * as documentsApi from './documentsApi.js'

// Same key-factory pattern as projects/tasks - projectId coerced to String()
// for the reason documented in projects/useProjects.js. No detail key: like
// tasks, there's no single-document GET endpoint (see backend/ARCHITECTURE.md
// section 10), only the per-project list.
export const documentKeys = {
  all: ['documents'],
  list: (projectId) => [...documentKeys.all, 'list', String(projectId)],
}

export function useDocuments(projectId) {
  return useQuery({
    queryKey: documentKeys.list(projectId),
    queryFn: () => documentsApi.listDocuments(projectId),
    enabled: Boolean(projectId),
  })
}

export function useUploadDocument(projectId) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (file) => documentsApi.uploadDocument(projectId, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: documentKeys.list(projectId) })
    },
  })
}

export function useDeleteDocument(projectId) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (documentId) => documentsApi.deleteDocument(projectId, documentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: documentKeys.list(projectId) })
    },
  })
}
