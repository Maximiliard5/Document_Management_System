import { useRef, useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import { PageHeader } from '../../shared/components/PageHeader.jsx'
import { Button } from '../../shared/components/Button.jsx'
import { Badge } from '../../shared/components/Badge.jsx'
import { Spinner } from '../../shared/components/Spinner.jsx'
import { EmptyState } from '../../shared/components/EmptyState.jsx'
import { ConfirmDialog } from '../../shared/components/ConfirmDialog.jsx'
import { ApiError } from '../../shared/api/httpClient.js'
import { formatFileSize } from '../../shared/utils/formatFileSize.js'
import { formatDate } from '../../shared/utils/formatDate.js'
import { useDocuments, useUploadDocument, useDeleteDocument } from './useDocuments.js'
import { downloadDocument } from './documentsApi.js'
import { DocumentPreviewModal } from './components/DocumentPreviewModal.jsx'
import { PREVIEWABLE_MIME_TYPES } from './previewableMimeTypes.js'
import styles from './DocumentsPage.module.css'

// Mirrors DocumentService.ALLOWED_MIME_TYPES (backend/ARCHITECTURE.md section 8) -
// anything else falls back to a generic label rather than failing to render.
const FILE_TYPE_LABEL = {
  'application/pdf': 'PDF',
  'image/jpeg': 'JPG',
  'image/png': 'PNG',
  'image/gif': 'GIF',
  'image/webp': 'WEBP',
  'text/plain': 'TXT',
  'text/csv': 'CSV',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'DOCX',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': 'XLSX',
  'application/vnd.openxmlformats-officedocument.presentationml.presentation': 'PPTX',
  'application/msword': 'DOC',
  'application/vnd.ms-excel': 'XLS',
  'application/vnd.ms-powerpoint': 'PPT',
}

function triggerBrowserDownload(blob, filename) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

export function DocumentsPage() {
  const { project } = useOutletContext()
  const { data: documents, isLoading, isError, error } = useDocuments(project.id)
  const uploadDocument = useUploadDocument(project.id)
  const deleteDocument = useDeleteDocument(project.id)

  const fileInputRef = useRef(null)
  const [actionError, setActionError] = useState(null)
  const [downloadingId, setDownloadingId] = useState(null)
  const [documentPendingDeletion, setDocumentPendingDeletion] = useState(null)
  const [previewDocument, setPreviewDocument] = useState(null)

  // DocumentResponse.ownerId is a bare Long, not a nested UserResponse (see
  // backend/ARCHITECTURE.md section 10) - resolve it against the project's
  // own owner/members, already loaded by ProjectDetailLayout, instead of a
  // separate lookup request.
  const uploaderById = Object.fromEntries(
    [project.owner, ...project.members].map((member) => [member.id, member]),
  )

  async function handleFileSelected(event) {
    const file = event.target.files[0]
    event.target.value = ''
    if (!file) return
    setActionError(null)
    try {
      await uploadDocument.mutateAsync(file)
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'Something went wrong. Try again.')
    }
  }

  // PDFs open in a new tab instead of the preview modal - the browser's own
  // full-tab PDF viewer (zoom, page nav, search, printing) is far better than
  // boxing an iframe into a modal-sized panel. Images/text still use the modal.
  async function handlePreview(doc) {
    if (doc.type !== 'application/pdf') {
      setPreviewDocument(doc)
      return
    }

    setActionError(null)
    // window.open must happen synchronously in the click handler or browsers
    // block it as a popup - opening a blank tab now and pointing it at the
    // object URL once the download resolves preserves the user-gesture
    // context that a fetch-then-open sequence would lose.
    const previewTab = window.open('', '_blank')
    if (!previewTab) {
      setActionError('Your browser blocked the preview tab. Allow pop-ups for this site and try again.')
      return
    }

    try {
      const blob = await downloadDocument(project.id, doc.id)
      const url = URL.createObjectURL(blob)
      previewTab.location.href = url
      // Give the new tab enough time to load the PDF into memory before the
      // object URL is revoked - it isn't tied to this component's lifecycle
      // like the modal's preview URLs are, since the tab stays open on its own.
      setTimeout(() => URL.revokeObjectURL(url), 60_000)
    } catch (err) {
      previewTab.close()
      setActionError(err instanceof ApiError ? err.message : 'Could not open this file.')
    }
  }

  async function handleDownload(doc) {
    setActionError(null)
    setDownloadingId(doc.id)
    try {
      const blob = await downloadDocument(project.id, doc.id)
      triggerBrowserDownload(blob, doc.name)
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'Could not download this file.')
    } finally {
      setDownloadingId(null)
    }
  }

  async function handleConfirmDelete() {
    if (!documentPendingDeletion) return
    try {
      await deleteDocument.mutateAsync(documentPendingDeletion.id)
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'Could not delete this file.')
    } finally {
      setDocumentPendingDeletion(null)
    }
  }

  return (
    <div>
      <PageHeader
        title="Documents"
        description="Files shared with everyone on this project."
        actions={
          <>
            <input
              ref={fileInputRef}
              type="file"
              className={styles.hiddenInput}
              onChange={handleFileSelected}
            />
            <Button onClick={() => fileInputRef.current?.click()} disabled={uploadDocument.isPending}>
              {uploadDocument.isPending ? 'Uploading...' : 'Upload document'}
            </Button>
          </>
        }
      />

      {actionError && (
        <p role="alert" className={styles.error}>
          {actionError}
        </p>
      )}

      {isLoading && <Spinner label="Loading documents" />}

      {isError && (
        <p role="alert" className={styles.error}>
          {error.message}
        </p>
      )}

      {!isLoading && !isError && documents.length === 0 && (
        <EmptyState
          title="No documents yet"
          description="Upload a file to share it with everyone on this project."
          action={<Button onClick={() => fileInputRef.current?.click()}>Upload document</Button>}
        />
      )}

      {!isLoading && !isError && documents.length > 0 && (
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Type</th>
              <th>Name</th>
              <th>Size</th>
              <th>Uploaded by</th>
              <th>Uploaded</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {documents.map((doc) => {
              const uploader = uploaderById[doc.ownerId]
              return (
                <tr key={doc.id}>
                  <td>
                    <Badge>{FILE_TYPE_LABEL[doc.type] ?? 'FILE'}</Badge>
                  </td>
                  <td>{doc.name}</td>
                  <td className={styles.mono}>{formatFileSize(doc.size)}</td>
                  <td>{uploader ? `${uploader.firstName} ${uploader.lastName}` : 'Unknown'}</td>
                  <td className={styles.mono}>{formatDate(doc.createdAt)}</td>
                  <td className={styles.actions}>
                    {PREVIEWABLE_MIME_TYPES.has(doc.type) && (
                      <Button variant="secondary" onClick={() => handlePreview(doc)}>
                        Preview
                      </Button>
                    )}
                    <Button
                      variant="secondary"
                      onClick={() => handleDownload(doc)}
                      disabled={downloadingId === doc.id}
                    >
                      {downloadingId === doc.id ? 'Downloading...' : 'Download'}
                    </Button>
                    <Button variant="danger" onClick={() => setDocumentPendingDeletion(doc)}>
                      Delete
                    </Button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      )}

      <ConfirmDialog
        isOpen={Boolean(documentPendingDeletion)}
        title="Delete document?"
        message={
          documentPendingDeletion
            ? `"${documentPendingDeletion.name}" will be permanently deleted.`
            : ''
        }
        confirmLabel="Delete"
        isDangerous
        onConfirm={handleConfirmDelete}
        onCancel={() => setDocumentPendingDeletion(null)}
      />

      <DocumentPreviewModal
        key={previewDocument?.id ?? 'closed'}
        isOpen={Boolean(previewDocument)}
        onClose={() => setPreviewDocument(null)}
        projectId={project.id}
        document={previewDocument}
      />
    </div>
  )
}
