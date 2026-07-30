import { httpClient } from '../../shared/api/httpClient.js'

export function listTasks(projectId, { status, priority } = {}) {
  const params = new URLSearchParams()
  if (status) params.set('status', status)
  if (priority) params.set('priority', priority)
  const query = params.toString()
  return httpClient.get(`/projects/${projectId}/tasks${query ? `?${query}` : ''}`)
}

export function createTask(projectId, { title, description, priority, deadline, assigneeId }) {
  return httpClient.post(`/projects/${projectId}/tasks`, { title, description, priority, deadline, assigneeId })
}

export function updateTask(projectId, taskId, { title, description, priority, status, deadline, assigneeId }) {
  return httpClient.put(`/projects/${projectId}/tasks/${taskId}`, {
    title,
    description,
    priority,
    status,
    deadline,
    assigneeId,
  })
}

export function deleteTask(projectId, taskId) {
  return httpClient.delete(`/projects/${projectId}/tasks/${taskId}`)
}
