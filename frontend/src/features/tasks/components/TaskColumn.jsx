import { useDroppable } from '@dnd-kit/core'
import { TaskStatus } from '../../../shared/constants/taskStatus.js'
import { Button } from '../../../shared/components/Button.jsx'
import { DraggableTaskCard } from './DraggableTaskCard.jsx'
import styles from './TaskColumn.module.css'

const STATUS_LABEL = {
  [TaskStatus.TODO]: 'To do',
  [TaskStatus.IN_PROGRESS]: 'In progress',
  [TaskStatus.DONE]: 'Done',
}

const STATUS_COLOR_VAR = {
  [TaskStatus.TODO]: 'var(--status-todo)',
  [TaskStatus.IN_PROGRESS]: 'var(--status-in-progress)',
  [TaskStatus.DONE]: 'var(--status-done)',
}

export function TaskColumn({ status, tasks, onTaskClick, onAddTask }) {
  const { setNodeRef, isOver } = useDroppable({ id: status })

  return (
    <div className={styles.column}>
      <div className={styles.header}>
        <span className={styles.dot} style={{ backgroundColor: STATUS_COLOR_VAR[status] }} />
        <h3 className={styles.title}>{STATUS_LABEL[status]}</h3>
        <span className={styles.count}>{tasks.length}</span>
      </div>

      <div ref={setNodeRef} className={isOver ? styles.listOver : styles.list}>
        {tasks.map((task) => (
          <DraggableTaskCard key={task.id} task={task} onClick={() => onTaskClick(task)} />
        ))}
      </div>

      <Button variant="secondary" className={styles.addButton} onClick={() => onAddTask(status)}>
        + Add task
      </Button>
    </div>
  )
}
