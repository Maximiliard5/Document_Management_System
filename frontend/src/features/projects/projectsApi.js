import { httpClient } from '../../shared/api/httpClient.js'

export function listProjects() {
  return httpClient.get('/projects')
}

export function getProject(projectId) {
  return httpClient.get(`/projects/${projectId}`)
}

export function createProject({ name, description }) {
  return httpClient.post('/projects', { name, description })
}

export function updateProject(projectId, { name, description }) {
  return httpClient.put(`/projects/${projectId}`, { name, description })
}

export function deleteProject(projectId) {
  return httpClient.delete(`/projects/${projectId}`)
}

export function addMember(projectId, userId) {
  return httpClient.post(`/projects/${projectId}/members/${userId}`)
}

export function removeMember(projectId, userId) {
  return httpClient.delete(`/projects/${projectId}/members/${userId}`)
}
