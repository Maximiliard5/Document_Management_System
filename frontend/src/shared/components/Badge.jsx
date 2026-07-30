import styles from './Badge.module.css'

// Deliberately domain-agnostic: takes a raw CSS color (typically one of the
// priority-*/status-* tokens from shared/styles/tokens.css) rather than
// knowing about TaskStatus/TaskPriority itself - the caller decides which
// token applies. With no color given, falls back to a neutral gray via CSS.
export function Badge({ color, children }) {
  const style = color
    ? { color, backgroundColor: `color-mix(in srgb, ${color} 15%, white)` }
    : undefined
  return (
    <span className={styles.badge} style={style}>
      {children}
    </span>
  )
}
