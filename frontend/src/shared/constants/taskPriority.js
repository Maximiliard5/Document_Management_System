// Mirrors com.example.dms.entity.TaskPriority. Values match the wire format exactly
// (Jackson serializes the enum as its name), so `TaskPriority.HIGH === 'HIGH'`.
export const TaskPriority = Object.freeze({
  LOW: 'LOW',
  MEDIUM: 'MEDIUM',
  HIGH: 'HIGH',
})

// Board display order - urgency high to low. There's no due-date field on
// tasks, so priority is the only "urgency" signal a column can sort by.
export const TASK_PRIORITY_ORDER = [TaskPriority.HIGH, TaskPriority.MEDIUM, TaskPriority.LOW]
