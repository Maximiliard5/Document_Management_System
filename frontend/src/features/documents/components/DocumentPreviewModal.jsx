import { useEffect, useState } from 'react'
import { Modal } from '../../../shared/components/Modal.jsx'
import { Spinner } from '../../../shared/components/Spinner.jsx'
import { downloadDocument } from '../documentsApi.js'
import styles from './DocumentPreviewModal.module.css'

// Handles images and text/csv only - PDFs are previewable too, but
// DocumentsPage opens those in a new tab instead (see its handlePreview),
// since the browser's own full-tab PDF viewer beats boxing an iframe into a
// modal-sized panel.
//
// The parent must remount this component per document (`key={doc?.id}`) -
// the initial "loading" state is derived once at mount via the lazy
// useState initializer below, rather than reset by calling setState
// synchronously inside the effect, which would cause an extra render pass.
export function DocumentPreviewModal({ isOpen, onClose, projectId, document: doc }) {
  const [state, setState] = useState(() => (doc ? { status: 'loading' } : { status: 'idle' }))

  useEffect(() => {
    if (!isOpen || !doc) {
      return
    }

    let cancelled = false
    let objectUrl = null

    downloadDocument(projectId, doc.id)
      .then(async (blob) => {
        if (doc.type === 'text/plain' || doc.type === 'text/csv') {
          const text = await blob.text()
          if (cancelled) return
          setState({ status: 'text', text })
          return
        }
        if (cancelled) return
        objectUrl = URL.createObjectURL(blob)
        setState({ status: 'image', url: objectUrl })
      })
      .catch(() => {
        if (!cancelled) setState({ status: 'error' })
      })

    return () => {
      cancelled = true
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [isOpen, projectId, doc])

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={doc?.name} size="large">
      <div className={styles.body}>
        {state.status === 'loading' && <Spinner label="Loading preview" />}
        {state.status === 'error' && (
          <p role="alert" className={styles.error}>
            Could not load a preview for this file.
          </p>
        )}
        {state.status === 'text' && <pre className={styles.text}>{state.text}</pre>}
        {state.status === 'image' && <img src={state.url} alt={doc.name} className={styles.image} />}
      </div>
    </Modal>
  )
}
