import styles from './Button.module.css'

// variant: 'primary' | 'secondary' | 'danger'. Everything else (type, onClick,
// disabled, ...) passes straight through to the native <button>.
export function Button({ variant = 'primary', className, ...props }) {
  const variantClass = styles[variant] ?? styles.primary
  const classes = [styles.button, variantClass, className].filter(Boolean).join(' ')
  return <button className={classes} {...props} />
}
