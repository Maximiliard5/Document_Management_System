import { useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { DndContext, DragOverlay, PointerSensor, useSensor, useSensors } from '@dnd-kit/core'
import { TASK_STATUS_ORDER } from '../../shared/constants/taskStatus.js'
import { TASK_PRIORITY_ORDER } from '../../shared/constants/taskPriority.js'
import { PageHeader } from '../../shared/components/PageHeader.jsx'
import { Spinner } from '../../shared/components/Spinner.jsx'
import { useTasks, useUpdateTask, optimisticallyUpdateTask, rollbackTaskLists } from './useTasks.js'
import { TaskColumn } from './components/TaskColumn.jsx'
import { TaskCard } from './components/TaskCard.jsx'
import { TaskFormModal } from './components/TaskFormModal.jsx'
import styles from './TaskBoardPage.module.css'

export function TaskBoardPage() {
  const { project } = useOutletContext()
  const { data: tasks, isLoading, isError, error } = useTasks(project.id)
  const updateTask = useUpdateTask(project.id)
  const queryClient = useQueryClient()
  const [activeTask, setActiveTask] = useState(null)
  // { mode: 'create', status } | { mode: 'edit', task } | null
  const [formState, setFormState] = useState(null)

  // A small activation distance keeps a plain click (opening a task) from
  // being swallowed as a drag.
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 8 } }))

  function handleDragStart(event) {
    setActiveTask(tasks.find((task) => task.id === event.active.id) ?? null)
  }

  function handleDragEnd(event) {
    const { active, over } = event
    const task = tasks.find((t) => t.id === active.id)
    const newStatus = over?.id

    // Both state updates happen synchronously in this same handler, so React
    // batches them into one render: the drag overlay disappears and the task
    // is already in its new column in the same frame, instead of rendering
    // the old column first (a visible snap-back) and jumping a moment later.
    if (task && newStatus && task.status !== newStatus) {
      const previousLists = optimisticallyUpdateTask(queryClient, project.id, task.id, { status: newStatus })
      updateTask.mutate(
        { taskId: task.id, status: newStatus },
        { onError: () => rollbackTaskLists(queryClient, previousLists) },
      )
    }
    setActiveTask(null)
  }

  return (
    <div>
      <PageHeader title="Board" />

      {isLoading && <Spinner label="Loading tasks" />}

      {isError && (
        <p role="alert" className={styles.error}>
          {error.message}
        </p>
      )}

      {!isLoading && !isError && (
        <DndContext
          sensors={sensors}
          onDragStart={handleDragStart}
          onDragEnd={handleDragEnd}
          onDragCancel={() => setActiveTask(null)}
        >
          <div className={styles.board}>
            {TASK_STATUS_ORDER.map((status) => (
              <TaskColumn
                key={status}
                status={status}
                tasks={tasks
                  .filter((task) => task.status === status)
                  .sort(
                    (a, b) =>
                      TASK_PRIORITY_ORDER.indexOf(a.priority) - TASK_PRIORITY_ORDER.indexOf(b.priority),
                  )}
                onTaskClick={(task) => setFormState({ mode: 'edit', task })}
                onAddTask={(clickedStatus) => setFormState({ mode: 'create', status: clickedStatus })}
              />
            ))}
          </div>
          <DragOverlay>{activeTask && <TaskCard task={activeTask} />}</DragOverlay>
        </DndContext>
      )}

      <TaskFormModal
        isOpen={Boolean(formState)}
        onClose={() => setFormState(null)}
        projectId={project.id}
        project={project}
        task={formState?.mode === 'edit' ? formState.task : undefined}
        createStatus={formState?.mode === 'create' ? formState.status : undefined}
      />
    </div>
  )
}
