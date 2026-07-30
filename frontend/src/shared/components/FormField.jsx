import { forwardRef } from 'react'
import styles from './FormField.module.css'

// forwardRef is required here (not just style) - react-hook-form's register()
// returns a ref that must land on the actual DOM element, not on this wrapper.
// `as` switches the rendered tag ('input' | 'textarea' | 'select') so callers
// get the same label/error layout for a <select> or <textarea> - pass <option>s
// as children when as="select".
export const FormField = forwardRef(function FormField(
  { label, error, as = 'input', type = 'text', children, ...fieldProps },
  ref,
) {
  const Tag = as
  return (
    <label className={styles.field}>
      <span className={styles.label}>{label}</span>
      <Tag
        ref={ref}
        {...(as === 'input' ? { type } : {})}
        className={styles.input}
        aria-invalid={error ? 'true' : undefined}
        {...fieldProps}
      >
        {children}
      </Tag>
      {error && (
        <span className={styles.error} role="alert">
          {error}
        </span>
      )}
    </label>
  )
})
