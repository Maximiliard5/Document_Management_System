// Mirrors com.example.dms.entity.TaskStatus. Values match the wire format exactly
// (Jackson serializes the enum as its name), so `TaskStatus.TODO === 'TODO'`.
export const TaskStatus = Object.freeze({
  TODO: 'TODO',
  IN_PROGRESS: 'IN_PROGRESS',
  DONE: 'DONE',
})

// Column order for the Kanban board.
export const TASK_STATUS_ORDER = [TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.DONE]
