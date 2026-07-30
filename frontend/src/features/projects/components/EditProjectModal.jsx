import { useCallback, useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Modal } from '../../../shared/components/Modal.jsx'
import { Button } from '../../../shared/components/Button.jsx'
import { FormField } from '../../../shared/components/FormField.jsx'
import { ApiError } from '../../../shared/api/httpClient.js'
import { useUpdateProject } from '../useProjects.js'
import styles from './EditProjectModal.module.css'

// Mirrors backend UpdateProjectRequest - same shape as CreateProjectModal's
// schema, just pointed at the update mutation and pre-filled from `project`.
const schema = z.object({
  name: z.string().min(1, 'Name is required').max(100, 'Must be 100 characters or fewer'),
  description: z.string().max(500, 'Must be 500 characters or fewer'),
})

export function EditProjectModal({ isOpen, onClose, project }) {
  const updateProject = useUpdateProject(project.id)
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({ resolver: zodResolver(schema), defaultValues: { name: '', description: '' } })

  // Same reseed-on-open pattern as TaskFormModal's edit mode - this modal is
  // reused across renders, so defaultValues (mount-only) can't do this alone.
  useEffect(() => {
    if (!isOpen) return
    reset({ name: project.name, description: project.description ?? '' })
  }, [isOpen, project, reset])

  const onSubmit = async (data) => {
    try {
      await updateProject.mutateAsync(data)
      onClose()
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Something went wrong. Try again.'
      setError('root', { message })
    }
  }

  const handleClose = useCallback(() => {
    reset()
    onClose()
  }, [reset, onClose])

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title="Edit project">
      <form onSubmit={handleSubmit(onSubmit)} noValidate className={styles.form}>
        <FormField label="Name" error={errors.name?.message} {...register('name')} />
        <FormField label="Description" error={errors.description?.message} {...register('description')} />

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
            {isSubmitting ? 'Saving...' : 'Save changes'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
