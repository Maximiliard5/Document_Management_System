import styles from './EmptyState.module.css'

export function EmptyState({ title, description, action }) {
  return (
    <div className={styles.emptyState}>
      <p className={styles.title}>{title}</p>
      {description && <p className={styles.description}>{description}</p>}
      {action && <div className={styles.action}>{action}</div>}
    </div>
  )
}
