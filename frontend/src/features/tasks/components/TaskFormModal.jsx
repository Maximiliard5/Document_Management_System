import { useEffect, useCallback } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Modal } from '../../../shared/components/Modal.jsx'
import { Button } from '../../../shared/components/Button.jsx'
import { FormField } from '../../../shared/components/FormField.jsx'
import { ApiError } from '../../../shared/api/httpClient.js'
import { TaskPriority } from '../../../shared/constants/taskPriority.js'
import { TaskStatus } from '../../../shared/constants/taskStatus.js'
import { useCreateTask, useUpdateTask } from '../useTasks.js'
import styles from './TaskFormModal.module.css'

// Mirrors CreateTaskRequest/UpdateTaskRequest - description has no @NotBlank,
// so an empty string is a valid value to send, not something to strip client-side.
const schema = z.object({
  title: z.string().min(1, 'Title is required').max(200, 'Must be 200 characters or fewer'),
  description: z.string().max(1000, 'Must be 1000 characters or fewer'),
  priority: z.enum([TaskPriority.LOW, TaskPriority.MEDIUM, TaskPriority.HIGH]),
  deadline: z.string().optional(),
  // A <select>'s value is always a string; '' means "Unassigned" and is
  // normalized to null on submit (see onSubmit below), same as deadline.
  assigneeId: z.string().optional(),
})

const PRIORITY_LABEL = {
  [TaskPriority.LOW]: 'Low',
  [TaskPriority.MEDIUM]: 'Medium',
  [TaskPriority.HIGH]: 'High',
}

const emptyValues = {
  title: '',
  description: '',
  priority: TaskPriority.MEDIUM,
  deadline: '',
  assigneeId: '',
}

// Create mode: pass `createStatus` (which column's "+ Add task" was clicked).
// Edit mode: pass `task`; createStatus is ignored.
// CreateTaskRequest has no status field (server always creates TODO) - creating
// directly into another column takes a second, immediate update call after create.
export function TaskFormModal({ isOpen, onClose, projectId, project, task, createStatus }) {
  const isEditing = Boolean(task)
  const createTask = useCreateTask(projectId)
  const updateTask = useUpdateTask(projectId)

  // ProjectResponse.members excludes the owner (see backend/ARCHITECTURE.md) -
  // same merge MembersSection.jsx uses. The backend itself allows assigning to
  // any user site-wide, not just project members - restricted to members here
  // since assigning to someone with no access to the board would be confusing,
  // and it means no extra search UI/API call is needed (the project's member
  // list is already loaded on this page).
  const assignableMembers = [project.owner, ...project.members]

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({ resolver: zodResolver(schema), defaultValues: emptyValues })

  // react-hook-form's defaultValues only apply on mount - reseed whenever the
  // modal opens, since it's reused for every task (edit) or every column (create).
  useEffect(() => {
    if (!isOpen) return
    reset(
      task
        ? {
            title: task.title,
            description: task.description ?? '',
            priority: task.priority,
            deadline: task.deadline ?? '',
            assigneeId: task.assignee ? String(task.assignee.id) : '',
          }
        : emptyValues,
    )
  }, [isOpen, task, reset])

  const handleClose = useCallback(() => {
    reset(emptyValues)
    onClose()
  }, [reset, onClose])

  const onSubmit = async (formData) => {
    // Native <input type="date"> yields '' when cleared; the backend expects
    // an absent/null deadline, not an empty string (LocalDate can't parse it).
    // Same normalization for assigneeId - '' ("Unassigned") becomes null,
    // otherwise the <select>'s string value is parsed back to the numeric id.
    const data = {
      ...formData,
      deadline: formData.deadline || null,
      assigneeId: formData.assigneeId ? Number(formData.assigneeId) : null,
    }
    try {
      if (isEditing) {
        await updateTask.mutateAsync({ taskId: task.id, ...data })
      } else {
        const created = await createTask.mutateAsync(data)
        if (createStatus && createStatus !== TaskStatus.TODO) {
          await updateTask.mutateAsync({ taskId: created.id, status: createStatus })
        }
      }
      handleClose()
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Something went wrong. Try again.'
      setError('root', { message })
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title={isEditing ? 'Edit task' : 'New task'}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className={styles.form}>
        <FormField label="Title" error={errors.title?.message} {...register('title')} />
        <FormField
          as="textarea"
          rows={3}
          label="Description"
          error={errors.description?.message}
          {...register('description')}
        />
        <FormField as="select" label="Priority" error={errors.priority?.message} {...register('priority')}>
          {Object.values(TaskPriority).map((priority) => (
            <option key={priority} value={priority}>
              {PRIORITY_LABEL[priority]}
            </option>
          ))}
        </FormField>
        <FormField
          type="date"
          label="Deadline"
          error={errors.deadline?.message}
          {...register('deadline')}
        />
        <FormField
          as="select"
          label="Assignee"
          error={errors.assigneeId?.message}
          {...register('assigneeId')}
        >
          <option value="">Unassigned</option>
          {assignableMembers.map((member) => (
            <option key={member.id} value={member.id}>
              {member.firstName} {member.lastName}
            </option>
          ))}
        </FormField>

        {errors.root && (
          <p className={styles.formError} role="alert">
            {errors.root.message}
          </p>
        )}

        <div className={styles.actions}>
          <Button type="button" variant="secondary" onClick={handleClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Saving...' : isEditing ? 'Save changes' : 'Create task'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
