import { httpClient } from '../../shared/api/httpClient.js'

export function listDocuments(projectId) {
  return httpClient.get(`/projects/${projectId}/documents`)
}

export function uploadDocument(projectId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return httpClient.post(`/projects/${projectId}/documents`, formData)
}

// Returns a Blob, not a DocumentResponse - the caller already has the display
// name from the list (DocumentResponse.name), so there's no need to parse the
// backend's unescaped Content-Disposition header (see backend/ARCHITECTURE.md
// section 10) to get it.
export function downloadDocument(projectId, documentId) {
  return httpClient.getBlob(`/projects/${projectId}/documents/${documentId}/download`)
}

export function deleteDocument(projectId, documentId) {
  return httpClient.delete(`/projects/${projectId}/documents/${documentId}`)
}
