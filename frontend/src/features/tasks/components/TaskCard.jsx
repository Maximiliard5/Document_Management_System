import { TaskPriority } from '../../../shared/constants/taskPriority.js'
import { Badge } from '../../../shared/components/Badge.jsx'
import { Avatar } from '../../../shared/components/Avatar.jsx'
import { getDeadlineCountdown } from '../../../shared/utils/deadlineCountdown.js'
import styles from './TaskCard.module.css'

const PRIORITY_COLOR_VAR = {
  [TaskPriority.LOW]: 'var(--priority-low)',
  [TaskPriority.MEDIUM]: 'var(--priority-medium)',
  [TaskPriority.HIGH]: 'var(--priority-high)',
}

function DeadlineBadge({ deadline }) {
  const countdown = getDeadlineCountdown(deadline)
  if (!countdown) return null

  const { days, hours, isOverdue } = countdown
  const label = isOverdue ? `Overdue by ${days}d ${hours}h` : `${days}d ${hours}h left`

  return <Badge color={isOverdue ? 'var(--priority-high)' : undefined}>{label}</Badge>
}

export function TaskCard({ task, onClick }) {
  return (
    <div
      className={onClick ? `${styles.card} ${styles.clickable}` : styles.card}
      style={{ borderLeftColor: PRIORITY_COLOR_VAR[task.priority] }}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
      onKeyDown={
        onClick
          ? (event) => {
              if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault()
                onClick()
              }
            }
          : undefined
      }
    >
      <p className={styles.title}>{task.title}</p>
      {task.description && (
        <p className={styles.description} title={task.description}>
          {task.description}
        </p>
      )}
      {(task.deadline || task.assignee) && (
        <div className={styles.footer}>
          {task.deadline ? <DeadlineBadge deadline={task.deadline} /> : <span />}
          {task.assignee && (
            <span className={styles.assignee}>
              <Avatar firstName={task.assignee.firstName} lastName={task.assignee.lastName} size={20} />
              {task.assignee.firstName} {task.assignee.lastName}
            </span>
          )}
        </div>
      )}
    </div>
  )
}
