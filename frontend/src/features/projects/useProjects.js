import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import * as projectsApi from './projectsApi.js'

// Key factory (standard TanStack Query pattern) - every query/invalidation
// call site builds its key from here rather than typing out array literals,
// so a mismatched key can't silently break cache invalidation.
//
// detail() coerces to String: query keys are matched by structural equality
// *including type*, and projectId shows up as a string from useParams() (URL
// params are always strings) but as a number from project.id (a JSON Long).
// Without this coercion, a mutation invalidating with the numeric id (e.g.
// MembersSection, which reads project.id) silently fails to match the query
// registered with the string id (ProjectDetailLayout, via useParams) - no
// error anywhere, the UI just never updates until a full reload.
export const projectKeys = {
  all: ['projects'],
  list: () => [...projectKeys.all, 'list'],
  detail: (projectId) => [...projectKeys.all, 'detail', String(projectId)],
}

export function useProjects() {
  return useQuery({
    queryKey: projectKeys.list(),
    queryFn: projectsApi.listProjects,
  })
}

export function useProject(projectId) {
  return useQuery({
    queryKey: projectKeys.detail(projectId),
    queryFn: () => projectsApi.getProject(projectId),
    enabled: Boolean(projectId),
  })
}

export function useCreateProject() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: projectsApi.createProject,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: projectKeys.list() })
    },
  })
}

export function useUpdateProject(projectId) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data) => projectsApi.updateProject(projectId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: projectKeys.detail(projectId) })
      queryClient.invalidateQueries({ queryKey: projectKeys.list() })
    },
  })
}

export function useDeleteProject() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: projectsApi.deleteProject,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: projectKeys.list() })
    },
  })
}

export function useAddMember(projectId) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (userId) => projectsApi.addMember(projectId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: projectKeys.detail(projectId) })
    },
  })
}

export function useRemoveMember(projectId) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (userId) => projectsApi.removeMember(projectId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: projectKeys.detail(projectId) })
    },
  })
}
