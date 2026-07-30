import { useEffect, useId, useRef } from 'react'
import { createPortal } from 'react-dom'
import styles from './Modal.module.css'

// Rendered via a portal to document.body so it's never clipped by an
// ancestor's overflow/stacking context, regardless of how deep in the tree
// it's opened from.
export function Modal({ isOpen, onClose, title, children, size = 'default' }) {
  const panelRef = useRef(null)
  const titleId = useId()

  // Deliberately separate from the listener effect below and depends only on
  // `isOpen` - focusing the panel has to happen once, on the open transition,
  // not every time `onClose` gets a new identity (e.g. a parent with state
  // that changes per keystroke re-creating an inline handler each render).
  // Doing this in one combined effect keyed on [isOpen, onClose] stole focus
  // away from whatever was focused inside the modal - such as a live-search
  // input - on every keystroke, which is what actually surfaced this.
  useEffect(() => {
    if (isOpen) {
      panelRef.current?.focus()
    }
  }, [isOpen])

  useEffect(() => {
    if (!isOpen) {
      return
    }
    function handleKeyDown(event) {
      if (event.key === 'Escape') {
        onClose()
      }
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, onClose])

  if (!isOpen) {
    return null
  }

  return createPortal(
    <div className={styles.backdrop} onClick={onClose}>
      <div
        ref={panelRef}
        className={size === 'large' ? `${styles.panel} ${styles.panelLarge}` : styles.panel}
        role="dialog"
        aria-modal="true"
        aria-labelledby={title ? titleId : undefined}
        tabIndex={-1}
        onClick={(event) => event.stopPropagation()}
      >
        {title && (
          <h2 id={titleId} className={styles.title}>
            {title}
          </h2>
        )}
        {children}
      </div>
    </div>,
    document.body,
  )
}
