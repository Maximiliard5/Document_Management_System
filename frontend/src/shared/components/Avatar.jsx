import styles from './Avatar.module.css'

function getInitials(firstName, lastName) {
  const initials = (firstName?.[0] ?? '') + (lastName?.[0] ?? '')
  return initials ? initials.toUpperCase() : '?'
}

export function Avatar({ firstName, lastName, size = 32 }) {
  const fullName = [firstName, lastName].filter(Boolean).join(' ')
  return (
    <span
      className={styles.avatar}
      style={{ width: size, height: size, fontSize: Math.round(size * 0.4) }}
      title={fullName || undefined}
    >
      {getInitials(firstName, lastName)}
    </span>
  )
}
