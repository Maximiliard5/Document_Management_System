import { useDraggable } from '@dnd-kit/core'
import { CSS } from '@dnd-kit/utilities'
import { TaskCard } from './TaskCard.jsx'
import styles from './DraggableTaskCard.module.css'

// Thin drag wrapper around the plain TaskCard, kept separate so TaskCard stays
// reusable outside the board (e.g. the task form's preview) without dnd-kit
// machinery attached. The overlay copy shown while dragging renders TaskCard
// directly, not this wrapper. onClick opens the edit form - dnd-kit's
// PointerSensor activation distance means a plain click still reaches it
// normally, only an actual drag suppresses it.
export function DraggableTaskCard({ task, onClick }) {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: task.id,
  })

  return (
    <div
      ref={setNodeRef}
      className={styles.wrapper}
      style={{ transform: CSS.Translate.toString(transform), opacity: isDragging ? 0.4 : 1 }}
      {...listeners}
      {...attributes}
    >
      <TaskCard task={task} onClick={onClick} />
    </div>
  )
}
