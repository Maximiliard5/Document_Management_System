import { useCallback } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Modal } from '../../../shared/components/Modal.jsx'
import { Button } from '../../../shared/components/Button.jsx'
import { FormField } from '../../../shared/components/FormField.jsx'
import { ApiError } from '../../../shared/api/httpClient.js'
import { useCreateProject } from '../useProjects.js'
import styles from './CreateProjectModal.module.css'

// Mirrors backend CreateProjectRequest - description has no @NotBlank, so an
// empty string is a valid value to send, not something to strip client-side.
const schema = z.object({
  name: z.string().min(1, 'Name is required').max(100, 'Must be 100 characters or fewer'),
  description: z.string().max(500, 'Must be 500 characters or fewer'),
})

export function CreateProjectModal({ isOpen, onClose }) {
  const createProject = useCreateProject()
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({ resolver: zodResolver(schema), defaultValues: { name: '', description: '' } })

  const onSubmit = async (data) => {
    try {
      await createProject.mutateAsync(data)
      reset()
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
    <Modal isOpen={isOpen} onClose={handleClose} title="New project">
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
            {isSubmitting ? 'Creating...' : 'Create project'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
